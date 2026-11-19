package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.CorrecaoNomeRequest;
import br.com.maisedu.app.dto.DadosPessoaisResponse;
import br.com.maisedu.app.dto.EmailResponsavelRequest;
import br.com.maisedu.app.dto.PaginaResponse;
import br.com.maisedu.app.dto.SenhaRedefinidaResponse;
import br.com.maisedu.app.dto.UsuarioGestaoResponse;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.GestaoUsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    private final UsuarioRepository usuarioRepository;

    /** Lista paginada (pagina começa em 0; no máximo 100 por página), com papel e busca opcionais. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public PaginaResponse<UsuarioGestaoResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado ator,
            @RequestParam(required = false) RoleNome papel,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return PaginaResponse.de(gestaoUsuarioService.listar(ator, papel, busca, pagina, tamanho),
                UsuarioGestaoResponse::from);
    }

    @PostMapping("/{usuarioId}/redefinir-senha")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public SenhaRedefinidaResponse redefinirSenha(
            @AuthenticationPrincipal UsuarioAutenticado ator, @PathVariable Long usuarioId) {
        SenhaRedefinidaResponse resposta = gestaoUsuarioService.redefinirSenha(ator, usuarioId);
        if (!resposta.enviadaPorEmail()) {
            return resposta;
        }
        // o e-mail saiu depois do commit, ainda dentro da chamada acima: a situação gravada já é a deste envio
        return resposta.comStatusDoEmail(usuarioRepository.findStatusEmailAcesso(usuarioId).orElse(null));
    }

    /** Correção do e-mail do responsável de um aluno: admin ou professor de uma turma do aluno. */
    @PutMapping("/{usuarioId}/email-responsavel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public void corrigirEmailResponsavel(
            @AuthenticationPrincipal UsuarioAutenticado ator, @PathVariable Long usuarioId,
            @Valid @RequestBody EmailResponsavelRequest request) {
        gestaoUsuarioService.corrigirEmailResponsavel(ator, usuarioId, request.emailResponsavel());
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
