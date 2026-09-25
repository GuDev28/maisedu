package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.LogAuditoriaResponse;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
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

        @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<LogAuditoriaResponse> listar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) AcaoAuditoria acao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ate,
            @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return auditoriaService.consultar(usuarioId, acao, de, ate, pageable)
                .map(LogAuditoriaResponse::from);
    }
}
