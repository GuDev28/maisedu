package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.LogAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {


    @Query("""
            select l from LogAuditoria l
            where (:usuarioId is null or l.usuarioId = :usuarioId)
              and (:acao is null or l.acao = :acao)
              and (:de is null or l.criadoEm >= :de)
              and (:ate is null or l.criadoEm <= :ate)
            """)
    Page<LogAuditoria> buscar(@Param("usuarioId") Long usuarioId,
                               @Param("acao") AcaoAuditoria acao,
                               @Param("de") LocalDateTime de,
                               @Param("ate") LocalDateTime ate,
                               Pageable pageable);
}
