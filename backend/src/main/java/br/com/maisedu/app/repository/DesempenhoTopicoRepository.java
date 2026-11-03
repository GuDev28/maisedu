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
}
