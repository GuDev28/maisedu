package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.ProfessorTurma;

public record ProfessorTurmaResponse(Long id, Long professorId, Long turmaId) {

    public static ProfessorTurmaResponse from(ProfessorTurma vinculo) {
        return new ProfessorTurmaResponse(
                vinculo.getId(),
                vinculo.getProfessor().getId(),
                vinculo.getTurma().getId());
    }

}
