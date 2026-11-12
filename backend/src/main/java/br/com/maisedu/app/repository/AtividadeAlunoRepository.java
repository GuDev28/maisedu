package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.AtividadeAluno;
import br.com.maisedu.app.model.TipoAtividade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AtividadeAlunoRepository extends JpaRepository<AtividadeAluno, Long> {

    @Query("select aa from AtividadeAluno aa where aa.aluno.id = :alunoId and aa.atividade.id = :atividadeId")
    Optional<AtividadeAluno> findByAlunoIdAndAtividadeId(@Param("alunoId") Long alunoId,
                                                         @Param("atividadeId") Long atividadeId);

    @Query("select aa from AtividadeAluno aa where aa.aluno.id = :alunoId and aa.tipo = :tipo "
            + "order by aa.iniciadaEm desc")
    List<AtividadeAluno> findByAlunoIdAndTipo(@Param("alunoId") Long alunoId, @Param("tipo") TipoAtividade tipo);

    @Query("select aa from AtividadeAluno aa where aa.atividade.id = :atividadeId")
    List<AtividadeAluno> findByAtividadeId(@Param("atividadeId") Long atividadeId);
}
