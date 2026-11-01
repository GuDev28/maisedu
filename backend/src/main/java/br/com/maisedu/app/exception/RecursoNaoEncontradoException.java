package br.com.maisedu.app.exception;

/** Recurso inexistente (ou de outra instituição, quando revelar a existência não é desejado): HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
