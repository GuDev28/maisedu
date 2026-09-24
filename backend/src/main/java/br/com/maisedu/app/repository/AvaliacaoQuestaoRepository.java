package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.AvaliacaoQuestao;
import br.com.maisedu.app.model.VotoAvaliacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoQuestaoRepository extends JpaRepository<AvaliacaoQuestao, Long> {

    boolean existsByQuestaoIdAndProfessorId(Long questaoId, Long professorId);

    long countByQuestaoId(Long questaoId);

    long countByQuestaoIdAndVoto(Long questaoId, VotoAvaliacao voto);
}
