package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.CriarQuestaoRequest;
import br.com.maisedu.app.dto.QuestaoResponse;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.NivelDificuldade;
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
                professor, request.enunciado(), request.topicoId(), request.nivelDificuldade(),
                request.anoEscolar(), request.alternativas(), request.indiceCorreta());
        return QuestaoResponse.from(questao);
    }

    /** Edição do rascunho pelo autor (mesmos campos do cadastro). */
    @PutMapping("/{questaoId}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public QuestaoResponse editar(
            @AuthenticationPrincipal UsuarioAutenticado professor,
            @PathVariable Long questaoId,
            @Valid @RequestBody CriarQuestaoRequest request) {
        Questao questao = questaoService.editar(
                professor, questaoId, request.enunciado(), request.topicoId(), request.nivelDificuldade(),
                request.anoEscolar(), request.alternativas(), request.indiceCorreta());
        return QuestaoResponse.from(questao);
    }

    @DeleteMapping("/{questaoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PROFESSOR')")
    public void excluir(@AuthenticationPrincipal UsuarioAutenticado professor, @PathVariable Long questaoId) {
        questaoService.excluir(professor, questaoId);
    }

    @PostMapping("/{questaoId}/enviar-para-validacao")
    @PreAuthorize("hasRole('PROFESSOR')")
    public QuestaoResponse enviarParaValidacao(
            @AuthenticationPrincipal UsuarioAutenticado professor,
            @PathVariable Long questaoId) {
        Questao questao = questaoService.enviarParaValidacao(professor, questaoId);
        return QuestaoResponse.from(questao);
    }

    /** Questões do professor com o placar da validação e os motivos das rejeições. */
    @GetMapping("/minhas")
    @PreAuthorize("hasRole('PROFESSOR')")
    public List<QuestaoResponse> minhas(@AuthenticationPrincipal UsuarioAutenticado professor) {
        return questaoService.listarMinhas(professor);
    }

    /** Banco compartilhado (RF15): questões aprovadas de qualquer instituição, com filtros opcionais. */
    @GetMapping("/aprovadas")
    @PreAuthorize("hasRole('PROFESSOR')")
    public List<QuestaoResponse> aprovadas(
            @RequestParam(required = false) Long topicoId,
            @RequestParam(required = false) NivelDificuldade nivel,
            @RequestParam(required = false) AnoEscolar anoEscolar) {
        List<QuestaoResponse> respostas = new ArrayList<>();
        for (Questao questao : questaoService.buscarAprovadas(topicoId, nivel, anoEscolar)) {
            respostas.add(QuestaoResponse.from(questao));
        }
        return respostas;
    }

    @GetMapping("/pendentes")
    @PreAuthorize("hasRole('PROFESSOR')")
    public List<QuestaoResponse> pendentes(@AuthenticationPrincipal UsuarioAutenticado professor) {
        return questaoService.listarPendentesComPlacar(professor);
    }
}
