package br.com.maisedu.app.dto;

import java.util.List;

/** Resultado de uma atividade regular por aluno da turma (visão do professor). */
public record ResultadoAtividadeResponse(
        Long atividadeId,
        String titulo,
        String turmaNome,
        List<QuestaoResponse> questoes,
        List<ResultadoAluno> alunos) {

    public record ResultadoAluno(Long alunoId, String nome, String situacao, int respondidas, int acertos,
                                 int totalQuestoes) {
    }
}
