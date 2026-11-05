package br.com.maisedu.app.security;

import br.com.maisedu.app.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Autentica a requisição a partir do cookie de sessão.
 *
 * Além de validar assinatura e expiração do JWT, confere no banco (consulta pela chave primária)
 * se a conta continua ativa e se a versão da sessão do token ainda é a atual. Assim, logout,
 * troca/redefinição de senha e desativação derrubam os tokens já emitidos na hora, sem esperar
 * os 120 minutos de validade.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> ROTAS_PERMITIDAS_COM_SENHA_TEMPORARIA =
            Set.of("/auth/trocar-senha", "/auth/logout", "/auth/me", "/auth/login");

    private static final Set<String> ROTAS_PERMITIDAS_COM_TERMOS_PENDENTES =
            Set.of("/auth/aceitar-termos", "/auth/logout", "/auth/me", "/auth/login", "/auth/trocar-senha");

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final String nomeCookie;
    private final int versaoTermosAtual;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            @Value("${maisedu.jwt.cookie-nome}") String nomeCookie,
            @Value("${maisedu.termos.versao-atual:1}") int versaoTermosAtual) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.nomeCookie = nomeCookie;
        this.versaoTermosAtual = versaoTermosAtual;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        Optional<UsuarioAutenticado> usuarioOpt = extrairToken(request)
                .flatMap(jwtService::validarEExtrair)
                .flatMap(this::conferirNoBanco);

        if (usuarioOpt.isPresent()) {
            UsuarioAutenticado usuario = usuarioOpt.get();
            List<GrantedAuthority> authorities = new ArrayList<>();
            for (var role : usuario.roles()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
            }

            var autenticacao = new UsernamePasswordAuthenticationToken(usuario, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(autenticacao);

            boolean preflight = "OPTIONS".equalsIgnoreCase(request.getMethod());
            String rota = request.getServletPath();

            if (!preflight && usuario.senhaTemporaria() && !ROTAS_PERMITIDAS_COM_SENHA_TEMPORARIA.contains(rota)) {
                recusar(response, "Troque a senha provisória antes de continuar.");
                return;
            }
            if (!preflight && usuario.termosPendentes() && !ROTAS_PERMITIDAS_COM_TERMOS_PENDENTES.contains(rota)) {
                recusar(response, "Os Termos de Uso e a Política de Privacidade foram atualizados. "
                        + "Aceite a nova versão para continuar.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /** Token válido só vale se a conta estiver ativa e a versão da sessão bater com a do banco. */
    private Optional<UsuarioAutenticado> conferirNoBanco(UsuarioAutenticado doToken) {
        Optional<EstadoSessao> estado = usuarioRepository.buscarEstadoSessao(doToken.id());
        if (estado.isEmpty() || !estado.get().contaAtiva() || estado.get().versao() != doToken.versaoSessao()) {
            return Optional.empty();
        }
        return Optional.of(doToken.comEstado(estado.get(), versaoTermosAtual));
    }

    private void recusar(HttpServletResponse response, String mensagem) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"mensagem\":\"" + mensagem.replace("\"", "\\\"") + "\"}");
    }

    private Optional<String> extrairToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (nomeCookie.equals(cookie.getName())) {
                return Optional.ofNullable(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
