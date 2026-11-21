package br.com.maisedu.app.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** JSON de erro escrito direto na resposta (filtro JWT e respostas 401/403 do Spring Security). */
class RespostaErroJsonTest {

    @Test
    void escrever_defineStatusTipoECorpoNoFormatoDoErroResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        RespostaErroJson.escrever(response, 401, "Faça login para continuar.");

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("UTF-8", response.getCharacterEncoding().toUpperCase());
        assertEquals("{\"mensagem\":\"Faça login para continuar.\"}",
                response.getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void escrever_escapaAspasBarrasEQuebrasDeLinha() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        RespostaErroJson.escrever(response, 403, "diz \"oi\" \\ a\nb\rc\td");

        assertEquals("{\"mensagem\":\"diz \\\"oi\\\" \\\\ a\\nb\\rc\\td\"}",
                response.getContentAsString(StandardCharsets.UTF_8));
    }
}
