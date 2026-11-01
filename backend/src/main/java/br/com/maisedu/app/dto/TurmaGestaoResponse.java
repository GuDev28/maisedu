package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.Turma;

import java.time.LocalDateTime;

public record TurmaGestaoResponse(Long id, String nome, String anoEscolar, boolean ativo, LocalDateTime criadoEm) {

    public static TurmaGestaoResponse from(Turma turma) {
        return new TurmaGestaoResponse(turma.getId(), turma.getNome(), turma.getAnoEscolar().name(),
                turma.isAtivo(), turma.getCriadoEm());
    }
}
