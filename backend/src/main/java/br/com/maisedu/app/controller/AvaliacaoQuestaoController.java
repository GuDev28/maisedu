package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.AvaliacaoQuestaoRequest;
import br.com.maisedu.app.service.AvaliacaoQuestaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/questoes/{questaoId}/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoQuestaoController {

    private final AvaliacaoQuestaoService avaliacaoQuestaoService;

    @PostMapping
    public ResponseEntity<Void> avaliar(@PathVariable Long questaoId,
                                         @RequestBody AvaliacaoQuestaoRequest request) {
        avaliacaoQuestaoService.avaliar(questaoId, request.professorId(), request.voto());
        return ResponseEntity.noContent().build();
    }
}
