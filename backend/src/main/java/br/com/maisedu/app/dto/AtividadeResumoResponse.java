package br.com.maisedu.app.dto;

import java.time.LocalDateTime;

/** Atividade da turma, para o professor: quantas questões e quantos alunos já iniciaram/concluíram. */
public record AtividadeResumoResponse(
        Long id,
        String titulo,
        Long turmaId,
        String turmaNome,
        String professorNome,
        int totalQuestoes,
        int alunosQueIniciaram,
        int alunosQueConcluiram,
        LocalDateTime criadoEm) {
}
