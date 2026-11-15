package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.PainelInstituicaoResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.StatusQuestao;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.InstituicaoRepository;
import br.com.maisedu.app.repository.LogAuditoriaRepository;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.TurmaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Números da página Início do administrador, sempre da própria instituição. */
@Service
@RequiredArgsConstructor
public class PainelService {

    private final InstituicaoRepository instituicaoRepository;
    private final TurmaRepository turmaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LogAuditoriaRepository logAuditoriaRepository;
    private final QuestaoRepository questaoRepository;

    @Transactional(readOnly = true)
    public PainelInstituicaoResponse resumoDaInstituicao(UsuarioAutenticado admin) {
        if (!admin.possuiRole(RoleNome.ADMIN)) {
            throw new AccessDeniedException("Só o administrador vê o resumo da instituição.");
        }
        Instituicao instituicao = instituicaoRepository.findById(admin.instituicaoId())
                .orElseThrow(() -> new NegocioException("Instituição não encontrada."));

        long turmasAtivas = 0;
        long turmasInativas = 0;
        for (Turma turma : turmaRepository.findByInstituicaoIdOrderByAtivoDescNomeAsc(instituicao.getId())) {
            if (turma.isAtivo()) {
                turmasAtivas++;
            } else {
                turmasInativas++;
            }
        }

        long professores = 0;
        long alunos = 0;
        long bloqueadas = 0;
        long primeiroAcesso = 0;
        long desativadas = 0;
        for (Usuario usuario : usuarioRepository.findDaInstituicao(instituicao.getId(), null)) {
            if (usuario.isAnonimizado()) {
                continue;
            }
            if (!usuario.isAtivo()) {
                desativadas++;
                continue;
            }
            if (usuario.possuiRole(RoleNome.PROFESSOR)) {
                professores++;
            }
            if (usuario.possuiRole(RoleNome.ALUNO)) {
                alunos++;
            }
            if (usuario.estaBloqueado()) {
                bloqueadas++;
            }
            if (usuario.isSenhaTemporaria()) {
                primeiroAcesso++;
            }
        }

        long logins = logAuditoriaRepository.countByInstituicaoIdAndAcaoAndSucessoTrueAndCriadoEmAfter(
                instituicao.getId(), AcaoAuditoria.LOGIN, LocalDateTime.now().minusDays(7));
        long noBanco = questaoRepository.countByStatus(StatusQuestao.APROVADA);

        return new PainelInstituicaoResponse(instituicao.getNome(), turmasAtivas, turmasInativas, professores, alunos,
                bloqueadas, primeiroAcesso, desativadas, logins, noBanco);
    }
}
