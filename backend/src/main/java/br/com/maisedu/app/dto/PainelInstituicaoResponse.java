package br.com.maisedu.app.dto;

/** Números da página Início do administrador: a escola em um relance. */
public record PainelInstituicaoResponse(
        String instituicao,
        long turmasAtivas,
        long turmasInativas,
        long professores,
        long alunos,
        long contasBloqueadas,
        long primeiroAcessoPendente,
        long contasDesativadas,
        long loginsUltimos7Dias,
        long questoesNoBancoCompartilhado) {
}
