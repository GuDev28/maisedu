package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.LoginRequest;
import br.com.maisedu.app.dto.TrocarSenhaRequest;
import br.com.maisedu.app.dto.UsuarioResumoResponse;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.AutenticacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AutenticacaoService autenticacaoService;

    @Value("${maisedu.jwt.cookie-nome}")
    private String nomeCookie;

    @Value("${maisedu.jwt.cookie-secure}")
    private boolean cookieSecure;

    @Value("${maisedu.jwt.expiracao-minutos}")
    private long expiracaoMinutos;

    @PostMapping("/login")
    public ResponseEntity<UsuarioResumoResponse> login(@Valid @RequestBody LoginRequest request) {
        var sessao = autenticacaoService.login(request.identificador(), request.senha());

        ResponseCookie cookie = construirCookie(sessao.token(), Duration.ofMinutes(expiracaoMinutos));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(UsuarioResumoResponse.from(sessao.usuario()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = construirCookie("", Duration.ZERO);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/me")
    public UsuarioResumoResponse me(@AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado) {
        return UsuarioResumoResponse.from(usuarioAutenticado);
    }

    @PostMapping("/trocar-senha")
    public ResponseEntity<Void> trocarSenha(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @Valid @RequestBody TrocarSenhaRequest request) {
        autenticacaoService.trocarSenha(usuarioAutenticado.id(), request.senhaAtual(), request.novaSenha());
        ResponseCookie cookie = construirCookie("", Duration.ZERO);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private ResponseCookie construirCookie(String token, Duration validade) {
        return ResponseCookie.from(nomeCookie, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(validade)
                .build();
    }
}