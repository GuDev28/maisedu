package br.com.maisedu.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Troca de senha. O aceite dos Termos/Política só é exigido quando ainda falta um aceite (primeiro
 * acesso ou versão nova dos documentos); na troca voluntária, em "Minha conta", ele é ignorado.
 */
public record TrocarSenhaRequest(
        @NotBlank(message = "Informe a senha atual") String senhaAtual,
        @NotBlank(message = "Informe a nova senha")
        @Size(min = 8, message = "A nova senha deve ter pelo menos 8 caracteres")
        String novaSenha,
        boolean aceiteTermos) {
}
