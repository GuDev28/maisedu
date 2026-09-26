package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.Alternativa;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.StatusQuestao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record QuestaoResponse(
        Long id,
        String enunciado,
        String topico,
        NivelDificuldade nivelDificuldade,
        StatusQuestao status,
        String autorNome,
        List<AlternativaResponse> alternativas,
        LocalDateTime criadoEm) {

    public static QuestaoResponse from(Questao questao) {
        List<AlternativaResponse> alternativas = new ArrayList<>();
        for (Alternativa alternativa : questao.getAlternativas()) {
            alternativas.add(AlternativaResponse.from(alternativa));
        }

        return new QuestaoResponse(
                questao.getId(),
                questao.getEnunciado(),
                questao.getTopico().getNome(),
                questao.getNivelDificuldade(),
                questao.getStatus(),
                questao.getAutor().getNome(),
                alternativas,
                questao.getCriadoEm());
    }
}
