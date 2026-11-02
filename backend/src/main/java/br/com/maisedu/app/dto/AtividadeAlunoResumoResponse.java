package br.com.maisedu.app.dto;

import java.time.LocalDateTime;

/**
 * Item da lista de atividades do aluno. {@code atividadeAlunoId} é nulo enquanto a atividade não foi
 * iniciada; {@code atividadeId} é nulo nas práticas adaptativas.
 */
public record AtividadeAlunoResumoResponse(
        Long atividadeId,
        Long atividadeAlunoId,
        String titulo,
        String tipo,
        String turmaNome,
        String situacao,
        int totalQuestoes,
        int respondidas,
        int acertos,
        LocalDateTime iniciadaEm,
        LocalDateTime concluidaEm) {

    public static final String NAO_INICIADA = "NAO_INICIADA";
    public static final String EM_ANDAMENTO = "EM_ANDAMENTO";
    public static final String CONCLUIDA = "CONCLUIDA";
}
