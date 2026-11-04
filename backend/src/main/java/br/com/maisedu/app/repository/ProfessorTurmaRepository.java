package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.ProfessorTurma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProfessorTurmaRepository extends JpaRepository<ProfessorTurma, Long> {

    boolean existsByProfessorIdAndTurmaId(Long professorId, Long turmaId);

    List<ProfessorTurma> findByProfessorId(Long professorId);

    @Query("select pt from ProfessorTurma pt join fetch pt.professor p where pt.turma.id = :turmaId order by p.nome")
    List<ProfessorTurma> findByTurmaIdComProfessor(@Param("turmaId") Long turmaId);

    @Modifying
    @Query("delete from ProfessorTurma pt where pt.professor.id = :professorId")
    int excluirVinculosDoProfessor(@Param("professorId") Long professorId);
}
