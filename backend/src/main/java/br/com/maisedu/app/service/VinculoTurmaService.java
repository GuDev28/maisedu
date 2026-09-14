package br.com.maisedu.app.service;

import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.AlunoTurma;
import br.com.maisedu.app.model.ProfessorTurma;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlunoTurmaRepository;
import br.com.maisedu.app.repository.ProfessorTurmaRepository;
import br.com.maisedu.app.repository.TurmaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VinculoTurmaService {

    private final UsuarioRepository usuarioRepository;
    private final TurmaRepository turmaRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final ProfessorTurmaRepository professorTurmaRepository;

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
    public AlunoTurma vincularAluno(Long alunoId, Long turmaId) {
        Usuario aluno = buscarUsuario(alunoId);
        Turma turma = buscarTurma(turmaId);

        validarElegibilidade(aluno, turma, RoleNome.ALUNO);

        if (alunoTurmaRepository.existsByAlunoIdAndTurmaId(alunoId, turmaId)) {
            throw new NegocioException("Aluno já vinculado a esta turma.");
        }

        return alunoTurmaRepository.save(new AlunoTurma(aluno, turma));
    }

    @Transactional
    public ProfessorTurma vincularProfessor(Long professorId, Long turmaId) {
        Usuario professor = buscarUsuario(professorId);
        Turma turma = buscarTurma(turmaId);

        validarElegibilidade(professor, turma, RoleNome.PROFESSOR);

        if (professorTurmaRepository.existsByProfessorIdAndTurmaId(professorId, turmaId)) {
            throw new NegocioException("Professor já vinculado a esta turma.");
        }

        return professorTurmaRepository.save(new ProfessorTurma(professor, turma));
    }
}
