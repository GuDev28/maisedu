package br.com.maisedu.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CriarAtividadeRequest(
        @NotNull(message = "Selecione a turma") Long turmaId,
        @NotBlank(message = "Informe o título da atividade")
        @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
        String titulo,
        @NotEmpty(message = "Selecione pelo menos uma questão para a atividade")
        List<@NotNull Long> questaoIds) {
}
