package br.com.maisedu.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** Nova atividade regular: turma, título, prazo de entrega e as questões aprovadas, na ordem. */
public record CriarAtividadeRequest(
        @NotNull(message = "Selecione a turma") Long turmaId,
        @NotBlank(message = "Informe o título da atividade")
        @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
        String titulo,
        @NotNull(message = "Informe o prazo de entrega")
        LocalDateTime prazo,
        @NotEmpty(message = "Selecione pelo menos uma questão para a atividade")
        List<@NotNull Long> questaoIds) {
}
