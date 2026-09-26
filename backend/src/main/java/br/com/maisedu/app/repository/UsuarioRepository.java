package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByLogin(String login);

    boolean existsByEmail(String email);

    boolean existsByLogin(String login);

    @Query("select u from Usuario u where u.email = :identificador or u.login = :identificador")
    Optional<Usuario> findByIdentificadorDeAcesso(@Param("identificador") String identificador);

    @Query("select u from Usuario u join u.roles r " +
            "where r.nome = :role and u.instituicao.id = :instituicaoId and u.ativo = true " +
            "and u.id not in (select at.aluno.id from AlunoTurma at where at.turma.id = :turmaId) " +
            "order by u.nome")
    List<Usuario> findAlunosDisponiveisParaTurma(
            @Param("role") RoleNome role, @Param("instituicaoId") Long instituicaoId, @Param("turmaId") Long turmaId);

    @Query("select u from Usuario u join u.roles r " +
            "where r.nome = :role and u.instituicao.id = :instituicaoId and u.ativo = true " +
            "and u.id not in (select pt.professor.id from ProfessorTurma pt where pt.turma.id = :turmaId) " +
            "order by u.nome")
    List<Usuario> findProfessoresDisponiveisParaTurma(
            @Param("role") RoleNome role, @Param("instituicaoId") Long instituicaoId, @Param("turmaId") Long turmaId);
}
