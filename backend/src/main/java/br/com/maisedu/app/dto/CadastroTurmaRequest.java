package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.AnoEscolar;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CadastroTurmaRequest(
        @NotBlank(message = "Informe o nome da turma")
        @Size(max = 100, message = "O nome da turma deve ter no máximo 100 caracteres")
        String nome,
        @NotNull(message = "Selecione o ano escolar (6º ao 9º ano)") AnoEscolar anoEscolar) {
}
