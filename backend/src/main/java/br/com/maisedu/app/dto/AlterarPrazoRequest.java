package br.com.maisedu.app.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AlterarPrazoRequest(@NotNull(message = "Informe o novo prazo de entrega") LocalDateTime prazo) {
}
