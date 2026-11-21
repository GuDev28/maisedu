package br.com.maisedu.app.security;

import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.repository.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Filtro JWT sem subir o Spring: cookie, validação do token, conferência no banco (conta ativa e versão
 * da sessão) e os bloqueios de senha provisória e de termos pendentes.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String NOME_COOKIE = "maisedu_sessao";
    private static final String TOKEN = "token-valido";
    private static final int VERSAO_TERMOS = 2;
    private static final Long USUARIO_ID = 7L;

    @Mock JwtService jwtService;
    @Mock UsuarioRepository usuarioRepository;

    private JwtAuthenticationFilter filtro;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filtro = new JwtAuthenticationFilter(jwtService, usuarioRepository, NOME_COOKIE, VERSAO_TERMOS);
        request = new MockHttpServletRequest("GET", "/turmas");
        request.setServletPath("/turmas");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private UsuarioAutenticado doToken(int versaoSessao) {
        return new UsuarioAutenticado(USUARIO_ID, "Paulo Professor", 1L, EnumSet.of(RoleNome.PROFESSOR),
                false, false, versaoSessao);
    }

    private void comTokenValido(UsuarioAutenticado usuario, Optional<EstadoSessao> estado) {
        request.setCookies(new Cookie(NOME_COOKIE, TOKEN));
        when(jwtService.validarEExtrair(TOKEN)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.buscarEstadoSessao(USUARIO_ID)).thenReturn(estado);
    }

    private void assertSeguiuSemAutenticar() {
        assertNotNull(chain.getRequest(), "a requisição deveria seguir para o próximo filtro");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void semCookie_segueSemAutenticar() throws Exception {
        filtro.doFilterInternal(request, response, chain);

        assertSeguiuSemAutenticar();
        verifyNoInteractions(jwtService, usuarioRepository);
    }

    @Test
    void cookieDeOutroNome_eIgnorado() throws Exception {
        request.setCookies(new Cookie("outro_cookie", TOKEN));

        filtro.doFilterInternal(request, response, chain);

        assertSeguiuSemAutenticar();
        verifyNoInteractions(jwtService, usuarioRepository);
    }

    @Test
    void tokenInvalido_segueSemAutenticarESemConsultarOBanco() throws Exception {
        request.setCookies(new Cookie(NOME_COOKIE, "token-adulterado"));
        when(jwtService.validarEExtrair("token-adulterado")).thenReturn(Optional.empty());

        filtro.doFilterInternal(request, response, chain);

        assertSeguiuSemAutenticar();
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void tokenValidoComContaAtiva_autenticaComOsPapeisDoToken() throws Exception {
        comTokenValido(doToken(3), Optional.of(new EstadoSessao(true, 3, false, VERSAO_TERMOS)));

        filtro.doFilterInternal(request, response, chain);

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(autenticacao);
        UsuarioAutenticado principal = (UsuarioAutenticado) autenticacao.getPrincipal();
        assertEquals(USUARIO_ID, principal.id());
        assertFalse(principal.senhaTemporaria());
        assertFalse(principal.termosPendentes());
        assertTrue(autenticacao.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PROFESSOR")));
        assertNotNull(chain.getRequest());
    }

    @Test
    void contaDesativada_naoAutentica() throws Exception {
        comTokenValido(doToken(0), Optional.of(new EstadoSessao(false, 0, false, VERSAO_TERMOS)));

        filtro.doFilterInternal(request, response, chain);

        assertSeguiuSemAutenticar();
    }

    @Test
    void versaoDaSessaoDiferente_derrubaOTokenAntigo() throws Exception {
        // o token foi emitido na versão 2, mas houve logout ou troca de senha depois (versão 3 no banco)
        comTokenValido(doToken(2), Optional.of(new EstadoSessao(true, 3, false, VERSAO_TERMOS)));

        filtro.doFilterInternal(request, response, chain);

        assertSeguiuSemAutenticar();
    }

    @Test
    void usuarioQueNaoExisteMais_naoAutentica() throws Exception {
        comTokenValido(doToken(0), Optional.empty());

        filtro.doFilterInternal(request, response, chain);

        assertSeguiuSemAutenticar();
    }

    @Test
    void senhaProvisoria_bloqueiaAsDemaisRotasCom403() throws Exception {
        comTokenValido(doToken(0), Optional.of(new EstadoSessao(true, 0, true, null)));

        filtro.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString(StandardCharsets.UTF_8).contains("Troque a senha provisória"));
        assertNull(chain.getRequest(), "a requisição não deveria chegar ao controller");
    }

    @Test
    void senhaProvisoria_permiteATrocaDeSenha() throws Exception {
        request.setServletPath("/auth/trocar-senha");
        comTokenValido(doToken(0), Optional.of(new EstadoSessao(true, 0, true, null)));

        filtro.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
        UsuarioAutenticado principal =
                (UsuarioAutenticado) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertTrue(principal.senhaTemporaria());
        // com senha provisória, os termos só são cobrados depois da troca
        assertFalse(principal.termosPendentes());
    }

    @Test
    void senhaProvisoria_naoBloqueiaOPreflightDoCors() throws Exception {
        request.setMethod("OPTIONS");
        comTokenValido(doToken(0), Optional.of(new EstadoSessao(true, 0, true, null)));

        filtro.doFilterInternal(request, response, chain);

        assertNotNull(chain.getRequest());
    }

    @Test
    void termosDesatualizados_bloqueiamAsDemaisRotasCom403() throws Exception {
        comTokenValido(doToken(0), Optional.of(new EstadoSessao(true, 0, false, VERSAO_TERMOS - 1)));

        filtro.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString(StandardCharsets.UTF_8).contains("Termos de Uso"));
        assertNull(chain.getRequest());
    }

    @Test
    void termosDesatualizados_permitemAceitarANovaVersao() throws Exception {
        request.setServletPath("/auth/aceitar-termos");
        comTokenValido(doToken(0), Optional.of(new EstadoSessao(true, 0, false, null)));

        filtro.doFilterInternal(request, response, chain);

        assertNotNull(chain.getRequest());
        UsuarioAutenticado principal =
                (UsuarioAutenticado) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertTrue(principal.termosPendentes());
    }
}
