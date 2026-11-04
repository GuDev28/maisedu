package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.AlunoTurma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlunoTurmaRepository extends JpaRepository<AlunoTurma, Long> {

    boolean existsByAlunoIdAndTurmaId(Long alunoId, Long turmaId);

    @Query("select at from AlunoTurma at join fetch at.aluno a where at.turma.id = :turmaId order by a.nome")
    List<AlunoTurma> findByTurmaIdComAluno(@Param("turmaId") Long turmaId);

    @Query("select at from AlunoTurma at join fetch at.turma where at.aluno.id = :alunoId")
    List<AlunoTurma> findByAlunoIdComTurma(@Param("alunoId") Long alunoId);

    /** O aluno está em alguma turma em que o professor leciona? */
    @Query("select case when count(at) > 0 then true else false end from AlunoTurma at " +
            "where at.aluno.id = :alunoId and at.turma.id in " +
            "(select pt.turma.id from ProfessorTurma pt where pt.professor.id = :professorId)")
    boolean alunoEstaEmTurmaDoProfessor(@Param("alunoId") Long alunoId, @Param("professorId") Long professorId);

    @Modifying
    @Query("delete from AlunoTurma at where at.aluno.id = :alunoId")
    int excluirVinculosDoAluno(@Param("alunoId") Long alunoId);
}
