package br.com.maisedu.app.dto;

import java.util.List;

/**
 * Prática adaptativa entregue ao aluno. {@code nova} é falso quando já havia uma prática em
 * andamento (do mesmo tópico, ou a recomendada), que é retomada em vez de gerar outra.
 * {@code diagnostica} é verdadeiro na primeira prática de quem ainda não tem respostas: questões
 * fáceis de vários tópicos, para o motor conhecer o aluno. {@code topico} só vem na prática que o
 * aluno pediu para um tópico (nulo na recomendada pelo motor).
 */
public record PraticaAdaptativaResponse(
        Long atividadeAlunoId,
        boolean nova,
        boolean diagnostica,
        String topico,
        List<TopicoDaPratica> topicos,
        int totalQuestoes) {

    public record TopicoDaPratica(String topico, String nivelDificuldade, int questoes) {
    }
}
