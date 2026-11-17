package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.DesempenhoTopico;
import br.com.maisedu.app.model.NivelDificuldade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DesempenhoTopicoRepository extends JpaRepository<DesempenhoTopico, Long> {

    @Query("select d from DesempenhoTopico d where d.aluno.id = :alunoId and d.topico.id = :topicoId " +
            "and d.nivelDificuldade = :nivel")
    Optional<DesempenhoTopico> buscar(@Param("alunoId") Long alunoId, @Param("topicoId") Long topicoId,
                                      @Param("nivel") NivelDificuldade nivel);

    @Query("select d from DesempenhoTopico d join fetch d.topico where d.aluno.id = :alunoId")
    List<DesempenhoTopico> findByAlunoId(@Param("alunoId") Long alunoId);

    @Query("select d from DesempenhoTopico d join fetch d.topico where d.aluno.id in :alunoIds")
    List<DesempenhoTopico> findByAlunoIdIn(@Param("alunoIds") Collection<Long> alunoIds);

    /**
     * Painel da escola (minimização): desempenho somado por ano escolar das turmas ativas e tópico,
     * sem identificar alunos.
     */
    @Query("select new br.com.maisedu.app.repository.Projecoes$DesempenhoAnoTopico(t.anoEscolar, tp.id, tp.nome, " +
            "sum(d.totalRespostas), sum(d.totalAcertos), count(distinct d.aluno.id)) " +
            "from DesempenhoTopico d join d.topico tp join AlunoTurma at on at.aluno.id = d.aluno.id join at.turma t " +
            "where t.instituicao.id = :instituicaoId and t.ativo = true " +
            "group by t.anoEscolar, tp.id, tp.nome")
    List<Projecoes.DesempenhoAnoTopico> somarPorAnoETopico(@Param("instituicaoId") Long instituicaoId);

    /** Painel da escola: desempenho somado dos alunos de cada turma ativa (somas nulas se ninguém respondeu). */
    @Query("select new br.com.maisedu.app.repository.Projecoes$DesempenhoDaTurma(t.id, t.nome, t.anoEscolar, " +
            "count(distinct at.aluno.id), count(distinct d.aluno.id), sum(d.totalRespostas), sum(d.totalAcertos)) " +
            "from AlunoTurma at join at.turma t left join DesempenhoTopico d on d.aluno.id = at.aluno.id " +
            "where t.instituicao.id = :instituicaoId and t.ativo = true " +
            "group by t.id, t.nome, t.anoEscolar order by t.nome")
    List<Projecoes.DesempenhoDaTurma> somarPorTurma(@Param("instituicaoId") Long instituicaoId);
}
