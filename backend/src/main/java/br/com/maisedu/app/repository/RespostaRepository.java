package br.com.maisedu.app.repository;

import br.com.maisedu.app.dto.RespostaHistorico;
import br.com.maisedu.app.model.Resposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RespostaRepository extends JpaRepository<Resposta, Long> {

    @Query("select case when count(r) > 0 then true else false end from Resposta r " +
            "where r.atividadeAlunoQuestao.id = :atividadeAlunoQuestaoId")
    boolean existsByAtividadeAlunoQuestaoId(@Param("atividadeAlunoQuestaoId") Long atividadeAlunoQuestaoId);

    /** Respostas de uma atividade do aluno (para montar a folha com o que já foi respondido). */
    @Query("select r from Resposta r join fetch r.alternativa join fetch r.atividadeAlunoQuestao aaq " +
            "where aaq.atividadeAluno.id = :atividadeAlunoId")
    List<Resposta> findByAtividadeAluno(@Param("atividadeAlunoId") Long atividadeAlunoId);

    /** Ids das questões que o aluno já respondeu, em qualquer atividade ou prática. */
    @Query("select distinct aaq.questao.id from Resposta r join r.atividadeAlunoQuestao aaq " +
            "where aaq.atividadeAluno.aluno.id = :alunoId")
    List<Long> findQuestoesRespondidasPeloAluno(@Param("alunoId") Long alunoId);

    /** Histórico (data + acerto) usado na evolução ao longo do tempo (RF19). */
    @Query("select new br.com.maisedu.app.dto.RespostaHistorico(r.respondidaEm, alt.correta) " +
            "from Resposta r join r.alternativa alt join r.atividadeAlunoQuestao aaq " +
            "where aaq.atividadeAluno.aluno.id = :alunoId order by r.respondidaEm")
    List<RespostaHistorico> findHistoricoDoAluno(@Param("alunoId") Long alunoId);

    /** Exportação de dados do titular: todas as respostas, com questão e atividade. */
    @Query("select r from Resposta r join fetch r.alternativa join fetch r.atividadeAlunoQuestao aaq " +
            "join fetch aaq.atividadeAluno aa left join fetch aa.atividade " +
            "where aa.aluno.id = :alunoId order by r.respondidaEm")
    List<Resposta> findDetalhadasDoAluno(@Param("alunoId") Long alunoId);
}
