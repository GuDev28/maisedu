package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.CriarQuestaoRequest;
import br.com.maisedu.app.dto.QuestaoResponse;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.QuestaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/questoes")
@RequiredArgsConstructor
public class QuestaoController {

    private final QuestaoService questaoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROFESSOR')")
    public QuestaoResponse criar(
            @AuthenticationPrincipal UsuarioAutenticado professor,
            @Valid @RequestBody CriarQuestaoRequest request) {
        Questao questao = questaoService.criar(
                professor.id(), request.enunciado(), request.topicoId(), request.nivelDificuldade(),
                request.alternativas(), request.indiceCorreta());
        return QuestaoResponse.from(questao);
    }

    @PostMapping("/{questaoId}/enviar-para-validacao")
    @PreAuthorize("hasRole('PROFESSOR')")
    public QuestaoResponse enviarParaValidacao(
            @AuthenticationPrincipal UsuarioAutenticado professor,
            @PathVariable Long questaoId) {
        Questao questao = questaoService.enviarParaValidacao(questaoId, professor.id());
        return QuestaoResponse.from(questao);
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('PROFESSOR')")
    public List<QuestaoResponse> minhas(@AuthenticationPrincipal UsuarioAutenticado professor) {
        List<QuestaoResponse> respostas = new ArrayList<>();
        for (Questao questao : questaoService.listarMinhas(professor.id())) {
            respostas.add(QuestaoResponse.from(questao));
        }
        return respostas;
    }

    @GetMapping("/pendentes")
    @PreAuthorize("hasRole('PROFESSOR')")
    public List<QuestaoResponse> pendentes(@AuthenticationPrincipal UsuarioAutenticado professor) {
        List<QuestaoResponse> respostas = new ArrayList<>();
        for (Questao questao : questaoService.listarPendentesParaAvaliar(professor.id())) {
            respostas.add(QuestaoResponse.from(questao));
        }
        return respostas;
    }
}
