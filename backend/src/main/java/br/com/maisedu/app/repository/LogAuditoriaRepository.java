package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A consulta da tela de auditoria usa {@link JpaSpecificationExecutor}: só os filtros informados entram
 * no SQL. O formato antigo, {@code (:de is null or l.criadoEm >= :de)}, falhava no PostgreSQL com
 * filtro de data vazio ("could not determine data type of parameter"): o driver manda data nula sem
 * tipo e o banco não consegue deduzi-lo. No H2 dos testes funcionava, por isso passou despercebido.
 */
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long>,
        JpaSpecificationExecutor<LogAuditoria> {

    List<LogAuditoria> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    /** Página Início do admin: quantos registros bem-sucedidos de uma ação desde a data informada. */
    long countByInstituicaoIdAndAcaoAndSucessoTrueAndCriadoEmAfter(Long instituicaoId, AcaoAuditoria acao,
                                                                    LocalDateTime desde);

    /** Retenção: remove os registros mais antigos que o limite configurado. */
    @Modifying
    @Query("delete from LogAuditoria l where l.criadoEm < :limite")
    int excluirAnterioresA(@Param("limite") LocalDateTime limite);

    /** Anonimização: troca o nome e apaga o identificador digitado nos registros do usuário. */
    @Modifying
    @Query("update LogAuditoria l set l.usuarioNome = :nome, l.identificadorAcesso = null where l.usuarioId = :usuarioId")
    int anonimizarRegistrosDoUsuario(@Param("usuarioId") Long usuarioId, @Param("nome") String nome);

    /** Anonimização: remove detalhes que podem conter dado pessoal (ex.: e-mail no ENVIO_EMAIL). */
    @Modifying
    @Query("update LogAuditoria l set l.detalhes = :detalhes where l.usuarioId = :usuarioId and l.acao = :acao")
    int substituirDetalhes(@Param("usuarioId") Long usuarioId, @Param("acao") AcaoAuditoria acao,
                           @Param("detalhes") String detalhes);

    /** Anonimização: limpa detalhes de registros feitos por outros usuários sobre esta pessoa (ex.: correção do nome). */
    @Modifying
    @Query("update LogAuditoria l set l.detalhes = :detalhes " +
            "where l.entidade = :entidade and l.entidadeId = :entidadeId and l.acao = :acao")
    int substituirDetalhesSobreEntidade(@Param("entidade") String entidade, @Param("entidadeId") Long entidadeId,
                                        @Param("acao") AcaoAuditoria acao, @Param("detalhes") String detalhes);
}
