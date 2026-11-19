package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.AtividadeAlunoResumoResponse;
import br.com.maisedu.app.dto.DesempenhoAlunoResponse;
import br.com.maisedu.app.dto.EnvioFolhaRequest;
import br.com.maisedu.app.dto.FolhaAtividadeResponse;
import br.com.maisedu.app.dto.PainelPraticaResponse;
import br.com.maisedu.app.dto.PraticaAdaptativaResponse;
import br.com.maisedu.app.dto.ResponderQuestaoRequest;
import br.com.maisedu.app.model.AtividadeAluno;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.AtividadeService;
import br.com.maisedu.app.service.DesempenhoService;
import br.com.maisedu.app.service.PraticaAdaptativaService;
import br.com.maisedu.app.service.RespostaService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    /**
     * Entrega única: envia as alternativas escolhidas em todas as questões. Recusado com questão sem
     * resposta, depois do prazo ou se a folha já foi enviada. Devolve a folha como fica depois do envio
     * (com a correção só se ela já estiver liberada: na prática adaptativa, na hora).
     */
    @PostMapping("/folhas/{atividadeAlunoId}/envio")
    public FolhaAtividadeResponse enviar(
            @AuthenticationPrincipal UsuarioAutenticado aluno,
            @PathVariable Long atividadeAlunoId,
            @Valid @RequestBody EnvioFolhaRequest request) {
        respostaService.enviar(aluno.id(), atividadeAlunoId, request.respostas());
        return atividadeService.obterFolha(aluno.id(), atividadeAlunoId);
    }

    /** Marca uma alternativa sem enviar: não é resposta, pode ser trocada até o envio (e dentro do prazo). */
    @PutMapping("/folhas/{atividadeAlunoId}/questoes/{atividadeAlunoQuestaoId}/marcacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcar(
            @AuthenticationPrincipal UsuarioAutenticado aluno,
            @PathVariable Long atividadeAlunoId,
            @PathVariable Long atividadeAlunoQuestaoId,
            @Valid @RequestBody ResponderQuestaoRequest request) {
        respostaService.marcar(aluno.id(), atividadeAlunoId, atividadeAlunoQuestaoId, request.alternativaId());
    }

    /** Tela "Praticar": a prática recomendada pelo motor e um card por tópico disponível ao aluno. */
    @GetMapping("/praticas")
    public PainelPraticaResponse painelDePratica(@AuthenticationPrincipal UsuarioAutenticado aluno) {
        return praticaAdaptativaService.painel(aluno.id());
    }

    /**
     * Gera (ou retoma, se houver uma em aberto) a prática recomendada pelo motor ou, com
     * {@code topicoId}, a prática de um tópico escolhido pelo aluno.
     */
    @PostMapping("/praticas-adaptativas")
    public PraticaAdaptativaResponse gerarPratica(@AuthenticationPrincipal UsuarioAutenticado aluno,
                                                  @RequestParam(required = false) Long topicoId) {
        return praticaAdaptativaService.gerarOuRetomar(aluno.id(), topicoId);
    }

    @GetMapping("/desempenho")
    public DesempenhoAlunoResponse desempenho(@AuthenticationPrincipal UsuarioAutenticado aluno) {
        return desempenhoService.consultarDoAluno(aluno.id());
    }
}
