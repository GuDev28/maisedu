package br.com.maisedu.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Correção do e-mail do responsável (ex.: digitado errado no cadastro). */
public record EmailResponsavelRequest(
        @NotBlank(message = "Informe o e-mail do responsável")
        @Email(message = "E-mail do responsável inválido")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
        String emailResponsavel) {
}
