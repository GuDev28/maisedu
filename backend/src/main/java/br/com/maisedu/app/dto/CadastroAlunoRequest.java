package br.com.maisedu.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroAlunoRequest(
        @NotBlank(message = "Informe o nome")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
        String nome,

        @NotBlank(message = "Informe o login do aluno")
        @Pattern(regexp = "^\\s*[A-Za-z0-9._-]{3,50}\\s*$",
                message = "O login deve ter de 3 a 50 caracteres: letras sem acento, números, ponto, hífen ou sublinhado")
        String login,

        /** Recebe o login e a senha provisória do aluno (o aluno não tem e-mail próprio). */
        @NotBlank(message = "Informe o e-mail do responsável pelo aluno")
        @Email(message = "E-mail do responsável inválido")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
        String emailResponsavel,

        /** Opcional: já vincula o aluno a esta turma no mesmo passo do cadastro. */
        Long turmaId) {

    public CadastroAlunoRequest(String nome, String login, String emailResponsavel) {
        this(nome, login, emailResponsavel, null);
    }
}
