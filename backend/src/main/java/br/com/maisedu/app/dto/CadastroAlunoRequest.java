package br.com.maisedu.app.dto;

import jakarta.validation.constraints.NotBlank;

public record CadastroAlunoRequest(
        @NotBlank(message = "Informe o nome") String nome,
        @NotBlank(message = "Informe o login do aluno") String login) {
}
