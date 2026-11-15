package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.VotoAvaliacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Voto na validação colaborativa (RN02). O comentário é obrigatório quando o voto é REJEITAR:
 * é o que o autor lê para corrigir a questão.
 */
public record AvaliacaoQuestaoRequest(
        @NotNull(message = "Informe o voto (APROVAR ou REJEITAR)") VotoAvaliacao voto,
        @Size(max = 500, message = "O comentário deve ter no máximo 500 caracteres") String comentario) {

    public AvaliacaoQuestaoRequest(VotoAvaliacao voto) {
        this(voto, null);
    }
}
