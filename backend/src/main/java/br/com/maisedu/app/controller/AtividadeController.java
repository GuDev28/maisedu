package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.AlterarPrazoRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDateTime;

/** RF15: atividades regulares (com prazo de entrega) do professor para as turmas em que leciona. */
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
        Atividade atividade = atividadeService.criar(professor, request.turmaId(), request.titulo(), request.prazo(),
                request.questaoIds());
        return atividadeService.resumir(atividade, atividade.getTurma(),
                atividadeService.contarAlunosDaTurma(atividade.getTurma().getId()), 0, 0, LocalDateTime.now());
    }

    /** Estende ou antecipa o prazo (só o professor da turma e só antes do prazo atual terminar). */
    @PutMapping("/atividades/{atividadeId}/prazo")
    @PreAuthorize("hasRole('PROFESSOR')")
    public AtividadeResumoResponse alterarPrazo(
            @AuthenticationPrincipal UsuarioAutenticado professor, @PathVariable Long atividadeId,
            @Valid @RequestBody AlterarPrazoRequest request) {
        atividadeService.alterarPrazo(professor, atividadeId, request.prazo());
        Atividade atividade = atividadeService.buscarParaResumo(atividadeId);
        for (AtividadeResumoResponse resumo : atividadeService.listarDaTurma(professor, atividade.getTurma().getId())) {
            if (resumo.id().equals(atividadeId)) {
                return resumo;
            }
        }
        throw new IllegalStateException("Atividade não encontrada depois de alterar o prazo");
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
