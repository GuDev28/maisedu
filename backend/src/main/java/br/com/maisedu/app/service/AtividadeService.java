package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.AtividadeAlunoResumoResponse;
import br.com.maisedu.app.dto.AtividadeResumoResponse;
import br.com.maisedu.app.dto.FolhaAtividadeResponse;
import br.com.maisedu.app.dto.QuestaoResponse;
import br.com.maisedu.app.dto.ResultadoAtividadeResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.Alternativa;
import br.com.maisedu.app.model.AlunoTurma;
import br.com.maisedu.app.model.Atividade;
import br.com.maisedu.app.model.AtividadeAluno;
import br.com.maisedu.app.model.AtividadeAlunoQuestao;
import br.com.maisedu.app.model.AtividadeQuestao;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.Resposta;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.TipoAtividade;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlunoTurmaRepository;
import br.com.maisedu.app.repository.AtividadeAlunoRepository;
import br.com.maisedu.app.repository.AtividadeRepository;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.TurmaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** RF15 (atividade regular), início da atividade pelo aluno com snapshot (RN03) e consulta das folhas. */
@Service
@RequiredArgsConstructor
public class AtividadeService {

    private final AtividadeRepository atividadeRepository;
    private final AtividadeAlunoRepository atividadeAlunoRepository;
    private final TurmaRepository turmaRepository;
    private final QuestaoRepository questaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final RespostaRepository respostaRepository;
    private final AcessoTurma acessoTurma;
    private final AuditoriaService auditoriaService;

    // ---------- Professor ----------

    /**
     * RF15: só o professor que leciona na turma cria atividades para ela (RN01), com questões
     * APROVADAS do banco compartilhado, na ordem informada, sem repetição.
     */
    @Transactional
    public Atividade criar(UsuarioAutenticado ator, Long turmaId, String titulo, List<Long> questaoIds) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma não encontrada."));
        if (!turma.getInstituicao().getId().equals(ator.instituicaoId())
                || !ator.possuiRole(RoleNome.PROFESSOR)
                || !acessoTurma.lecionaNaTurma(ator, turmaId)) {
            throw new AccessDeniedException("Só o professor que leciona na turma pode criar atividades para ela.");
        }
        if (!turma.isAtivo()) {
            throw new NegocioException("Turma inativa não recebe novas atividades.");
        }
        if (questaoIds == null || questaoIds.isEmpty()) {
            throw new NegocioException("Selecione pelo menos uma questão para a atividade");
        }

        Map<Long, Questao> encontradas = new HashMap<>();
        for (Questao questao : questaoRepository.findAllByIdIn(questaoIds)) {
            encontradas.put(questao.getId(), questao);
        }
        List<Questao> emOrdem = new ArrayList<>();
        for (Long id : questaoIds) {
            Questao questao = encontradas.get(id);
            if (questao == null) {
                throw new NegocioException("Questão " + id + " não encontrada");
            }
            emOrdem.add(questao);
        }

        Usuario professor = usuarioRepository.findById(ator.id())
                .orElseThrow(() -> new NegocioException("Professor não encontrado"));
        Atividade atividade = new Atividade(titulo, turma, professor);
        atividade.definirQuestoes(emOrdem);
        Atividade salva = atividadeRepository.save(atividade);

        auditoriaService.registrar(ator, AcaoAuditoria.CRIACAO_ATIVIDADE, "Atividade", salva.getId(), true,
                "turma " + turmaId + ", " + emOrdem.size() + " questão(ões)");
        return salva;
    }

    @Transactional(readOnly = true)
    public List<AtividadeResumoResponse> listarDaTurma(UsuarioAutenticado ator, Long turmaId) {
        Turma turma = acessoTurma.buscarComAcesso(ator, turmaId);
        List<AtividadeResumoResponse> resumo = new ArrayList<>();
        for (Atividade atividade : atividadeRepository.findByTurmaId(turmaId)) {
            int iniciaram = 0;
            int concluiram = 0;
            for (AtividadeAluno atividadeAluno : atividadeAlunoRepository.findByAtividadeId(atividade.getId())) {
                iniciaram++;
                if (atividadeAluno.isConcluida()) {
                    concluiram++;
                }
            }
            resumo.add(new AtividadeResumoResponse(atividade.getId(), atividade.getTitulo(), turma.getId(),
                    turma.getNome(), atividade.getProfessor().getNome(), atividade.getQuestoes().size(),
                    iniciaram, concluiram, atividade.getCriadoEm()));
        }
        return resumo;
    }

    /** Resultado da atividade por aluno da turma: situação, respondidas e acertos. */
    @Transactional(readOnly = true)
    public ResultadoAtividadeResponse resultados(UsuarioAutenticado ator, Long atividadeId) {
        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada."));
        acessoTurma.autorizar(ator, atividade.getTurma());

        Map<Long, AtividadeAluno> porAluno = new HashMap<>();
        for (AtividadeAluno atividadeAluno : atividadeAlunoRepository.findByAtividadeId(atividadeId)) {
            porAluno.put(atividadeAluno.getAluno().getId(), atividadeAluno);
        }

        int totalQuestoes = atividade.getQuestoes().size();
        List<ResultadoAtividadeResponse.ResultadoAluno> alunos = new ArrayList<>();
        for (AlunoTurma vinculo : alunoTurmaRepository.findByTurmaIdComAluno(atividade.getTurma().getId())) {
            Usuario aluno = vinculo.getAluno();
            AtividadeAluno atividadeAluno = porAluno.get(aluno.getId());
            if (atividadeAluno == null) {
                alunos.add(new ResultadoAtividadeResponse.ResultadoAluno(aluno.getId(), aluno.getNome(),
                        AtividadeAlunoResumoResponse.NAO_INICIADA, 0, 0, totalQuestoes));
                continue;
            }
            int[] placar = placar(respostaRepository.findByAtividadeAluno(atividadeAluno.getId()));
            alunos.add(new ResultadoAtividadeResponse.ResultadoAluno(aluno.getId(), aluno.getNome(),
                    situacao(atividadeAluno), placar[0], placar[1], atividadeAluno.getQuestoes().size()));
        }

        List<QuestaoResponse> questoes = new ArrayList<>();
        for (AtividadeQuestao atividadeQuestao : atividade.getQuestoes()) {
            questoes.add(QuestaoResponse.from(atividadeQuestao.getQuestao()));
        }
        return new ResultadoAtividadeResponse(atividade.getId(), atividade.getTitulo(), atividade.getTurma().getNome(),
                questoes, alunos);
    }

    // ---------- Aluno ----------

    /** Atividades das turmas do aluno (com a situação de cada uma) e as práticas adaptativas dele. */
    @Transactional(readOnly = true)
    public List<AtividadeAlunoResumoResponse> listarDoAluno(Long alunoId) {
        List<AtividadeAlunoResumoResponse> lista = new ArrayList<>();
        for (Atividade atividade : atividadeRepository.findDisponiveisParaAluno(alunoId)) {
            AtividadeAluno atividadeAluno = atividadeAlunoRepository
                    .findByAlunoIdAndAtividadeId(alunoId, atividade.getId()).orElse(null);
            if (atividadeAluno == null) {
                lista.add(new AtividadeAlunoResumoResponse(atividade.getId(), null, atividade.getTitulo(),
                        TipoAtividade.REGULAR.name(), atividade.getTurma().getNome(),
                        AtividadeAlunoResumoResponse.NAO_INICIADA, atividade.getQuestoes().size(), 0, 0, null, null));
            } else {
                lista.add(resumir(atividadeAluno));
            }
        }
        for (AtividadeAluno pratica : atividadeAlunoRepository.findByAlunoIdAndTipo(alunoId, TipoAtividade.ADAPTATIVA)) {
            lista.add(resumir(pratica));
        }
        return lista;
    }

    /**
     * RF16, passo 1 e 2: inicia a atividade e fixa as questões entregues (RN03). Se o aluno já a
     * iniciou, devolve a mesma folha, com as mesmas questões e a mesma ordem (CA03).
     */
    @Transactional
    public AtividadeAluno iniciar(Long alunoId, Long atividadeId) {
        Usuario aluno = usuarioRepository.findByIdParaAtualizacao(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada."));

        if (!aluno.possuiRole(RoleNome.ALUNO)
                || !alunoTurmaRepository.existsByAlunoIdAndTurmaId(alunoId, atividade.getTurma().getId())) {
            throw new AccessDeniedException("Esta atividade não é de uma turma sua.");
        }

        var existente = atividadeAlunoRepository.findByAlunoIdAndAtividadeId(alunoId, atividadeId);
        if (existente.isPresent()) {
            return existente.get();
        }
        if (!atividade.getTurma().isAtivo()) {
            throw new NegocioException("A turma desta atividade foi desativada.");
        }

        AtividadeAluno nova = atividadeAlunoRepository.save(AtividadeAluno.regular(aluno, atividade));
        auditoriaService.registrar(aluno.getId(), aluno.getNome(), AcaoAuditoria.INICIO_ATIVIDADE, "AtividadeAluno",
                nova.getId(), true, "atividade " + atividadeId);
        return nova;
    }

    /** Folha da atividade (regular ou adaptativa) do próprio aluno. */
    @Transactional(readOnly = true)
    public FolhaAtividadeResponse obterFolha(Long alunoId, Long atividadeAlunoId) {
        AtividadeAluno atividadeAluno = buscarDoAluno(alunoId, atividadeAlunoId);
        return montarFolha(atividadeAluno);
    }

    public AtividadeAluno buscarDoAluno(Long alunoId, Long atividadeAlunoId) {
        AtividadeAluno atividadeAluno = atividadeAlunoRepository.findById(atividadeAlunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada."));
        if (!atividadeAluno.pertenceAo(alunoId)) {
            // RN04: o aluno só acessa o que foi entregue a ele; 404 para não revelar folhas de outros alunos.
            throw new RecursoNaoEncontradoException("Atividade não encontrada.");
        }
        return atividadeAluno;
    }

    @Transactional(readOnly = true)
    public FolhaAtividadeResponse montarFolha(AtividadeAluno atividadeAluno) {
        Map<Long, Resposta> respostas = new HashMap<>();
        for (Resposta resposta : respostaRepository.findByAtividadeAluno(atividadeAluno.getId())) {
            respostas.put(resposta.getAtividadeAlunoQuestao().getId(), resposta);
        }

        List<FolhaAtividadeResponse.QuestaoEntregue> questoes = new ArrayList<>();
        int acertos = 0;
        for (AtividadeAlunoQuestao entregue : atividadeAluno.getQuestoes()) {
            Questao questao = entregue.getQuestao();
            List<FolhaAtividadeResponse.AlternativaAluno> alternativas = new ArrayList<>();
            for (Alternativa alternativa : questao.getAlternativas()) {
                alternativas.add(new FolhaAtividadeResponse.AlternativaAluno(alternativa.getId(), alternativa.letra(),
                        alternativa.getTexto()));
            }

            FolhaAtividadeResponse.RespostaDada respostaDada = null;
            Resposta resposta = respostas.get(entregue.getId());
            if (resposta != null) {
                Alternativa correta = questao.alternativaCorreta();
                respostaDada = new FolhaAtividadeResponse.RespostaDada(resposta.getAlternativa().getId(),
                        resposta.getAlternativa().letra(), resposta.acertou(), correta.getId(), correta.letra());
                if (resposta.acertou()) {
                    acertos++;
                }
            }
            questoes.add(new FolhaAtividadeResponse.QuestaoEntregue(entregue.getId(), entregue.getOrdem(),
                    questao.getEnunciado(), questao.getTopico().getNome(), questao.getNivelDificuldade().name(),
                    alternativas, respostaDada));
        }

        return new FolhaAtividadeResponse(atividadeAluno.getId(), titulo(atividadeAluno),
                atividadeAluno.getTipo().name(), atividadeAluno.getIniciadaEm(), atividadeAluno.getConcluidaEm(),
                questoes.size(), respostas.size(), acertos, questoes);
    }

    private AtividadeAlunoResumoResponse resumir(AtividadeAluno atividadeAluno) {
        int[] placar = placar(respostaRepository.findByAtividadeAluno(atividadeAluno.getId()));
        Atividade origem = atividadeAluno.getAtividade();
        return new AtividadeAlunoResumoResponse(
                origem == null ? null : origem.getId(),
                atividadeAluno.getId(),
                titulo(atividadeAluno),
                atividadeAluno.getTipo().name(),
                origem == null ? null : origem.getTurma().getNome(),
                situacao(atividadeAluno),
                atividadeAluno.getQuestoes().size(),
                placar[0],
                placar[1],
                atividadeAluno.getIniciadaEm(),
                atividadeAluno.getConcluidaEm());
    }

    private static String titulo(AtividadeAluno atividadeAluno) {
        return atividadeAluno.getAtividade() != null ? atividadeAluno.getAtividade().getTitulo() : "Prática adaptativa";
    }

    private static String situacao(AtividadeAluno atividadeAluno) {
        return atividadeAluno.isConcluida()
                ? AtividadeAlunoResumoResponse.CONCLUIDA
                : AtividadeAlunoResumoResponse.EM_ANDAMENTO;
    }

    /** [respondidas, acertos] */
    private static int[] placar(List<Resposta> respostas) {
        int acertos = 0;
        for (Resposta resposta : respostas) {
            if (resposta.acertou()) {
                acertos++;
            }
        }
        return new int[]{respostas.size(), acertos};
    }
}
