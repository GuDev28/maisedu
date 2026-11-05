package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.Atividade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AtividadeRepository extends JpaRepository<Atividade, Long> {

    @Query("select a from Atividade a join fetch a.professor where a.turma.id = :turmaId order by a.criadoEm desc")
    List<Atividade> findByTurmaId(@Param("turmaId") Long turmaId);

    /** Quantidade de atividades criadas para a turma (indicador do painel da turma, RF19). */
    @Query("select count(a) from Atividade a where a.turma.id = :turmaId")
    long contarDaTurma(@Param("turmaId") Long turmaId);

    /** Atividades das turmas ativas a que o aluno está vinculado. */
    @Query("select a from Atividade a join fetch a.turma t " +
            "where t.ativo = true and t.id in (select at.turma.id from AlunoTurma at where at.aluno.id = :alunoId) " +
            "order by a.criadoEm desc")
    List<Atividade> findDisponiveisParaAluno(@Param("alunoId") Long alunoId);
}
