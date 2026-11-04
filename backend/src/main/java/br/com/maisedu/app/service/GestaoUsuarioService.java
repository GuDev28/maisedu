package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.DadosPessoaisResponse;
import br.com.maisedu.app.dto.SenhaRedefinidaResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.AlunoTurma;
import br.com.maisedu.app.model.DesempenhoTopico;
import br.com.maisedu.app.model.LogAuditoria;
import br.com.maisedu.app.model.ProfessorTurma;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.Resposta;
import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlunoTurmaRepository;
import br.com.maisedu.app.repository.DesempenhoTopicoRepository;
import br.com.maisedu.app.repository.LogAuditoriaRepository;
import br.com.maisedu.app.repository.ProfessorTurmaRepository;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.SenhaGeradora;
import br.com.maisedu.app.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gestão de usuários pela instituição: redefinição de senha e os controles técnicos que dão
 * efeito aos direitos do titular descritos na Política de Privacidade (LGPD, art. 18) — acesso e
 * portabilidade (exportação), correção, bloqueio (desativação) e eliminação (anonimização).
 */
@Service
@RequiredArgsConstructor
public class GestaoUsuarioService {

    public static final int TAMANHO_MAXIMO_NOME = 255;

    private final UsuarioRepository usuarioRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final ProfessorTurmaRepository professorTurmaRepository;
    private final QuestaoRepository questaoRepository;
    private final RespostaRepository respostaRepository;
    private final DesempenhoTopicoRepository desempenhoTopicoRepository;
    private final LogAuditoriaRepository logAuditoriaRepository;
    private final PasswordEncoder passwordEncoder;
    private final SenhaGeradora senhaGeradora;
    private final ResendEmailService emailService;
    private final AuditoriaService auditoriaService;

    /** Admin: usuários da instituição (filtro opcional por papel). Professor: alunos das turmas em que leciona. */
    @Transactional(readOnly = true)
    public List<Usuario> listar(UsuarioAutenticado ator, RoleNome papel) {
        if (ator.possuiRole(RoleNome.ADMIN)) {
            return usuarioRepository.findDaInstituicao(ator.instituicaoId(), papel);
        }
        if (ator.possuiRole(RoleNome.PROFESSOR)) {
            return usuarioRepository.findAlunosDasTurmasDoProfessor(ator.id());
        }
        throw new AccessDeniedException("Sem permissão para listar usuários.");
    }

    /**
     * Gera nova senha provisória (a ser trocada no próximo acesso), desbloqueia a conta e derruba as
     * sessões abertas. Professores e admins recebem a senha por e-mail; para alunos, que não têm
     * e-mail, ela é devolvida para quem fez a redefinição repassar ao aluno.
     *
     * Admin redefine a senha de qualquer usuário da instituição; o professor, só a dos alunos das
     * turmas em que leciona.
     */
    @Transactional
    public SenhaRedefinidaResponse redefinirSenha(UsuarioAutenticado ator, Long usuarioId) {
        Usuario alvo = buscarNaInstituicao(ator, usuarioId);
        if (alvo.getId().equals(ator.id())) {
            throw new NegocioException("Para mudar a sua própria senha, use a opção de troca de senha.");
        }
        exigirNaoAnonimizado(alvo);

        if (!ator.possuiRole(RoleNome.ADMIN)) {
            boolean alunoDoProfessor = ator.possuiRole(RoleNome.PROFESSOR)
                    && alvo.possuiRole(RoleNome.ALUNO)
                    && alunoTurmaRepository.alunoEstaEmTurmaDoProfessor(alvo.getId(), ator.id());
            if (!alunoDoProfessor) {
                throw new AccessDeniedException("O professor só redefine a senha de alunos das turmas em que leciona.");
            }
        }

        String senhaProvisoria = senhaGeradora.gerar();
        alvo.redefinirSenhaProvisoria(passwordEncoder.encode(senhaProvisoria));
        usuarioRepository.save(alvo);
        auditoriaService.registrar(ator, AcaoAuditoria.REDEFINICAO_SENHA, "Usuario", alvo.getId(), true, null);

        if (alvo.getEmail() != null) {
            emailService.enviarNovaSenhaProvisoria(alvo.getId(), alvo.getNome(), alvo.getEmail(), senhaProvisoria);
            return new SenhaRedefinidaResponse(alvo.getId(), alvo.getNome(), alvo.identificadorDeAcesso(), null, true);
        }
        return new SenhaRedefinidaResponse(alvo.getId(), alvo.getNome(), alvo.identificadorDeAcesso(),
                senhaProvisoria, false);
    }

    /** Correção de dados (art. 18, III): o nome é o único dado cadastral editável. */
    @Transactional
    public Usuario corrigirNome(UsuarioAutenticado admin, Long usuarioId, String novoNome) {
        exigirAdmin(admin);
        Usuario alvo = buscarNaInstituicao(admin, usuarioId);
        exigirNaoAnonimizado(alvo);
        if (novoNome == null || novoNome.isBlank()) {
            throw new NegocioException("Informe o nome");
        }
        String nome = novoNome.strip();
        if (nome.length() > TAMANHO_MAXIMO_NOME) {
            throw new NegocioException("O nome deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres");
        }
        String anterior = alvo.getNome();
        alvo.corrigirNome(nome);
        usuarioRepository.save(alvo);
        auditoriaService.registrar(admin, AcaoAuditoria.CORRECAO_DADOS_USUARIO, "Usuario", alvo.getId(), true,
                "nome alterado (antes: " + anterior + ")");
        return alvo;
    }

    /** Bloqueio (art. 18, IV): a conta deixa de autenticar e as sessões abertas caem na hora. */
    @Transactional
    public Usuario desativar(UsuarioAutenticado admin, Long usuarioId) {
        exigirAdmin(admin);
        Usuario alvo = buscarNaInstituicao(admin, usuarioId);
        if (alvo.getId().equals(admin.id())) {
            throw new NegocioException("Você não pode desativar a sua própria conta.");
        }
        exigirNaoAnonimizado(alvo);
        alvo.desativar();
        usuarioRepository.save(alvo);
        auditoriaService.registrar(admin, AcaoAuditoria.DESATIVACAO_USUARIO, "Usuario", alvo.getId(), true, null);
        return alvo;
    }

    @Transactional
    public Usuario reativar(UsuarioAutenticado admin, Long usuarioId) {
        exigirAdmin(admin);
        Usuario alvo = buscarNaInstituicao(admin, usuarioId);
        exigirNaoAnonimizado(alvo);
        alvo.reativar();
        usuarioRepository.save(alvo);
        auditoriaService.registrar(admin, AcaoAuditoria.REATIVACAO_USUARIO, "Usuario", alvo.getId(), true, null);
        return alvo;
    }

    /** Acesso e portabilidade (art. 18, II e V): todos os dados pessoais do usuário em formato estruturado (JSON). */
    @Transactional(readOnly = true)
    public DadosPessoaisResponse exportarDados(UsuarioAutenticado admin, Long usuarioId) {
        exigirAdmin(admin);
        Usuario alvo = buscarNaInstituicao(admin, usuarioId);

        List<String> papeis = new ArrayList<>();
        for (Role role : alvo.getRoles()) {
            papeis.add(role.getNome().name());
        }
        var cadastro = new DadosPessoaisResponse.Cadastro(
                alvo.getId(), alvo.getNome(), alvo.getEmail(), alvo.getLogin(), papeis,
                alvo.getInstituicao().getNome(), alvo.isAtivo(), alvo.getCriadoEm(),
                alvo.getTermosAceitosEm(), alvo.getTermosVersao());

        List<String> turmas = new ArrayList<>();
        for (AlunoTurma vinculo : alunoTurmaRepository.findByAlunoIdComTurma(alvo.getId())) {
            turmas.add(vinculo.getTurma().getNome() + " (aluno)");
        }
        for (ProfessorTurma vinculo : professorTurmaRepository.findByProfessorId(alvo.getId())) {
            turmas.add(vinculo.getTurma().getNome() + " (professor)");
        }

        List<DadosPessoaisResponse.QuestaoAutoria> questoes = new ArrayList<>();
        for (Questao questao : questaoRepository.findByAutorIdOrderByCriadoEmDesc(alvo.getId())) {
            questoes.add(new DadosPessoaisResponse.QuestaoAutoria(questao.getId(), questao.getEnunciado(),
                    questao.getTopico().getNome(), questao.getStatus().name(), questao.getCriadoEm()));
        }

        List<DadosPessoaisResponse.RespostaExportada> respostas = new ArrayList<>();
        for (Resposta resposta : respostaRepository.findDetalhadasDoAluno(alvo.getId())) {
            var entregue = resposta.getAtividadeAlunoQuestao();
            var atividadeAluno = entregue.getAtividadeAluno();
            String origem = atividadeAluno.getAtividade() != null
                    ? atividadeAluno.getAtividade().getTitulo()
                    : "Prática adaptativa";
            respostas.add(new DadosPessoaisResponse.RespostaExportada(resposta.getRespondidaEm(), origem,
                    entregue.getQuestao().getId(), String.valueOf(resposta.getAlternativa().letra()),
                    resposta.acertou()));
        }

        List<DadosPessoaisResponse.DesempenhoExportado> desempenho = new ArrayList<>();
        for (DesempenhoTopico registro : desempenhoTopicoRepository.findByAlunoId(alvo.getId())) {
            desempenho.add(new DadosPessoaisResponse.DesempenhoExportado(registro.getTopico().getNome(),
                    registro.getNivelDificuldade().name(), registro.getTotalRespostas(), registro.getTotalAcertos(),
                    registro.getPercentualAcerto()));
        }

        List<DadosPessoaisResponse.RegistroAuditoria> registros = new ArrayList<>();
        for (LogAuditoria log : logAuditoriaRepository.findByUsuarioIdOrderByCriadoEmDesc(alvo.getId())) {
            registros.add(new DadosPessoaisResponse.RegistroAuditoria(log.getCriadoEm(), log.getAcao().name(),
                    log.isSucesso(), log.getIp(), log.getUserAgent(), log.getDetalhes()));
        }

        auditoriaService.registrar(admin, AcaoAuditoria.EXPORTACAO_DADOS_USUARIO, "Usuario", alvo.getId(), true, null);
        return new DadosPessoaisResponse(LocalDateTime.now(), cadastro, turmas, questoes, respostas, desempenho,
                registros);
    }

    /**
     * Eliminação (art. 18, VI). Remove nome, e-mail, login e vínculos com turmas e apaga o nome e o
     * identificador nos registros de auditoria. Respostas, desempenho e questões continuam no banco
     * ligados a um id sem identificação — dados anonimizados deixam de ser dados pessoais (art. 12) —
     * para não distorcer estatísticas da turma nem apagar questões já usadas por outras escolas.
     * Contas de administrador não são anonimizadas por aqui, para a instituição não ficar sem gestor.
     */
    @Transactional
    public Usuario anonimizar(UsuarioAutenticado admin, Long usuarioId) {
        exigirAdmin(admin);
        Usuario alvo = buscarNaInstituicao(admin, usuarioId);
        if (alvo.getId().equals(admin.id())) {
            throw new NegocioException("Você não pode anonimizar a sua própria conta.");
        }
        if (alvo.possuiRole(RoleNome.ADMIN)) {
            throw new NegocioException("Contas de administrador não podem ser anonimizadas pela plataforma.");
        }
        exigirNaoAnonimizado(alvo);

        alunoTurmaRepository.excluirVinculosDoAluno(alvo.getId());
        professorTurmaRepository.excluirVinculosDoProfessor(alvo.getId());

        alvo.anonimizar(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuarioRepository.save(alvo);

        logAuditoriaRepository.anonimizarRegistrosDoUsuario(alvo.getId(), alvo.getNome());
        logAuditoriaRepository.substituirDetalhes(alvo.getId(), AcaoAuditoria.ENVIO_EMAIL, "Dados anonimizados");
        logAuditoriaRepository.substituirDetalhesSobreEntidade("Usuario", alvo.getId(),
                AcaoAuditoria.CORRECAO_DADOS_USUARIO, "Dados anonimizados");

        auditoriaService.registrar(admin, AcaoAuditoria.ANONIMIZACAO_USUARIO, "Usuario", alvo.getId(), true, null);
        return alvo;
    }

    private Usuario buscarNaInstituicao(UsuarioAutenticado ator, Long usuarioId) {
        Usuario alvo = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        if (!alvo.getInstituicao().getId().equals(ator.instituicaoId())) {
            // Mesmo 404 de um id inexistente: não revela usuários de outras instituições.
            throw new RecursoNaoEncontradoException("Usuário não encontrado.");
        }
        return alvo;
    }

    private void exigirAdmin(UsuarioAutenticado ator) {
        if (!ator.possuiRole(RoleNome.ADMIN)) {
            throw new AccessDeniedException("Só o administrador da instituição pode fazer esta operação.");
        }
    }

    private void exigirNaoAnonimizado(Usuario alvo) {
        if (alvo.isAnonimizado()) {
            throw new NegocioException("Esta conta foi anonimizada e não pode mais ser alterada.");
        }
    }
}
