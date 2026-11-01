package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.Turma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TurmaRepository extends JpaRepository<Turma, Long> {

    List<Turma> findByInstituicaoIdAndAtivoTrueOrderByNomeAsc(Long instituicaoId);

    /** Gestão de turmas do admin: ativas primeiro, depois as inativas, em ordem alfabética. */
    List<Turma> findByInstituicaoIdOrderByAtivoDescNomeAsc(Long instituicaoId);

    boolean existsByInstituicaoIdAndNomeIgnoreCase(Long instituicaoId, String nome);
}
