package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.Topico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicoRepository extends JpaRepository<Topico, Long> {
}
