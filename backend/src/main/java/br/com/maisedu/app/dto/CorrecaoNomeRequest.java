package br.com.maisedu.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CorrecaoNomeRequest(
        @NotBlank(message = "Informe o nome")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
        String nome) {
}
