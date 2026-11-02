package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.AtividadeResumoResponse;
import br.com.maisedu.app.dto.CriarAtividadeRequest;
import br.com.maisedu.app.dto.ResultadoAtividadeResponse;
import br.com.maisedu.app.model.Atividade;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.AtividadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** RF15: atividades regulares do professor para as turmas em que leciona. */
@RestController
@RequiredArgsConstructor
public class AtividadeController {

    private final AtividadeService atividadeService;

    @PostMapping("/atividades")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROFESSOR')")
    public AtividadeResumoResponse criar(
            @AuthenticationPrincipal UsuarioAutenticado professor,
            @Valid @RequestBody CriarAtividadeRequest request) {
        Atividade atividade = atividadeService.criar(professor, request.turmaId(), request.titulo(),
                request.questaoIds());
        return new AtividadeResumoResponse(atividade.getId(), atividade.getTitulo(), atividade.getTurma().getId(),
                atividade.getTurma().getNome(), atividade.getProfessor().getNome(), atividade.getQuestoes().size(),
                0, 0, atividade.getCriadoEm());
    }

    @GetMapping("/turmas/{turmaId}/atividades")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public List<AtividadeResumoResponse> listarDaTurma(
            @AuthenticationPrincipal UsuarioAutenticado ator, @PathVariable Long turmaId) {
        return atividadeService.listarDaTurma(ator, turmaId);
    }

    @GetMapping("/atividades/{atividadeId}/resultados")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResultadoAtividadeResponse resultados(
            @AuthenticationPrincipal UsuarioAutenticado ator, @PathVariable Long atividadeId) {
        return atividadeService.resultados(ator, atividadeId);
    }
}
