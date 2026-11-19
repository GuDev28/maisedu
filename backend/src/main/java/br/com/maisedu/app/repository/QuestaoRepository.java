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
import java.util.Collection;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Questao q where q.id = :id")
    Optional<Questao> findByIdParaVotacao(@Param("id") Long id);

    List<Questao> findByAutorIdOrderByCriadoEmDesc(Long autorId);

    /**
     * Fila de avaliação do professor (RN02): questões pendentes de outros autores em que ele ainda
     * não votou, numa única consulta.
     */
    @Query("select q from Questao q join fetch q.topico join fetch q.autor " +
            "where q.status = :status and q.autor.id <> :professorId " +
            "and not exists (select a.id from AvaliacaoQuestao a where a.questao = q and a.professor.id = :professorId) " +
            "order by q.criadoEm asc")
    List<Questao> findPendentesParaAvaliar(@Param("status") StatusQuestao status,
                                           @Param("professorId") Long professorId);

    long countByStatus(StatusQuestao status);

    /** Banco compartilhado (RF15): questões no status informado, de qualquer autor e instituição, com filtros opcionais. */
    @Query("select q from Questao q join fetch q.topico t join fetch q.autor a join fetch a.instituicao " +
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

    /** Candidatas da prática adaptativa: questões no status informado de vários tópicos, com o tópico. */
    @Query("select q from Questao q join fetch q.topico where q.status = :status and q.topico.id in :topicoIds")
    List<Questao> findByStatusAndTopicoIdIn(@Param("status") StatusQuestao status,
                                           @Param("topicoIds") Collection<Long> topicoIds);

    /** Questões no status informado por tópico, só dos anos escolares informados (cards da prática por tópico). */
    @Query("select new br.com.maisedu.app.repository.Projecoes$ContagemTopico(t.id, t.nome, count(q)) " +
            "from Questao q join q.topico t where q.status = :status and q.anoEscolar in :anos " +
            "group by t.id, t.nome order by t.nome")
    List<Projecoes.ContagemTopico> contarPorTopicoNosAnos(@Param("status") StatusQuestao status,
                                                         @Param("anos") Collection<AnoEscolar> anos);

    /** Questões no status informado por tópico, de todos os anos. */
    @Query("select new br.com.maisedu.app.repository.Projecoes$ContagemTopico(t.id, t.nome, count(q)) " +
            "from Questao q join q.topico t where q.status = :status group by t.id, t.nome order by t.nome")
    List<Projecoes.ContagemTopico> contarPorTopico(@Param("status") StatusQuestao status);

    /** Questões de autores da escola, por status (painel: participação no banco compartilhado). */
    @Query("select new br.com.maisedu.app.repository.Projecoes$ContagemStatusQuestao(q.status, count(q)) " +
            "from Questao q where q.autor.instituicao.id = :instituicaoId group by q.status")
    List<Projecoes.ContagemStatusQuestao> contarDaInstituicaoPorStatus(@Param("instituicaoId") Long instituicaoId);
}
