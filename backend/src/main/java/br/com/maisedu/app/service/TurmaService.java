package br.com.maisedu.app.service;

import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.repository.InstituicaoRepository;
import br.com.maisedu.app.repository.TurmaRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** RF06: cadastro e gestão das turmas da instituição, exclusivos do administrador. */
@Service
@RequiredArgsConstructor
public class TurmaService {

    public static final int TAMANHO_MAXIMO_NOME = 100;

    private final TurmaRepository turmaRepository;
    private final InstituicaoRepository instituicaoRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public Turma cadastrar(UsuarioAutenticado admin, String nomeInformado, AnoEscolar anoEscolar) {
        exigirAdmin(admin);
        if (nomeInformado == null || nomeInformado.isBlank()) {
            throw new NegocioException("Informe o nome da turma");
        }
        String nome = nomeInformado.strip();
        if (nome.length() > TAMANHO_MAXIMO_NOME) {
            throw new NegocioException("O nome da turma deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres");
        }
        if (anoEscolar == null) {
            throw new NegocioException("Selecione o ano escolar (6º ao 9º ano)");
        }
        if (turmaRepository.existsByInstituicaoIdAndNomeIgnoreCase(admin.instituicaoId(), nome)) {
            throw new NegocioException("Já existe uma turma com este nome na sua instituição.");
        }

        Instituicao instituicao = instituicaoRepository.findById(admin.instituicaoId())
                .orElseThrow(() -> new NegocioException("Instituição não encontrada."));

        Turma turma = turmaRepository.save(new Turma(nome, instituicao, anoEscolar));
        auditoriaService.registrar(admin, AcaoAuditoria.CADASTRO_TURMA, "Turma", turma.getId(), true,
                nome + " (" + anoEscolar + ")");
        return turma;
    }

    /** Todas as turmas da instituição, inclusive as inativas (tela de gestão do admin). */
    @Transactional(readOnly = true)
    public List<Turma> listarDaInstituicao(UsuarioAutenticado admin) {
        exigirAdmin(admin);
        return turmaRepository.findByInstituicaoIdOrderByAtivoDescNomeAsc(admin.instituicaoId());
    }

    /** Turma inativa não aceita novos vínculos nem atividades (RN01), mas o histórico é mantido. */
    @Transactional
    public Turma alterarSituacao(UsuarioAutenticado admin, Long turmaId, boolean ativa) {
        exigirAdmin(admin);
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma não encontrada."));
        if (!turma.getInstituicao().getId().equals(admin.instituicaoId())) {
            throw new AccessDeniedException("Turma pertence a outra instituição.");
        }
        turma.setAtivo(ativa);
        auditoriaService.registrar(admin, AcaoAuditoria.ALTERACAO_TURMA, "Turma", turmaId, true,
                ativa ? "reativada" : "desativada");
        return turma;
    }

    private void exigirAdmin(UsuarioAutenticado ator) {
        if (!ator.possuiRole(RoleNome.ADMIN)) {
            throw new AccessDeniedException("Só o administrador gerencia turmas.");
        }
    }
}
