package br.com.maisedu.app.security;

import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Token JWT do login: o segredo precisa ter pelo menos 32 bytes (HS256), o token carrega os dados
 * usados na autorização por perfil e só é aceito se tiver sido assinado com a mesma chave e não estiver vencido.
 * O JwtService não tem dependências, então é testado com instâncias reais, sem mocks e sem subir o Spring.
 */
class JwtServiceTest {

    private static final String SEGREDO = "chave-de-teste-maisedu-0123456789abcdef";
    private static final String OUTRO_SEGREDO = "outra-chave-de-teste-maisedu-0123456789";
    private static final long EXPIRACAO_MINUTOS = 120;
    private static final String MENSAGEM_SEGREDO_CURTO =
            "maisedu.jwt.secret precisa ter pelo menos 32 bytes. Defina JWT_SECRET no .env";

    private JwtService jwtService;
    private Usuario professor;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SEGREDO, EXPIRACAO_MINUTOS);

        Instituicao escola = new Instituicao("Escola Teste", "00.000.000/0001-00", "Rua Teste, 1");
        ReflectionTestUtils.setField(escola, "id", 3L);
        professor = new Usuario("Paula Professora", "paula@escola.test", "hash", escola);
        ReflectionTestUtils.setField(professor, "id", 7L);
        professor.adicionarRole(new Role(RoleNome.PROFESSOR));
        professor.adicionarRole(new Role(RoleNome.ADMIN));
    }

    @Test
    void deveGerarTokenQueDevolveOsDadosDoUsuarioAoSerValidado() {
        // Arrange
        String token = jwtService.gerarToken(professor);

        // Act
        Optional<UsuarioAutenticado> resultado = jwtService.validarEExtrair(token);

        // Assert
        assertTrue(resultado.isPresent());
        UsuarioAutenticado autenticado = resultado.get();
        assertEquals(7L, autenticado.id());
        assertEquals("Paula Professora", autenticado.nome());
        assertEquals(3L, autenticado.instituicaoId());
        assertEquals(EnumSet.of(RoleNome.PROFESSOR, RoleNome.ADMIN), autenticado.roles());
        assertTrue(autenticado.senhaTemporaria());
    }

    @Test
    void deveRejeitarTokenAssinadoComOutroSegredo() {
        // Arrange
        JwtService outroEmissor = new JwtService(OUTRO_SEGREDO, EXPIRACAO_MINUTOS);
        String tokenDeOutraChave = outroEmissor.gerarToken(professor);

        // Act
        Optional<UsuarioAutenticado> resultado = jwtService.validarEExtrair(tokenDeOutraChave);

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveRejeitarTokenExpirado() {
        // Arrange: expiração negativa gera um token que já nasce vencido (sem precisar esperar o tempo passar)
        JwtService emissorDeTokenVencido = new JwtService(SEGREDO, -1);
        String tokenVencido = emissorDeTokenVencido.gerarToken(professor);

        // Act
        Optional<UsuarioAutenticado> resultado = jwtService.validarEExtrair(tokenVencido);

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @ParameterizedTest(name = "{index}: token = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "token-sem-formato-jwt"})
    void deveRetornarVazioQuandoTokenEstaAusenteOuMalformado(String token) {
        // Act
        Optional<UsuarioAutenticado> resultado = jwtService.validarEExtrair(token);

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @ParameterizedTest(name = "segredo com {0} bytes é recusado")
    @ValueSource(ints = {0, 31})
    void deveRecusarSegredoComMenosDe32Bytes(int tamanhoDoSegredo) {
        // Arrange
        String segredoCurto = "a".repeat(tamanhoDoSegredo);

        // Act
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService(segredoCurto, EXPIRACAO_MINUTOS));

        // Assert
        assertEquals(MENSAGEM_SEGREDO_CURTO, ex.getMessage());
    }

    @Test
    void deveAceitarSegredoComExatamente32Bytes() {
        // Arrange
        String segredoNoLimite = "a".repeat(32);

        // Act
        JwtService servicoNoLimite = new JwtService(segredoNoLimite, EXPIRACAO_MINUTOS);
        String token = servicoNoLimite.gerarToken(professor);

        // Assert
        assertTrue(servicoNoLimite.validarEExtrair(token).isPresent());
    }
}
