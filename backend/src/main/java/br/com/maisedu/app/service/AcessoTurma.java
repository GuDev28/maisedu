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
 * Regra de acesso a dados de uma turma (RN01), compartilhada por atividades e desempenho:
 * o professor só enxerga as turmas em que leciona; o admin, as turmas da própria instituição.
 */
@Component
@RequiredArgsConstructor
public class AcessoTurma {

    private final TurmaRepository turmaRepository;
    private final ProfessorTurmaRepository professorTurmaRepository;

    public Turma buscarComAcesso(UsuarioAutenticado ator, Long turmaId) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma não encontrada."));
        autorizar(ator, turma);
        return turma;
    }

    public void autorizar(UsuarioAutenticado ator, Turma turma) {
        if (!turma.getInstituicao().getId().equals(ator.instituicaoId())) {
            throw new AccessDeniedException("Turma pertence a outra instituição.");
        }
        if (ator.possuiRole(RoleNome.ADMIN)) {
            return;
        }
        if (ator.possuiRole(RoleNome.PROFESSOR) && lecionaNaTurma(ator, turma.getId())) {
            return;
        }
        throw new AccessDeniedException("Você não leciona nesta turma.");
    }

    public boolean lecionaNaTurma(UsuarioAutenticado ator, Long turmaId) {
        return professorTurmaRepository.existsByProfessorIdAndTurmaId(ator.id(), turmaId);
    }
}
