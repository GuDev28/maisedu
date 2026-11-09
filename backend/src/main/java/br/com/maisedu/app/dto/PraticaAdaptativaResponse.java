package br.com.maisedu.app.dto;

import java.util.List;

/**
 * Prática adaptativa entregue ao aluno. {@code nova} é falso quando já havia uma prática em
 * andamento, que é retomada em vez de gerar outra. {@code diagnostica} é verdadeiro na primeira
 * prática de quem ainda não tem respostas: questões fáceis de vários tópicos, para o motor
 * conhecer o aluno.
 */
public record PraticaAdaptativaResponse(
        Long atividadeAlunoId,
        boolean nova,
        boolean diagnostica,
        List<TopicoDaPratica> topicos,
        int totalQuestoes) {

    public record TopicoDaPratica(String topico, String nivelDificuldade, int questoes) {
    }
}
