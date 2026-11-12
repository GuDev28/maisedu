package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.CadastroTurmaRequest;
import br.com.maisedu.app.dto.TurmaGestaoResponse;
import br.com.maisedu.app.dto.TurmaResponse;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.TurmaService;
import br.com.maisedu.app.service.VinculoTurmaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/turmas")
@RequiredArgsConstructor
public class TurmaController {

    private final VinculoTurmaService vinculoTurmaService;
    private final TurmaService turmaService;

    /** Turmas ativas em que o ator pode atuar (admin: todas da instituição; professor: as que leciona). */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public List<TurmaResponse> listarParaVinculo(@AuthenticationPrincipal UsuarioAutenticado ator) {
        List<TurmaResponse> resposta = new ArrayList<>();
        for (Turma turma : vinculoTurmaService.listarTurmasParaVinculo(ator)) {
            resposta.add(TurmaResponse.from(turma));
        }
        return resposta;
    }

    /** RF06: cadastro de turma pelo administrador. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TurmaGestaoResponse cadastrar(
            @AuthenticationPrincipal UsuarioAutenticado admin,
            @Valid @RequestBody CadastroTurmaRequest request) {
        return TurmaGestaoResponse.from(turmaService.cadastrar(admin, request.nome(), request.anoEscolar()));
    }

    /** Todas as turmas da instituição, inclusive as inativas. */
    @GetMapping("/gestao")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TurmaGestaoResponse> listarParaGestao(@AuthenticationPrincipal UsuarioAutenticado admin) {
        List<TurmaGestaoResponse> resposta = new ArrayList<>();
        for (Turma turma : turmaService.listarDaInstituicao(admin)) {
            resposta.add(TurmaGestaoResponse.from(turma));
        }
        return resposta;
    }

    @PostMapping("/{turmaId}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    public TurmaGestaoResponse desativar(@AuthenticationPrincipal UsuarioAutenticado admin,
                                         @PathVariable Long turmaId) {
        return TurmaGestaoResponse.from(turmaService.alterarSituacao(admin, turmaId, false));
    }

    @PostMapping("/{turmaId}/reativar")
    @PreAuthorize("hasRole('ADMIN')")
    public TurmaGestaoResponse reativar(@AuthenticationPrincipal UsuarioAutenticado admin, @PathVariable Long turmaId) {
        return TurmaGestaoResponse.from(turmaService.alterarSituacao(admin, turmaId, true));
    }
}
