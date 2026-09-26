package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.Alternativa;

public record AlternativaResponse(Long id, char letra, String texto, boolean correta) {

    public static AlternativaResponse from(Alternativa alternativa) {
        return new AlternativaResponse(
                alternativa.getId(), alternativa.letra(), alternativa.getTexto(), alternativa.isCorreta());
    }
}
