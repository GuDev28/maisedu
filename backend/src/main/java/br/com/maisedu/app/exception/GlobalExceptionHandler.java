package br.com.maisedu.app.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduz exceções em respostas JSON no formato {"mensagem": "..."}, em português e sem detalhes
 * técnicos (RNF13).
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErroResponse> handleNegocioException(NegocioException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(ex.getMessage()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidacao(MethodArgumentNotValidException ex) {
        String mensagem = "Dados inválidos.";
        FieldError primeiroErro = ex.getBindingResult().getFieldError();
        if (primeiroErro != null && primeiroErro.getDefaultMessage() != null) {
            mensagem = primeiroErro.getDefaultMessage();
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(mensagem));
    }

    @ExceptionHandler(AutenticacaoException.class)
    public ResponseEntity<ErroResponse> handleAutenticacaoException(AutenticacaoException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErroResponse(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErroResponse("Sem permissão para esta operação."));
    }

    /** Corrida entre duas requisições (ex.: mesmo e-mail cadastrado duas vezes ao mesmo tempo): o banco barra a segunda. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> handleIntegridade(DataIntegrityViolationException ex) {
        log.info("Violação de integridade barrada pelo banco: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErroResponse("Não foi possível concluir: os dados conflitam com um registro existente "
                        + "(por exemplo, e-mail, login ou resposta já cadastrados). Atualize a tela e tente de novo."));
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErroResponse> handleBloqueio(PessimisticLockingFailureException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErroResponse("Outra operação está em andamento sobre estes dados. Tente novamente."));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroResponse> handleRequisicaoMalFormada(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse("Requisição inválida: verifique os dados enviados."));
    }

    /**
     * Demais exceções. As do próprio Spring MVC (rota inexistente, método não suportado etc.)
     * mantêm o status HTTP delas; qualquer outra vira 500 com mensagem genérica, e o detalhe vai só para o log.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGenerico(Exception ex) {
        if (ex instanceof ErrorResponse erroDoSpring) {
            HttpStatusCode status = erroDoSpring.getStatusCode();
            String mensagem = status.value() == 404 ? "Recurso não encontrado." : "Requisição não suportada.";
            return ResponseEntity.status(status).body(new ErroResponse(mensagem));
        }
        log.error("Erro inesperado", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroResponse("Erro inesperado ao processar a solicitação. Tente novamente em instantes."));
    }

}
