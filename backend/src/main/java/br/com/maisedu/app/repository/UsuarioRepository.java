package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.security.EstadoSessao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    /**
     * Estado mínimo conferido pelo filtro JWT a cada requisição: conta ativa, versão da sessão,
     * senha provisória e versão dos termos aceita. Consulta pela chave primária, sem carregar a entidade.
     */
    @Query("select new br.com.maisedu.app.security.EstadoSessao(u.ativo, u.versaoSessao, u.senhaTemporaria, u.termosVersao) "
            + "from Usuario u where u.id = :id")
    Optional<EstadoSessao> buscarEstadoSessao(@Param("id") Long id);

    /** Trava a linha do usuário até o fim da transação: serializa as respostas e práticas de um mesmo aluno. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findByIdParaAtualizacao(@Param("id") Long id);

    @Query("select u.instituicao.id from Usuario u where u.id = :id")
    Optional<Long> findInstituicaoIdById(@Param("id") Long id);

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

    /** Usuários da instituição (tela de gestão do admin), opcionalmente filtrados por papel. */
    @Query("select distinct u from Usuario u join u.roles r " +
            "where u.instituicao.id = :instituicaoId and (:papel is null or r.nome = :papel) " +
            "order by u.nome")
    List<Usuario> findDaInstituicao(@Param("instituicaoId") Long instituicaoId, @Param("papel") RoleNome papel);

    /** Alunos vinculados às turmas em que o professor leciona. */
    @Query("select distinct u from AlunoTurma at join at.aluno u " +
            "where at.turma.id in (select pt.turma.id from ProfessorTurma pt where pt.professor.id = :professorId) " +
            "order by u.nome")
    List<Usuario> findAlunosDasTurmasDoProfessor(@Param("professorId") Long professorId);
}
