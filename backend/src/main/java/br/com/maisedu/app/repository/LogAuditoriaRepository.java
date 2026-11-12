package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.LogAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    /** Consulta do admin: sempre restrita à instituição dele; os demais filtros são opcionais. */
    @Query("""
            select l from LogAuditoria l
            where l.instituicaoId = :instituicaoId
              and (:usuarioId is null or l.usuarioId = :usuarioId)
              and (:acao is null or l.acao = :acao)
              and (:de is null or l.criadoEm >= :de)
              and (:ate is null or l.criadoEm <= :ate)
            """)
    Page<LogAuditoria> buscar(@Param("instituicaoId") Long instituicaoId,
                               @Param("usuarioId") Long usuarioId,
                               @Param("acao") AcaoAuditoria acao,
                               @Param("de") LocalDateTime de,
                               @Param("ate") LocalDateTime ate,
                               Pageable pageable);

    List<LogAuditoria> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    /** Retenção: remove os registros mais antigos que o limite configurado. */
    @Modifying
    @Query("delete from LogAuditoria l where l.criadoEm < :limite")
    int excluirAnterioresA(@Param("limite") LocalDateTime limite);

    /** Anonimização: troca o nome e apaga o identificador digitado nos registros do usuário. */
    @Modifying
    @Query("update LogAuditoria l set l.usuarioNome = :nome, l.identificadorAcesso = null "
            + "where l.usuarioId = :usuarioId")
    int anonimizarRegistrosDoUsuario(@Param("usuarioId") Long usuarioId, @Param("nome") String nome);

    /** Anonimização: remove detalhes que podem conter dado pessoal (ex.: e-mail no ENVIO_EMAIL). */
    @Modifying
    @Query("update LogAuditoria l set l.detalhes = :detalhes where l.usuarioId = :usuarioId and l.acao = :acao")
    int substituirDetalhes(@Param("usuarioId") Long usuarioId, @Param("acao") AcaoAuditoria acao,
                           @Param("detalhes") String detalhes);

    /**
     * Anonimização: limpa detalhes de registros feitos por outros usuários sobre esta pessoa
     * (ex.: correção do nome).
     */
    @Modifying
    @Query("update LogAuditoria l set l.detalhes = :detalhes " +
            "where l.entidade = :entidade and l.entidadeId = :entidadeId and l.acao = :acao")
    int substituirDetalhesSobreEntidade(@Param("entidade") String entidade, @Param("entidadeId") Long entidadeId,
                                        @Param("acao") AcaoAuditoria acao, @Param("detalhes") String detalhes);
}
