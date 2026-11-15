package br.com.maisedu.app.service;

import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.repository.ProfessorTurmaRepository;
import br.com.maisedu.app.repository.TurmaRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Regra de acesso a uma turma (RN01), num lugar só, usada por vínculos, turmas, atividades e
 * desempenho. Turma de outra instituição nunca é acessível; dentro da instituição:
 * <ul>
 *   <li>{@link #buscarComAcesso}: admin da instituição ou professor que leciona na turma;</li>
 *   <li>{@link #buscarComoAdmin}: só o admin (gestão de turmas e vínculo de professores);</li>
 *   <li>{@link #buscarComoProfessorDaTurma}: só quem leciona na turma (criar atividades).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AcessoTurma {

    private final TurmaRepository turmaRepository;
    private final ProfessorTurmaRepository professorTurmaRepository;

    public Turma buscarComAcesso(UsuarioAutenticado ator, Long turmaId) {
        Turma turma = buscar(turmaId);
        autorizar(ator, turma);
        return turma;
    }

    public void autorizar(UsuarioAutenticado ator, Turma turma) {
        exigirMesmaInstituicao(ator, turma);
        if (ator.possuiRole(RoleNome.ADMIN)) {
            return;
        }
        if (ator.possuiRole(RoleNome.PROFESSOR) && lecionaNaTurma(ator, turma.getId())) {
            return;
        }
        throw new AccessDeniedException("Você não leciona nesta turma.");
    }

    public Turma buscarComoAdmin(UsuarioAutenticado ator, Long turmaId, String mensagemSemPermissao) {
        if (!ator.possuiRole(RoleNome.ADMIN)) {
            throw new AccessDeniedException(mensagemSemPermissao);
        }
        Turma turma = buscar(turmaId);
        exigirMesmaInstituicao(ator, turma);
        return turma;
    }

    public Turma buscarComoProfessorDaTurma(UsuarioAutenticado ator, Long turmaId, String mensagemSemPermissao) {
        Turma turma = buscar(turmaId);
        if (!turma.getInstituicao().getId().equals(ator.instituicaoId())
                || !ator.possuiRole(RoleNome.PROFESSOR)
                || !lecionaNaTurma(ator, turmaId)) {
            throw new AccessDeniedException(mensagemSemPermissao);
        }
        return turma;
    }

    public boolean lecionaNaTurma(UsuarioAutenticado ator, Long turmaId) {
        return professorTurmaRepository.existsByProfessorIdAndTurmaId(ator.id(), turmaId);
    }

    private Turma buscar(Long turmaId) {
        return turmaRepository.findById(turmaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma não encontrada."));
    }

    private void exigirMesmaInstituicao(UsuarioAutenticado ator, Turma turma) {
        if (!turma.getInstituicao().getId().equals(ator.instituicaoId())) {
            throw new AccessDeniedException("Turma pertence a outra instituição.");
        }
    }
}
