package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.Questao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {
}
