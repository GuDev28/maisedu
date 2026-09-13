package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.AlunoTurma;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlunoTurmaRepository extends JpaRepository<AlunoTurma, Long> {

    boolean existsByAlunoIdAndTurmaId(Long alunoId, Long turmaId);

}
