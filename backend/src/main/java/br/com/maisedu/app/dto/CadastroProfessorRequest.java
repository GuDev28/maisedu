package br.com.maisedu.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CadastroProfessorRequest(
        @NotBlank(message = "Informe o nome") String nome,
        @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") String email) {
}
