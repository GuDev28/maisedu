package br.com.maisedu.app.dto;

import java.time.LocalDateTime;

/**
 * Atividade da turma, para o professor: questões, prazo e quantos alunos da turma iniciaram e
 * entregaram. {@code fechada}: o desempenho das respostas já foi contabilizado (depois do prazo).
 */
public record AtividadeResumoResponse(
        Long id,
        String titulo,
        Long turmaId,
        String turmaNome,
        String professorNome,
        int totalQuestoes,
        int alunosDaTurma,
        int alunosQueIniciaram,
        int alunosQueEntregaram,
        LocalDateTime criadoEm,
        LocalDateTime prazo,
        boolean prazoEncerrado,
        boolean fechada) {
}
