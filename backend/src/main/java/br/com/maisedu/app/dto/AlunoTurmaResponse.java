package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.AlunoTurma;

public record AlunoTurmaResponse(Long id, Long alunoId, Long turmaId) {

    public static AlunoTurmaResponse from(AlunoTurma vinculo) {
        return new AlunoTurmaResponse(
                vinculo.getId(),
                vinculo.getAluno().getId(),
                vinculo.getTurma().getId());
    }

}
