package br.com.maisedu.app.service;

import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.AlunoTurma;
import br.com.maisedu.app.model.ProfessorTurma;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlunoTurmaRepository;
import br.com.maisedu.app.repository.ProfessorTurmaRepository;
import br.com.maisedu.app.repository.TurmaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VinculoTurmaService {

    private final UsuarioRepository usuarioRepository;
    private final TurmaRepository turmaRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final ProfessorTurmaRepository professorTurmaRepository;
    private final AuditoriaService auditoriaService;

    private void validarElegibilidade(Usuario usuario, Turma turma, RoleNome roleEsperada) {
        if (!usuario.isAtivo()) {
            throw new NegocioException("Usuário inativo não pode ser vinculado.");
        }

        if (!turma.isAtivo()) {
            throw new NegocioException("Turma inativa não aceita novos vínculos.");
        }

        if (!usuario.possuiRole(roleEsperada)) {
            throw new NegocioException(
                    "Usuário não possui o papel de " + roleEsperada + " necessário para este vínculo.");
        }

        if (!usuario.getInstituicao().getId().equals(turma.getInstituicao().getId())) {
            throw new NegocioException(
                    "Usuário e turma pertencem a instituições diferentes.");
        }
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NegocioException("Usuário não encontrado: " + id));
    }
    private Turma buscarTurma(Long id) {
        return turmaRepository.findById(id)
                .orElseThrow(() -> new NegocioException("Turma não encontrada: " + id));
    }

    @Transactional
    public AlunoTurma vincularAluno(Long alunoId, Long turmaId, UsuarioAutenticado ator) {
        Turma turma = buscarTurma(turmaId);
        autorizarAtorNaTurma(ator, turma);

        Usuario aluno = buscarUsuario(alunoId);
        validarElegibilidade(aluno, turma, RoleNome.ALUNO);

        if (alunoTurmaRepository.existsByAlunoIdAndTurmaId(alunoId, turmaId)) {
            throw new NegocioException("Aluno já vinculado a esta turma.");
        }

        AlunoTurma vinculo = alunoTurmaRepository.save(new AlunoTurma(aluno, turma));
        auditoriaService.registrar(ator, AcaoAuditoria.VINCULO_ALUNO_TURMA, "Turma", turmaId,
                true, "aluno " + alunoId);
        return vinculo;
    }

    /** Vincular um professor à turma é exclusivo do admin (decisão do grupo: só o admin cria/gerencia turmas). */
    @Transactional
    public ProfessorTurma vincularProfessor(Long professorId, Long turmaId, UsuarioAutenticado ator) {
        if (!ator.possuiRole(RoleNome.ADMIN)) {
            throw new AccessDeniedException("Só o admin vincula professores a turmas.");
        }

        Usuario professor = buscarUsuario(professorId);
        Turma turma = buscarTurma(turmaId);

        if (!ator.instituicaoId().equals(turma.getInstituicao().getId())) {
            throw new AccessDeniedException("Turma pertence a outra instituição.");
        }

        validarElegibilidade(professor, turma, RoleNome.PROFESSOR);

        if (professorTurmaRepository.existsByProfessorIdAndTurmaId(professorId, turmaId)) {
            throw new NegocioException("Professor já vinculado a esta turma.");
        }

        ProfessorTurma vinculo = professorTurmaRepository.save(new ProfessorTurma(professor, turma));
        auditoriaService.registrar(ator, AcaoAuditoria.VINCULO_PROFESSOR_TURMA, "Turma", turmaId,
                true, "professor " + professorId);
        return vinculo;
    }

    /**
     * Turmas em que o ator pode atuar: todas as da instituição para o admin,
     * ou só as que o professor leciona (mesma regra de {@link #autorizarAtorNaTurma}).
     * Usado para popular o seletor de turma nas telas de vínculo.
     */
    public List<Turma> listarTurmasParaVinculo(UsuarioAutenticado ator) {
        if (ator.possuiRole(RoleNome.ADMIN)) {
            return turmaRepository.findByInstituicaoIdAndAtivoTrueOrderByNomeAsc(ator.instituicaoId());
        }

        List<Turma> turmas = new ArrayList<>();
        for (ProfessorTurma vinculo : professorTurmaRepository.findByProfessorId(ator.id())) {
            if (vinculo.getTurma().isAtivo()) {
                turmas.add(vinculo.getTurma());
            }
        }
        return turmas;
    }

    /** Alunos ainda não vinculados à turma informada, para a tela "Vincular aluno". */
    public List<Usuario> listarAlunosDisponiveis(Long turmaId, UsuarioAutenticado ator) {
        Turma turma = buscarTurma(turmaId);
        autorizarAtorNaTurma(ator, turma);
        return usuarioRepository.findAlunosDisponiveisParaTurma(RoleNome.ALUNO, turma.getInstituicao().getId(), turmaId);
    }

    /** Professores ainda não vinculados à turma informada, para a tela "Vincular professor" (só admin). */
    public List<Usuario> listarProfessoresDisponiveis(Long turmaId, UsuarioAutenticado ator) {
        if (!ator.possuiRole(RoleNome.ADMIN)) {
            throw new AccessDeniedException("Só o admin vincula professores a turmas.");
        }

        Turma turma = buscarTurma(turmaId);
        if (!ator.instituicaoId().equals(turma.getInstituicao().getId())) {
            throw new AccessDeniedException("Turma pertence a outra instituição.");
        }

        return usuarioRepository.findProfessoresDisponiveisParaTurma(
                RoleNome.PROFESSOR, turma.getInstituicao().getId(), turmaId);
    }

    private void autorizarAtorNaTurma(UsuarioAutenticado ator, Turma turma) {
        if (ator.possuiRole(RoleNome.ADMIN)) {
            if (!ator.instituicaoId().equals(turma.getInstituicao().getId())) {
                throw new AccessDeniedException("Turma pertence a outra instituição.");
            }
            return;
        }
        if (ator.possuiRole(RoleNome.PROFESSOR)
                && professorTurmaRepository.existsByProfessorIdAndTurmaId(ator.id(), turma.getId())) {
            return;
        }
        throw new AccessDeniedException("Você não leciona nesta turma.");
    }
}
