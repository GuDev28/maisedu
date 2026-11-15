package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.AvaliacaoQuestao;
import br.com.maisedu.app.model.VotoAvaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AvaliacaoQuestaoRepository extends JpaRepository<AvaliacaoQuestao, Long> {

    boolean existsByQuestaoIdAndProfessorId(Long questaoId, Long professorId);

    long countByQuestaoId(Long questaoId);

    long countByQuestaoIdAndVoto(Long questaoId, VotoAvaliacao voto);

    /** Votos das questões informadas (placar e motivos de rejeição em "Minhas questões"), numa consulta. */
    @Query("select a from AvaliacaoQuestao a where a.questao.id in :questaoIds order by a.avaliadoEm")
    List<AvaliacaoQuestao> findByQuestaoIdIn(@Param("questaoIds") Collection<Long> questaoIds);
}
