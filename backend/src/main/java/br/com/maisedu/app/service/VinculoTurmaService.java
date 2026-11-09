package br.com.maisedu.app.service;

import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * RN01: vínculo e elegibilidade aluno-turma-professor. O admin vincula e desvincula professores; o
 * admin e o professor da turma vinculam e desvinculam alunos. Quem pode mexer em cada turma é
 * decidido por {@link AcessoTurma}.
 */
@Service
@RequiredArgsConstructor
public class VinculoTurmaService {

    private static final String SO_ADMIN_VINCULA_PROFESSOR = "Só o admin vincula professores a turmas.";

    private final UsuarioRepository usuarioRepository;
    private final TurmaRepository turmaRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final ProfessorTurmaRepository professorTurmaRepository;
    private final AuditoriaService auditoriaService;
    private final AcessoTurma acessoTurma;

    /** Membros da turma, ordenados por nome: professores que lecionam e alunos vinculados. */
    public record MembrosTurma(Turma turma, List<Usuario> professores, List<Usuario> alunos) {
    }

    @Transactional
    public AlunoTurma vincularAluno(Long alunoId, Long turmaId, UsuarioAutenticado ator) {
        Turma turma = acessoTurma.buscarComAcesso(ator, turmaId);
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
        Turma turma = acessoTurma.buscarComoAdmin(ator, turmaId, SO_ADMIN_VINCULA_PROFESSOR);
        Usuario professor = buscarUsuario(professorId);
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
     * Tira o aluno da turma (ex.: mudou de turma). O histórico dele (respostas e desempenho) é
     * mantido; ele só deixa de ver as atividades desta turma.
     */
    @Transactional
    public void desvincularAluno(Long alunoId, Long turmaId, UsuarioAutenticado ator) {
        acessoTurma.buscarComAcesso(ator, turmaId);
        AlunoTurma vinculo = alunoTurmaRepository.findByAlunoIdAndTurmaId(alunoId, turmaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Este aluno não está vinculado a esta turma."));
        alunoTurmaRepository.delete(vinculo);
        auditoriaService.registrar(ator, AcaoAuditoria.DESVINCULO_ALUNO_TURMA, "Turma", turmaId,
                true, "aluno " + alunoId);
    }

    /** Tira o professor da turma. As atividades que ele criou continuam na turma, com a autoria dele. */
    @Transactional
    public void desvincularProfessor(Long professorId, Long turmaId, UsuarioAutenticado ator) {
        acessoTurma.buscarComoAdmin(ator, turmaId, SO_ADMIN_VINCULA_PROFESSOR);
        ProfessorTurma vinculo = professorTurmaRepository.findByProfessorIdAndTurmaId(professorId, turmaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Este professor não está vinculado a esta turma."));
        professorTurmaRepository.delete(vinculo);
        auditoriaService.registrar(ator, AcaoAuditoria.DESVINCULO_PROFESSOR_TURMA, "Turma", turmaId,
                true, "professor " + professorId);
    }

    @Transactional(readOnly = true)
    public MembrosTurma listarMembros(Long turmaId, UsuarioAutenticado ator) {
        Turma turma = acessoTurma.buscarComAcesso(ator, turmaId);
        List<Usuario> professores = new ArrayList<>();
        for (ProfessorTurma vinculo : professorTurmaRepository.findByTurmaIdComProfessor(turmaId)) {
            professores.add(vinculo.getProfessor());
        }
        List<Usuario> alunos = new ArrayList<>();
        for (AlunoTurma vinculo : alunoTurmaRepository.findByTurmaIdComAluno(turmaId)) {
            alunos.add(vinculo.getAluno());
        }
        return new MembrosTurma(turma, professores, alunos);
    }

    /**
     * Turmas em que o ator pode atuar: todas as ativas da instituição para o admin, ou só as que o
     * professor leciona. Usado nos seletores de turma e na tela "Turmas e alunos".
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

    /** Alunos da instituição ainda não vinculados à turma informada. */
    public List<Usuario> listarAlunosDisponiveis(Long turmaId, UsuarioAutenticado ator) {
        Turma turma = acessoTurma.buscarComAcesso(ator, turmaId);
        return usuarioRepository.findAlunosDisponiveisParaTurma(RoleNome.ALUNO, turma.getInstituicao().getId(), turmaId);
    }

    /** Professores da instituição ainda não vinculados à turma informada (só admin). */
    public List<Usuario> listarProfessoresDisponiveis(Long turmaId, UsuarioAutenticado ator) {
        Turma turma = acessoTurma.buscarComoAdmin(ator, turmaId, SO_ADMIN_VINCULA_PROFESSOR);
        return usuarioRepository.findProfessoresDisponiveisParaTurma(
                RoleNome.PROFESSOR, turma.getInstituicao().getId(), turmaId);
    }

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
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }
}
