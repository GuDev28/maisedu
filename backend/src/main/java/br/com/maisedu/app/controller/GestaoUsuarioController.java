package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.CorrecaoNomeRequest;
import br.com.maisedu.app.dto.DadosPessoaisResponse;
import br.com.maisedu.app.dto.SenhaRedefinidaResponse;
import br.com.maisedu.app.dto.UsuarioGestaoResponse;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.GestaoUsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestão de usuários: redefinição de senha (admin e professor) e controles de direitos do titular
 * da LGPD (só admin): correção, desativação/reativação, exportação e anonimização.
 */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class GestaoUsuarioController {

    private final GestaoUsuarioService gestaoUsuarioService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public List<UsuarioGestaoResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado ator,
            @RequestParam(required = false) RoleNome papel) {
        List<UsuarioGestaoResponse> resposta = new ArrayList<>();
        for (Usuario usuario : gestaoUsuarioService.listar(ator, papel)) {
            resposta.add(UsuarioGestaoResponse.from(usuario));
        }
        return resposta;
    }

    @PostMapping("/{usuarioId}/redefinir-senha")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public SenhaRedefinidaResponse redefinirSenha(
            @AuthenticationPrincipal UsuarioAutenticado ator, @PathVariable Long usuarioId) {
        return gestaoUsuarioService.redefinirSenha(ator, usuarioId);
    }

    @PatchMapping("/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioGestaoResponse corrigirNome(
            @AuthenticationPrincipal UsuarioAutenticado admin, @PathVariable Long usuarioId,
            @Valid @RequestBody CorrecaoNomeRequest request) {
        return UsuarioGestaoResponse.from(gestaoUsuarioService.corrigirNome(admin, usuarioId, request.nome()));
    }

    @PostMapping("/{usuarioId}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioGestaoResponse desativar(
            @AuthenticationPrincipal UsuarioAutenticado admin, @PathVariable Long usuarioId) {
        return UsuarioGestaoResponse.from(gestaoUsuarioService.desativar(admin, usuarioId));
    }

    @PostMapping("/{usuarioId}/reativar")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioGestaoResponse reativar(
            @AuthenticationPrincipal UsuarioAutenticado admin, @PathVariable Long usuarioId) {
        return UsuarioGestaoResponse.from(gestaoUsuarioService.reativar(admin, usuarioId));
    }

    /** Exportação em JSON (acesso e portabilidade). O navegador baixa como arquivo. */
    @GetMapping("/{usuarioId}/dados-pessoais")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DadosPessoaisResponse> exportarDados(
            @AuthenticationPrincipal UsuarioAutenticado admin, @PathVariable Long usuarioId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"maisedu-dados-usuario-" + usuarioId + ".json\"")
                .body(gestaoUsuarioService.exportarDados(admin, usuarioId));
    }

    @PostMapping("/{usuarioId}/anonimizar")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioGestaoResponse anonimizar(
            @AuthenticationPrincipal UsuarioAutenticado admin, @PathVariable Long usuarioId) {
        return UsuarioGestaoResponse.from(gestaoUsuarioService.anonimizar(admin, usuarioId));
    }
}
