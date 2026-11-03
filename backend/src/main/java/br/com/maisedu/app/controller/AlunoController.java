package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.AtividadeAlunoResumoResponse;
import br.com.maisedu.app.dto.DesempenhoAlunoResponse;
import br.com.maisedu.app.dto.FolhaAtividadeResponse;
import br.com.maisedu.app.dto.PraticaAdaptativaResponse;
import br.com.maisedu.app.dto.ResponderQuestaoRequest;
import br.com.maisedu.app.dto.RespostaResultadoResponse;
import br.com.maisedu.app.model.AtividadeAluno;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.AtividadeService;
import br.com.maisedu.app.service.DesempenhoService;
import br.com.maisedu.app.service.PraticaAdaptativaService;
import br.com.maisedu.app.service.RespostaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Área do aluno: atividades (RF16), prática adaptativa (RF18) e o próprio desempenho (RF19).
 * O aluno é sempre o da sessão: não há id de aluno na URL, então ninguém acessa dados de outro aluno.
 */
@RestController
@RequestMapping("/aluno")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ALUNO')")
public class AlunoController {

    private final AtividadeService atividadeService;
    private final RespostaService respostaService;
    private final PraticaAdaptativaService praticaAdaptativaService;
    private final DesempenhoService desempenhoService;

    @GetMapping("/atividades")
    public List<AtividadeAlunoResumoResponse> atividades(@AuthenticationPrincipal UsuarioAutenticado aluno) {
        return atividadeService.listarDoAluno(aluno.id());
    }

    /** Inicia (ou reabre) a atividade e devolve a folha com as questões fixadas. */
    @PostMapping("/atividades/{atividadeId}/iniciar")
    public FolhaAtividadeResponse iniciar(
            @AuthenticationPrincipal UsuarioAutenticado aluno, @PathVariable Long atividadeId) {
        AtividadeAluno atividadeAluno = atividadeService.iniciar(aluno.id(), atividadeId);
        return atividadeService.obterFolha(aluno.id(), atividadeAluno.getId());
    }

    @GetMapping("/folhas/{atividadeAlunoId}")
    public FolhaAtividadeResponse folha(
            @AuthenticationPrincipal UsuarioAutenticado aluno, @PathVariable Long atividadeAlunoId) {
        return atividadeService.obterFolha(aluno.id(), atividadeAlunoId);
    }

    @PostMapping("/folhas/{atividadeAlunoId}/questoes/{atividadeAlunoQuestaoId}/resposta")
    public RespostaResultadoResponse responder(
            @AuthenticationPrincipal UsuarioAutenticado aluno,
            @PathVariable Long atividadeAlunoId,
            @PathVariable Long atividadeAlunoQuestaoId,
            @Valid @RequestBody ResponderQuestaoRequest request) {
        return respostaService.responder(aluno.id(), atividadeAlunoId, atividadeAlunoQuestaoId,
                request.alternativaId());
    }

    @PostMapping("/praticas-adaptativas")
    public PraticaAdaptativaResponse gerarPratica(@AuthenticationPrincipal UsuarioAutenticado aluno) {
        return praticaAdaptativaService.gerarOuRetomar(aluno.id());
    }

    @GetMapping("/desempenho")
    public DesempenhoAlunoResponse desempenho(@AuthenticationPrincipal UsuarioAutenticado aluno) {
        return desempenhoService.consultarDoAluno(aluno.id());
    }
}
