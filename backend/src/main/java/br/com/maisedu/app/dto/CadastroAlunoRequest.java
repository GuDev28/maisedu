package br.com.maisedu.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroAlunoRequest(
        @NotBlank(message = "Informe o nome")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
        String nome,
        @NotBlank(message = "Informe o login do aluno")
        @Pattern(regexp = "^\\s*[A-Za-z0-9._-]{3,50}\\s*$",
                message = "O login deve ter de 3 a 50 caracteres: letras sem acento, números, "
                        + "ponto, hífen ou sublinhado")
        String login) {
}
