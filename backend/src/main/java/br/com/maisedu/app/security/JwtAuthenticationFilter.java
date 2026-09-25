package br.com.maisedu.app.security;

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
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> ROTAS_PERMITIDAS_COM_SENHA_TEMPORARIA =
            Set.of("/auth/trocar-senha", "/auth/logout", "/auth/me", "/auth/login");

    private final JwtService jwtService;
    private final String nomeCookie;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            @Value("${maisedu.jwt.cookie-nome}") String nomeCookie) {
        this.jwtService = jwtService;
        this.nomeCookie = nomeCookie;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        Optional<UsuarioAutenticado> usuarioOpt = extrairToken(request).flatMap(jwtService::validarEExtrair);

        if (usuarioOpt.isPresent()) {
            UsuarioAutenticado usuario = usuarioOpt.get();
            List<GrantedAuthority> authorities = usuario.roles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                    .map(GrantedAuthority.class::cast)
                    .toList();

            var autenticacao = new UsernamePasswordAuthenticationToken(usuario, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(autenticacao);

            if (usuario.senhaTemporaria()
                    && !"OPTIONS".equalsIgnoreCase(request.getMethod())
                    && !ROTAS_PERMITIDAS_COM_SENHA_TEMPORARIA.contains(request.getServletPath())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"mensagem\":\"Troque a senha provisória antes de continuar.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
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
