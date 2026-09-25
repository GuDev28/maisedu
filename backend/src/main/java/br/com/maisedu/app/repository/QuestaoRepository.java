package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.Questao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {

    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Questao q where q.id = :id")
    Optional<Questao> findByIdParaVotacao(@Param("id") Long id);
}
