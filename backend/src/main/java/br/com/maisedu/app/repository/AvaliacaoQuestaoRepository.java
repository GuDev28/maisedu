package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.AvaliacaoQuestao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoQuestaoRepository extends JpaRepository<AvaliacaoQuestao, Long> {
    boolean existsByQuestaoIdAndProfessorId(Long questaoId, Long professorId);
}
