package br.com.maisedu.app.dto;

/**
 * Resultado da redefinição de senha. A senha nova vai por e-mail: para o próprio professor/admin ou,
 * no caso do aluno, para o e-mail do responsável; então {@code senhaProvisoria} é nula. Ela só volta
 * aqui para aluno cadastrado antes do e-mail do responsável existir (sem e-mail para onde mandar).
 */
public record SenhaRedefinidaResponse(
        Long usuarioId,
        String nome,
        String identificadorDeAcesso,
        String senhaProvisoria,
        boolean enviadaPorEmail) {
}
