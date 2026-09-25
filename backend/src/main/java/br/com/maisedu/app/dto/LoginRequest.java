package br.com.maisedu.app.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Informe o e-mail ou o login") String identificador,
        @NotBlank(message = "Informe a senha") String senha) {
}
