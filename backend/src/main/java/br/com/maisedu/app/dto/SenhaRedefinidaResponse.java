package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.StatusEmailAcesso;

/**
 * Resultado da redefinição de senha. A senha nova vai por e-mail: para o próprio professor/admin ou,
 * no caso do aluno, para o e-mail do responsável; então {@code senhaProvisoria} é nula. Ela só volta
 * aqui para aluno cadastrado antes do e-mail do responsável existir (sem e-mail para onde mandar).
 * {@code statusEmailAcesso} diz se o e-mail saiu (preenchido pelo controller depois do envio).
 */
public record SenhaRedefinidaResponse(
        Long usuarioId,
        String nome,
        String identificadorDeAcesso,
        String senhaProvisoria,
        boolean enviadaPorEmail,
        StatusEmailAcesso statusEmailAcesso) {

    public SenhaRedefinidaResponse(Long usuarioId, String nome, String identificadorDeAcesso, String senhaProvisoria,
                                   boolean enviadaPorEmail) {
        this(usuarioId, nome, identificadorDeAcesso, senhaProvisoria, enviadaPorEmail, null);
    }

    public SenhaRedefinidaResponse comStatusDoEmail(StatusEmailAcesso status) {
        return new SenhaRedefinidaResponse(usuarioId, nome, identificadorDeAcesso, senhaProvisoria, enviadaPorEmail,
                status);
    }
}
