package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.NivelDificuldade;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CriarQuestaoRequest(
        @NotBlank(message = "Informe o enunciado") String enunciado,
        @NotNull(message = "Selecione o tópico") Long topicoId,
        @NotNull(message = "Selecione o nível de dificuldade") NivelDificuldade nivelDificuldade,
        @NotNull(message = "Informe as 5 alternativas")
        @Size(min = 5, max = 5, message = "A questão deve ter exatamente 5 alternativas")
        List<@NotBlank(message = "Nenhuma alternativa pode ficar em branco") String> alternativas,
        @NotNull(message = "Indique qual alternativa é a correta")
        @Min(value = 0, message = "Índice da alternativa correta inválido")
        @Max(value = 4, message = "Índice da alternativa correta inválido")
        Integer indiceCorreta) {
}
