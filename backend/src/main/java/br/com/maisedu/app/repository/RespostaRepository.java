package br.com.maisedu.app.repository;

import br.com.maisedu.app.dto.RespostaHistorico;
import br.com.maisedu.app.model.Resposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;

public interface RespostaRepository extends JpaRepository<Resposta, Long> {

    @Query("select case when count(r) > 0 then true else false end from Resposta r " +
            "where r.atividadeAlunoQuestao.id = :atividadeAlunoQuestaoId")
    boolean existsByAtividadeAlunoQuestaoId(@Param("atividadeAlunoQuestaoId") Long atividadeAlunoQuestaoId);

    /** Respostas de uma atividade do aluno (para montar a folha com o que já foi respondido). */
    @Query("select r from Resposta r join fetch r.alternativa join fetch r.atividadeAlunoQuestao aaq " +
            "where aaq.atividadeAluno.id = :atividadeAlunoId")
    List<Resposta> findByAtividadeAluno(@Param("atividadeAlunoId") Long atividadeAlunoId);

    /** Respondidas e acertos de várias folhas de uma vez (listas de atividades), sem uma consulta por folha. */
    @Query("select new br.com.maisedu.app.repository.PlacarAtividade(aaq.atividadeAluno.id, count(r), " +
            "sum(case when alt.correta = true then 1 else 0 end)) " +
            "from Resposta r join r.alternativa alt join r.atividadeAlunoQuestao aaq " +
            "where aaq.atividadeAluno.id in :atividadeAlunoIds group by aaq.atividadeAluno.id")
    List<PlacarAtividade> contarPorAtividadeAluno(@Param("atividadeAlunoIds") Collection<Long> atividadeAlunoIds);

    /** Ids das questões que o aluno já respondeu, em qualquer atividade ou prática. */
    @Query("select distinct aaq.questao.id from Resposta r join r.atividadeAlunoQuestao aaq " +
            "where aaq.atividadeAluno.aluno.id = :alunoId")
    List<Long> findQuestoesRespondidasPeloAluno(@Param("alunoId") Long alunoId);

    /**
     * Histórico (data + acerto) usado na evolução ao longo do tempo (RF19). Só respostas já
     * contabilizadas: as de atividades com prazo em aberto não revelam o resultado antes da hora.
     */
    @Query("select new br.com.maisedu.app.dto.RespostaHistorico(r.respondidaEm, alt.correta) " +
            "from Resposta r join r.alternativa alt join r.atividadeAlunoQuestao aaq " +
            "where aaq.atividadeAluno.aluno.id = :alunoId and r.contabilizadaEm is not null " +
            "order by r.respondidaEm")
    List<RespostaHistorico> findHistoricoDoAluno(@Param("alunoId") Long alunoId);

    /** Exportação de dados do titular: todas as respostas, com questão e atividade. */
    @Query("select r from Resposta r join fetch r.alternativa join fetch r.atividadeAlunoQuestao aaq " +
            "join fetch aaq.atividadeAluno aa left join fetch aa.atividade " +
            "where aa.aluno.id = :alunoId order by r.respondidaEm")
    List<Resposta> findDetalhadasDoAluno(@Param("alunoId") Long alunoId);

    /** Respostas enviadas de uma atividade que ainda não entraram no desempenho (fechamento do prazo). */
    @Query("select r from Resposta r join fetch r.alternativa join fetch r.atividadeAlunoQuestao aaq " +
            "join fetch aaq.questao q join fetch q.topico join fetch aaq.atividadeAluno aa join fetch aa.aluno " +
            "where aa.atividade.id = :atividadeId and r.contabilizadaEm is null")
    List<Resposta> findNaoContabilizadasDaAtividade(@Param("atividadeId") Long atividadeId);

    /** Quantas vezes cada alternativa foi escolhida em cada questão da atividade (análise do professor). */
    @Query("select new br.com.maisedu.app.repository.Projecoes$ContagemAlternativa(aaq.questao.id, alt.id, count(r)) " +
            "from Resposta r join r.alternativa alt join r.atividadeAlunoQuestao aaq join aaq.atividadeAluno aa " +
            "where aa.atividade.id = :atividadeId group by aaq.questao.id, alt.id")
    List<Projecoes.ContagemAlternativa> contarAlternativasDaAtividade(@Param("atividadeId") Long atividadeId);

    /** Respostas contabilizadas de vários alunos (evolução semanal da turma). */
    @Query("select new br.com.maisedu.app.repository.Projecoes$RespostaDoAluno(aa.aluno.id, r.respondidaEm, alt.correta) " +
            "from Resposta r join r.alternativa alt join r.atividadeAlunoQuestao aaq join aaq.atividadeAluno aa " +
            "where aa.aluno.id in :alunoIds and r.contabilizadaEm is not null order by r.respondidaEm")
    List<Projecoes.RespostaDoAluno> findHistoricoDosAlunos(@Param("alunoIds") Collection<Long> alunoIds);

    /** Respostas contabilizadas com tópico e tipo da folha (efeito da prática adaptativa na turma). */
    @Query("select new br.com.maisedu.app.repository.Projecoes$RespostaPorTopico(aa.aluno.id, t.id, t.nome, " +
            "r.respondidaEm, alt.correta, aa.tipo) " +
            "from Resposta r join r.alternativa alt join r.atividadeAlunoQuestao aaq join aaq.questao q " +
            "join q.topico t join aaq.atividadeAluno aa " +
            "where aa.aluno.id in :alunoIds and r.contabilizadaEm is not null order by r.respondidaEm")
    List<Projecoes.RespostaPorTopico> findPorTopicoDosAlunos(@Param("alunoIds") Collection<Long> alunoIds);

    /** Respostas dos alunos da escola desde uma data (engajamento semanal do painel). */
    @Query("select new br.com.maisedu.app.repository.Projecoes$RespostaDoAluno(aa.aluno.id, r.respondidaEm, alt.correta) " +
            "from Resposta r join r.alternativa alt join r.atividadeAlunoQuestao aaq join aaq.atividadeAluno aa " +
            "where aa.aluno.instituicao.id = :instituicaoId and r.respondidaEm >= :desde")
    List<Projecoes.RespostaDoAluno> findDaInstituicaoDesde(@Param("instituicaoId") Long instituicaoId,
                                                         @Param("desde") LocalDateTime desde);

    /** Alunos distintos que responderam algo desde a data, por turma da escola. */
    @Query("select new br.com.maisedu.app.repository.Projecoes$ContagemPorId(at.turma.id, count(distinct aa.aluno.id)) " +
            "from Resposta r join r.atividadeAlunoQuestao aaq join aaq.atividadeAluno aa " +
            "join AlunoTurma at on at.aluno.id = aa.aluno.id " +
            "where at.turma.instituicao.id = :instituicaoId and r.respondidaEm >= :desde group by at.turma.id")
    List<Projecoes.ContagemPorId> contarAlunosAtivosPorTurma(@Param("instituicaoId") Long instituicaoId,
                                                           @Param("desde") LocalDateTime desde);
}
