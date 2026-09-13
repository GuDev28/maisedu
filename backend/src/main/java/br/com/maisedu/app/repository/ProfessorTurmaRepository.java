package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.ProfessorTurma;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessorTurmaRepository extends JpaRepository<ProfessorTurma, Long> {

    boolean existsByProfessorIdAndTurmaId(Long professorId, Long turmaId);

}
