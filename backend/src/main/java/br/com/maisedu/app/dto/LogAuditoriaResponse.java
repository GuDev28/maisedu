package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.LogAuditoria;

import java.time.LocalDateTime;

public record LogAuditoriaResponse(
        Long id,
        Long usuarioId,
        String usuarioNome,
        String identificadorAcesso,
        AcaoAuditoria acao,
        String entidade,
        Long entidadeId,
        boolean sucesso,
        String detalhes,
        String ip,
        String userAgent,
        LocalDateTime criadoEm) {

    public static LogAuditoriaResponse from(LogAuditoria log) {
        return new LogAuditoriaResponse(
                log.getId(),
                log.getUsuarioId(),
                log.getUsuarioNome(),
                log.getIdentificadorAcesso(),
                log.getAcao(),
                log.getEntidade(),
                log.getEntidadeId(),
                log.isSucesso(),
                log.getDetalhes(),
                log.getIp(),
                log.getUserAgent(),
                log.getCriadoEm());
    }
}
