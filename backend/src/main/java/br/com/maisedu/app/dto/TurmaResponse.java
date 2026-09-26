package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.Turma;

public record TurmaResponse(Long id, String nome, String anoEscolar) {

    public static TurmaResponse from(Turma turma) {
        return new TurmaResponse(turma.getId(), turma.getNome(), turma.getAnoEscolar().name());
    }
}
