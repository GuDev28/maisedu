package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.VotoAvaliacao;
import jakarta.validation.constraints.NotNull;

public record AvaliacaoQuestaoRequest(@NotNull(message = "Informe o voto (APROVAR ou REJEITAR)") VotoAvaliacao voto) {
}
