package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.StatusQuestao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Questao q where q.id = :id")
    Optional<Questao> findByIdParaVotacao(@Param("id") Long id);

    List<Questao> findByAutorIdOrderByCriadoEmDesc(Long autorId);

    List<Questao> findByStatusAndAutorIdNotOrderByCriadoEmAsc(StatusQuestao status, Long autorId);

    /**
     * Banco compartilhado (RF15): questões no status informado, de qualquer autor e instituição,
     * com filtros opcionais.
     */
    @Query("select q from Questao q join fetch q.topico t join fetch q.autor " +
            "where q.status = :status " +
            "and (:topicoId is null or t.id = :topicoId) " +
            "and (:nivel is null or q.nivelDificuldade = :nivel) " +
            "and (:anoEscolar is null or q.anoEscolar = :anoEscolar) " +
            "order by t.nome, q.nivelDificuldade, q.id")
    List<Questao> buscarNoBanco(@Param("status") StatusQuestao status,
                                @Param("topicoId") Long topicoId,
                                @Param("nivel") NivelDificuldade nivel,
                                @Param("anoEscolar") AnoEscolar anoEscolar);

    /** Candidatas do motor adaptativo: todas as questões de um tópico em um status (todos os níveis). */
    @Query("select q from Questao q where q.status = :status and q.topico.id = :topicoId")
    List<Questao> findByStatusAndTopicoId(@Param("status") StatusQuestao status, @Param("topicoId") Long topicoId);

    @Query("select q from Questao q where q.id in :ids")
    List<Questao> findAllByIdIn(@Param("ids") List<Long> ids);
}
