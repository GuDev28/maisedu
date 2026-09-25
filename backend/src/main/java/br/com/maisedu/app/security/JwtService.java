package br.com.maisedu.app.security;

import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class JwtService {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_INSTITUICAO_ID = "instituicaoId";
    private static final String CLAIM_NOME = "nome";
    private static final String CLAIM_SENHA_TEMPORARIA = "senhaTemporaria";

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtService(
            @Value("${maisedu.jwt.secret}") String segredo,
            @Value("${maisedu.jwt.expiracao-minutos}") long expiracaoMinutos) {
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "maisedu.jwt.secret precisa ter pelo menos 32 bytes. Defina JWT_SECRET no .env");
        }
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        List<String> roles = usuario.getRoles().stream()
                .map(r -> r.getNome().name())
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim(CLAIM_NOME, usuario.getNome())
                .claim(CLAIM_INSTITUICAO_ID, usuario.getInstituicao().getId())
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_SENHA_TEMPORARIA, usuario.isSenhaTemporaria())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES)))
                .signWith(chave)
                .compact();
    }

    public Optional<UsuarioAutenticado> validarEExtrair(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            @SuppressWarnings("unchecked")
            List<String> rolesClaim = claims.get(CLAIM_ROLES, List.class);
            Set<RoleNome> roles = EnumSet.noneOf(RoleNome.class);
            if (rolesClaim != null) {
                rolesClaim.forEach(r -> roles.add(RoleNome.valueOf(r)));
            }

            UsuarioAutenticado usuario = new UsuarioAutenticado(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_NOME, String.class),
                    claims.get(CLAIM_INSTITUICAO_ID, Long.class),
                    roles,
                    Boolean.TRUE.equals(claims.get(CLAIM_SENHA_TEMPORARIA, Boolean.class)));

            return Optional.of(usuario);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long expiracaoEmSegundos() {
        return expiracaoMinutos * 60;
    }
}
