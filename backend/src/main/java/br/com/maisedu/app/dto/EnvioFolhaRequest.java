package br.com.maisedu.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Entrega única: a alternativa escolhida em cada questão da folha. O envio só é aceito com todas as
 * questões respondidas (as marcações salvas servem para o aluno sair e voltar; o que vale é o enviado).
 */
public record EnvioFolhaRequest(
        @NotEmpty(message = "Responda as questões antes de enviar")
        List<@Valid @NotNull Escolha> respostas) {

    public record Escolha(
            @NotNull(message = "Questão não informada") Long atividadeAlunoQuestaoId,
            @NotNull(message = "Escolha uma alternativa") Long alternativaId) {
    }
}
