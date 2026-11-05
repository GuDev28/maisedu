package br.com.maisedu.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroProfessorRequest(
        @NotBlank(message = "Informe o nome")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
        String nome,
        @NotBlank(message = "Informe o e-mail")
        @Email(message = "E-mail inválido")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
        String email) {
}
