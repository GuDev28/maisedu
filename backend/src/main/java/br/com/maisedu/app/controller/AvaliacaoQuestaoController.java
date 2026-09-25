package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.AvaliacaoQuestaoRequest;
import br.com.maisedu.app.dto.AvaliacaoQuestaoResponse;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.AvaliacaoQuestaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/questoes/{questaoId}/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoQuestaoController {

    private final AvaliacaoQuestaoService avaliacaoQuestaoService;

    @PostMapping
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<AvaliacaoQuestaoResponse> avaliar(
            @AuthenticationPrincipal UsuarioAutenticado professor,
            @PathVariable Long questaoId,
            @Valid @RequestBody AvaliacaoQuestaoRequest request) {
        return ResponseEntity.ok(
                avaliacaoQuestaoService.avaliar(questaoId, professor.id(), request.voto()));
    }
}
