package br.com.maisedu.app.dto;

import java.time.LocalDateTime;

/**
 * Item da lista de atividades do aluno. {@code atividadeAlunoId} é nulo enquanto a atividade não foi
 * iniciada; {@code atividadeId} e {@code prazo} são nulos nas práticas adaptativas, e {@code topico}
 * só vem na prática que o aluno pediu para um tópico.
 *
 * {@code escolhidas}: questões com alternativa escolhida (marcada ou enviada). {@code acertos} é nulo
 * enquanto a correção não foi liberada (na atividade regular, até o prazo).
 */
public record AtividadeAlunoResumoResponse(
        Long atividadeId,
        Long atividadeAlunoId,
        String titulo,
        String tipo,
        String turmaNome,
        String topico,
        String situacao,
        int totalQuestoes,
        int escolhidas,
        Integer acertos,
        LocalDateTime prazo,
        boolean correcaoLiberada,
        LocalDateTime iniciadaEm,
        LocalDateTime enviadaEm) {

    public static final String NAO_INICIADA = "NAO_INICIADA";
    public static final String EM_ANDAMENTO = "EM_ANDAMENTO";
    /** Respostas enviadas (entrega única). */
    public static final String ENTREGUE = "ENTREGUE";
    /** O prazo terminou sem o envio (as marcações não contam). */
    public static final String NAO_ENTREGUE = "NAO_ENTREGUE";
}
