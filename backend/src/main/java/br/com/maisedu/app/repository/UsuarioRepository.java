package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByLogin(String login);

    boolean existsByEmail(String email);

    boolean existsByLogin(String login);

    @Query("select u from Usuario u where u.email = :identificador or u.login = :identificador")
    Optional<Usuario> findByIdentificadorDeAcesso(@Param("identificador") String identificador);
}
