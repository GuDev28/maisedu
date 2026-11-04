package br.com.maisedu.app.dto;

/**
 * Resultado da redefinição de senha. Para quem tem e-mail (professor/admin), a senha vai só por
 * e-mail e {@code senhaProvisoria} é nula; para alunos, ela volta aqui para ser repassada em sala.
 */
public record SenhaRedefinidaResponse(
        Long usuarioId,
        String nome,
        String identificadorDeAcesso,
        String senhaProvisoria,
        boolean enviadaPorEmail) {
}
