package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.Atividade;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

public interface AtividadeRepository extends JpaRepository<Atividade, Long> {

    @Query("select a from Atividade a join fetch a.professor where a.turma.id = :turmaId order by a.criadoEm desc")
    List<Atividade> findByTurmaId(@Param("turmaId") Long turmaId);

    /** Atividades das turmas ativas a que o aluno está vinculado. */
    @Query("select a from Atividade a join fetch a.turma t " +
            "where t.ativo = true and t.id in (select at.turma.id from AlunoTurma at where at.aluno.id = :alunoId) " +
            "order by a.criadoEm desc")
    List<Atividade> findDisponiveisParaAluno(@Param("alunoId") Long alunoId);

    /** Atividades com prazo vencido cujo desempenho ainda não foi contabilizado (rotina de fechamento). */
    @Query("select a.id from Atividade a where a.fechadaEm is null and a.prazo <= :agora order by a.prazo")
    List<Long> findIdsVencidasNaoFechadas(@Param("agora") LocalDateTime agora);

    /** Trava a atividade durante o fechamento: duas execuções simultâneas não contabilizam duas vezes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Atividade a where a.id = :id")
    Optional<Atividade> findByIdParaFechamento(@Param("id") Long id);

    // ---------- Painel da escola (só contagens) ----------

    @Query("select new br.com.maisedu.app.repository.Projecoes$ContagemPorId(a.turma.id, count(a)) " +
            "from Atividade a where a.turma.instituicao.id = :instituicaoId group by a.turma.id")
    List<Projecoes.ContagemPorId> contarPorTurma(@Param("instituicaoId") Long instituicaoId);

    @Query("select a.criadoEm from Atividade a where a.turma.instituicao.id = :instituicaoId and a.criadoEm >= :desde")
    List<LocalDateTime> findDatasDeCriacao(@Param("instituicaoId") Long instituicaoId,
                                           @Param("desde") LocalDateTime desde);

    /** Vezes em que questões de autores da escola entraram em atividades de outras escolas. */
    @Query("select count(aq) from AtividadeQuestao aq where aq.questao.autor.instituicao.id = :instituicaoId " +
            "and aq.atividade.turma.instituicao.id <> :instituicaoId")
    long contarUsosPorOutrasEscolas(@Param("instituicaoId") Long instituicaoId);

    /** Questões de autores da escola usadas em atividades da própria escola. */
    @Query("select count(aq) from AtividadeQuestao aq where aq.questao.autor.instituicao.id = :instituicaoId " +
            "and aq.atividade.turma.instituicao.id = :instituicaoId")
    long contarUsosNaPropriaEscola(@Param("instituicaoId") Long instituicaoId);

    /** Questões distintas de outras escolas que a escola usou nas suas atividades. */
    @Query("select count(distinct aq.questao.id) from AtividadeQuestao aq " +
            "where aq.atividade.turma.instituicao.id = :instituicaoId " +
            "and aq.questao.autor.instituicao.id <> :instituicaoId")
    long contarQuestoesDeOutrasEscolasUsadas(@Param("instituicaoId") Long instituicaoId);
}
