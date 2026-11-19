package br.com.maisedu.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cadastro de aluno: o login é gerado pelo sistema e vai, com a senha provisória, ao e-mail do responsável. */
public record CadastroAlunoRequest(
        @NotBlank(message = "Informe o nome")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
        String nome,

        /** Recebe o login e a senha provisória do aluno (o aluno não tem e-mail próprio). */
        @NotBlank(message = "Informe o e-mail do responsável pelo aluno")
        @Email(message = "E-mail do responsável inválido")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
        String emailResponsavel,

        /** Opcional: já vincula o aluno a esta turma no mesmo passo do cadastro. */
        Long turmaId) {

    public CadastroAlunoRequest(String nome, String emailResponsavel) {
        this(nome, emailResponsavel, null);
    }
}
