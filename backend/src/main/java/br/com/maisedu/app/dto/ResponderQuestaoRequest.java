package br.com.maisedu.app.dto;

import jakarta.validation.constraints.NotNull;

public record ResponderQuestaoRequest(@NotNull(message = "Escolha uma alternativa") Long alternativaId) {
}
