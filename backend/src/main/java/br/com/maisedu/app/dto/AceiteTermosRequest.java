package br.com.maisedu.app.dto;

import jakarta.validation.constraints.AssertTrue;

public record AceiteTermosRequest(
        @AssertTrue(message = "É preciso aceitar os Termos de Uso e a Política de Privacidade para continuar")
        boolean aceiteTermos) {
}
