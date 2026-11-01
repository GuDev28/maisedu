package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.LogAuditoriaResponse;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    /** RF13: só registros da instituição do admin, do mais recente para o mais antigo, 20 por página. */
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<LogAuditoriaResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado admin,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) AcaoAuditoria acao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ate,
            @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return auditoriaService.consultar(admin.instituicaoId(), usuarioId, acao, de, ate, pageable)
                .map(LogAuditoriaResponse::from);
    }
}
