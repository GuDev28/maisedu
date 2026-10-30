package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.LoginRequest;
import br.com.maisedu.app.dto.TrocarSenhaRequest;
import br.com.maisedu.app.dto.UsuarioResumoResponse;
import br.com.maisedu.app.exception.ErroResponse;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.LogAuditoriaRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integracao do AuthController (login por cookie HttpOnly, sessao, troca de senha e logout).
 *
 * Fluxo real exercitado em cada teste:
 * RestClient -> HTTP real -> JwtAuthenticationFilter/Spring Security -> AuthController
 * -> AutenticacaoService/AuditoriaService -> UsuarioRepository/LogAuditoriaRepository -> H2 em memoria
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerIntegrationTest extends IntegracaoTestBase {

    private static final String MENSAGEM_LOGIN_INVALIDO = "Identificador ou senha inválidos.";

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    private Conta admin;

    @BeforeEach
    void criarAdminDaInstituicao() {
        admin = criarAdmin();
    }

    // ---------- POST /auth/login ----------

    @Test
    void login_comCredenciaisValidas_deveRetornar200ComCookieHttpOnly() {
        Conta professor = cadastrarProfessor(admin);

        ResponseEntity<UsuarioResumoResponse> response = postLogin(professor.identificador(), SENHA_DEFINITIVA)
                .toEntity(UsuarioResumoResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UsuarioResumoResponse body = response.getBody();
        assertThat(body.id()).isEqualTo(professor.id());
        assertThat(body.nome()).isEqualTo(professor.nome());
        assertThat(body.roles()).containsExactly(RoleNome.PROFESSOR);
        assertThat(body.senhaTemporaria()).isFalse();

        // O JWT vai num cookie que o JavaScript do front-end nao consegue ler
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .startsWith(nomeCookie + "=")
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .contains("Path=/")
                .contains("Max-Age=7200");
    }

    @Test
    void login_comSenhaIncorreta_deveRetornar401ERegistrarTentativaFalha() {
        Conta professor = cadastrarProfessor(admin);

        ResponseEntity<ErroResponse> response = postLogin(professor.identificador(), "senha-errada-123")
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().mensagem()).isEqualTo(MENSAGEM_LOGIN_INVALIDO);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).isNull();

        // Checa persistencia: a tentativa falha ficou contada no banco
        assertThat(usuarioRepository.findById(professor.id()))
                .get()
                .extracting(Usuario::getTentativasFalhas)
                .isEqualTo(1);
    }

    @Test
    void login_aposCincoSenhasIncorretas_deveRetornar401BloqueadoMesmoComSenhaCorreta() {
        Conta professor = cadastrarProfessor(admin);
        for (int tentativa = 1; tentativa <= 5; tentativa++) {
            ResponseEntity<ErroResponse> falha = postLogin(professor.identificador(), "senha-errada-" + tentativa)
                    .toEntity(ErroResponse.class);
            assertThat(falha.getStatusCode()).as("tentativa %d", tentativa).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<ErroResponse> response = postLogin(professor.identificador(), SENHA_DEFINITIVA)
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().mensagem()).startsWith("Login bloqueado por excesso de tentativas.");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).isNull();
        Usuario bloqueado = usuarioRepository.findById(professor.id()).orElseThrow();
        assertThat(bloqueado.getBloqueadoAte()).isAfter(LocalDateTime.now());
    }

    @Test
    void login_comIdentificadorInexistente_deveRetornar401ComMensagemGenerica() {
        ResponseEntity<ErroResponse> response = postLogin("ninguem." + System.nanoTime() + "@maisedu.test", "qualquer")
                .toEntity(ErroResponse.class);

        // Mesma mensagem da senha errada: a API nao revela quais e-mails existem
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().mensagem()).isEqualTo(MENSAGEM_LOGIN_INVALIDO);
    }

    @Test
    void login_semSenha_deveRetornar400ComMensagem() {
        ResponseEntity<ErroResponse> response = postLogin(admin.identificador(), "")
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().mensagem()).isEqualTo("Informe a senha");
    }

    // ---------- GET /auth/me ----------

    @Test
    void me_comCookieValido_deveRetornar200ComDadosDoUsuario() {
        Conta professor = cadastrarProfessor(admin);

        ResponseEntity<UsuarioResumoResponse> response = client.get().uri("/auth/me")
                .header(HttpHeaders.COOKIE, cookie(professor))
                .retrieve()
                .toEntity(UsuarioResumoResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo(professor.id());
        assertThat(response.getBody().nome()).isEqualTo(professor.nome());
        assertThat(response.getBody().roles()).containsExactly(RoleNome.PROFESSOR);
        assertThat(response.getBody().senhaTemporaria()).isFalse();
    }

    @Test
    void me_semAutenticacao_deveRetornar401ComMensagem() {
        ResponseEntity<ErroResponse> response = client.get().uri("/auth/me")
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().mensagem()).isEqualTo("É preciso estar autenticado.");
    }

    @Test
    void me_comTokenAssinadoComOutraChave_deveRetornar401ComMensagem() {
        // Token forjado: mesmos claims que o sistema gera, com papel ADMIN, mas assinado com outra chave
        String tokenForjado = Jwts.builder()
                .subject(String.valueOf(admin.id()))
                .claim("nome", "Invasor")
                .claim("instituicaoId", 1L)
                .claim("roles", List.of("ADMIN", "PROFESSOR"))
                .claim("senhaTemporaria", false)
                .signWith(Keys.hmacShaKeyFor(
                        "chave-que-nao-e-a-do-servidor-0123456789".getBytes(StandardCharsets.UTF_8)))
                .compact();

        ResponseEntity<ErroResponse> response = client.get().uri("/auth/me")
                .header(HttpHeaders.COOKIE, cookie(tokenForjado))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().mensagem()).isEqualTo("É preciso estar autenticado.");
    }

    // ---------- POST /auth/trocar-senha ----------

    @Test
    void trocarSenha_comDadosValidos_deveRetornar204ELiberarAcesso() {
        NovoCadastro professorNovo = cadastrarProfessorSemAtivar(admin);
        String tokenProvisorio = login(professorNovo.identificador(), professorNovo.senhaProvisoria());

        ResponseEntity<Void> response = client.post().uri("/auth/trocar-senha")
                .header(HttpHeaders.COOKIE, cookie(tokenProvisorio))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TrocarSenhaRequest(professorNovo.senhaProvisoria(), SENHA_DEFINITIVA, true))
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // O cookie antigo (que carregava senhaTemporaria=true) e apagado
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .startsWith(nomeCookie + "=;")
                .contains("Max-Age=0");

        // Checa persistencia: senha definitiva e aceite dos termos (LGPD) gravados
        Usuario gravado = usuarioRepository.findById(professorNovo.id()).orElseThrow();
        assertThat(gravado.isSenhaTemporaria()).isFalse();
        assertThat(gravado.getTermosAceitosEm()).isNotNull();
        assertThat(passwordEncoder.matches(SENHA_DEFINITIVA, gravado.getSenha())).isTrue();

        // A senha nova ja funciona no login
        ResponseEntity<UsuarioResumoResponse> novoLogin = postLogin(professorNovo.identificador(), SENHA_DEFINITIVA)
                .toEntity(UsuarioResumoResponse.class);
        assertThat(novoLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(novoLogin.getBody().senhaTemporaria()).isFalse();
    }

    @Test
    void trocarSenha_comSenhaAtualIncorreta_deveRetornar401EManterSenhaProvisoria() {
        NovoCadastro professorNovo = cadastrarProfessorSemAtivar(admin);
        String tokenProvisorio = login(professorNovo.identificador(), professorNovo.senhaProvisoria());

        ResponseEntity<ErroResponse> response = client.post().uri("/auth/trocar-senha")
                .header(HttpHeaders.COOKIE, cookie(tokenProvisorio))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TrocarSenhaRequest("nao-e-a-senha-atual", SENHA_DEFINITIVA, true))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().mensagem()).isEqualTo("Senha atual incorreta.");
        assertThat(usuarioRepository.findById(professorNovo.id()))
                .get()
                .extracting(Usuario::isSenhaTemporaria)
                .isEqualTo(true);
    }

    @Test
    void trocarSenha_semAceitarTermos_deveRetornar400ComMensagem() {
        NovoCadastro professorNovo = cadastrarProfessorSemAtivar(admin);
        String tokenProvisorio = login(professorNovo.identificador(), professorNovo.senhaProvisoria());

        ResponseEntity<ErroResponse> response = client.post().uri("/auth/trocar-senha")
                .header(HttpHeaders.COOKIE, cookie(tokenProvisorio))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TrocarSenhaRequest(professorNovo.senhaProvisoria(), SENHA_DEFINITIVA, false))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().mensagem())
                .isEqualTo("É preciso aceitar os Termos de Uso e a Política de Privacidade para continuar");
        assertThat(usuarioRepository.findById(professorNovo.id()))
                .get()
                .extracting(Usuario::getTermosAceitosEm)
                .isNull();
    }

    // ---------- POST /auth/logout ----------

    @Test
    void logout_comCookieValido_deveRetornar204ELimparCookie() {
        Conta professor = cadastrarProfessor(admin);

        ResponseEntity<Void> response = client.post().uri("/auth/logout")
                .header(HttpHeaders.COOKIE, cookie(professor))
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .startsWith(nomeCookie + "=;")
                .contains("Max-Age=0");

        // Checa persistencia: o logout ficou registrado na auditoria
        assertThat(logAuditoriaRepository.findAll())
                .filteredOn(log -> professor.id().equals(log.getUsuarioId())
                        && log.getAcao() == AcaoAuditoria.LOGOUT
                        && log.isSucesso())
                .hasSize(1);
    }

    @Test
    void logout_semAutenticacao_deveRetornar401ComMensagem() {
        ResponseEntity<ErroResponse> response = client.post().uri("/auth/logout")
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().mensagem()).isEqualTo("É preciso estar autenticado.");
    }

    // ---------- Helpers ----------

    private RestClient.ResponseSpec postLogin(String identificador, String senha) {
        return client.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(identificador, senha))
                .retrieve();
    }
}
