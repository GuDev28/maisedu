package br.com.maisedu.app.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Tradução das exceções em {"mensagem": "..."} com o status HTTP certo e sem detalhes técnicos (RNF13). */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** Só para montar um MethodParameter de verdade nas exceções de validação e de tipo. */
    @SuppressWarnings("unused")
    private void endpointDeExemplo(Long id) {
    }

    private static MethodParameter parametroDeExemplo() throws NoSuchMethodException {
        Method metodo = GlobalExceptionHandlerTest.class.getDeclaredMethod("endpointDeExemplo", Long.class);
        return new MethodParameter(metodo, 0);
    }

    private static void assertResposta(int status, String mensagem, ResponseEntity<ErroResponse> resposta) {
        assertEquals(status, resposta.getStatusCode().value());
        assertNotNull(resposta.getBody());
        assertEquals(mensagem, resposta.getBody().mensagem());
    }

    @Test
    void negocio_vira400ComAMensagemDaRegra() {
        assertResposta(400, "Só um rascunho pode ser editado.",
                handler.handleNegocioException(new NegocioException("Só um rascunho pode ser editado.")));
    }

    @Test
    void recursoNaoEncontrado_vira404() {
        assertResposta(404, "Turma não encontrada.",
                handler.handleNaoEncontrado(new RecursoNaoEncontradoException("Turma não encontrada.")));
    }

    @Test
    void autenticacao_vira401() {
        assertResposta(401, "E-mail ou senha inválidos.",
                handler.handleAutenticacaoException(new AutenticacaoException("E-mail ou senha inválidos.")));
    }

    @Test
    void acessoNegado_vira403ComMensagemPadrao() {
        assertResposta(403, "Sem permissão para esta operação.",
                handler.handleAccessDeniedException(new AccessDeniedException("detalhe interno")));
    }

    @Test
    void validacao_usaAMensagemDoPrimeiroCampoInvalido() throws Exception {
        BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "cadastro");
        resultado.addError(new FieldError("cadastro", "email", "Informe um e-mail válido."));
        resultado.addError(new FieldError("cadastro", "nome", "Informe o nome."));

        assertResposta(400, "Informe um e-mail válido.",
                handler.handleValidacao(new MethodArgumentNotValidException(parametroDeExemplo(), resultado)));
    }

    @Test
    void validacao_semErroDeCampo_usaMensagemGenerica() throws Exception {
        BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "cadastro");
        resultado.addError(new ObjectError("cadastro", "Objeto inválido."));

        assertResposta(400, "Dados inválidos.",
                handler.handleValidacao(new MethodArgumentNotValidException(parametroDeExemplo(), resultado)));
    }

    @Test
    void violacaoDeIntegridade_vira409SemExporOBanco() {
        ResponseEntity<ErroResponse> resposta = handler.handleIntegridade(new DataIntegrityViolationException(
                "duplicate key", new IllegalStateException("uk_usuario_email")));

        assertEquals(409, resposta.getStatusCode().value());
        assertNotNull(resposta.getBody());
        assertFalse(resposta.getBody().mensagem().contains("uk_usuario_email"));
    }

    @Test
    void bloqueioPessimista_vira409() {
        assertResposta(409, "Outra operação está em andamento sobre estes dados. Tente novamente.",
                handler.handleBloqueio(new PessimisticLockingFailureException("lock timeout")));
    }

    @Test
    void jsonMalFormado_vira400() {
        HttpMessageNotReadableException excecao = new HttpMessageNotReadableException("JSON inválido",
                new MockHttpInputMessage(new byte[0]));

        assertResposta(400, "Requisição inválida: verifique os dados enviados.",
                handler.handleRequisicaoMalFormada(excecao));
    }

    @Test
    void parametroDeTipoErrado_vira400() throws Exception {
        MethodArgumentTypeMismatchException excecao = new MethodArgumentTypeMismatchException("abc", Long.class,
                "id", parametroDeExemplo(), new NumberFormatException("abc"));

        assertResposta(400, "Requisição inválida: verifique os dados enviados.",
                handler.handleRequisicaoMalFormada(excecao));
    }

    @Test
    void erroDoSpringMvc_mantemOStatus404() {
        assertResposta(404, "Recurso não encontrado.",
                handler.handleGenerico(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @Test
    void erroDoSpringMvc_mantemOutrosStatus() {
        assertResposta(405, "Requisição não suportada.",
                handler.handleGenerico(new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED)));
    }

    @Test
    void erroInesperado_vira500ComMensagemGenerica() {
        ResponseEntity<ErroResponse> resposta = handler.handleGenerico(new IllegalStateException("NullPointer em X"));

        assertEquals(500, resposta.getStatusCode().value());
        assertNotNull(resposta.getBody());
        assertFalse(resposta.getBody().mensagem().contains("NullPointer"));
    }
}
