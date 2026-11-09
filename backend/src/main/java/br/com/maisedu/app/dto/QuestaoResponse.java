package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.Alternativa;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.StatusQuestao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Questão como o front-end a vê. {@code autorInstituicao} mostra de qual escola veio a questão do
 * banco compartilhado. O placar ({@code aprovacoes}, {@code rejeicoes}) e os motivos de rejeição
 * só vêm preenchidos em "Minhas questões", para o autor acompanhar a validação.
 */
public record QuestaoResponse(
        Long id,
        String enunciado,
        Long topicoId,
        String topico,
        NivelDificuldade nivelDificuldade,
        AnoEscolar anoEscolar,
        StatusQuestao status,
        String autorNome,
        String autorInstituicao,
        List<AlternativaResponse> alternativas,
        LocalDateTime criadoEm,
        Long aprovacoes,
        Long rejeicoes,
        List<String> motivosRejeicao) {

    public static QuestaoResponse from(Questao questao) {
        return montar(questao, null, null, null);
    }

    /** Versão do autor: inclui o placar da validação e os motivos de quem rejeitou. */
    public static QuestaoResponse comPlacar(Questao questao, long aprovacoes, long rejeicoes,
                                            List<String> motivosRejeicao) {
        return montar(questao, aprovacoes, rejeicoes, motivosRejeicao);
    }

    private static QuestaoResponse montar(Questao questao, Long aprovacoes, Long rejeicoes,
                                          List<String> motivosRejeicao) {
        List<AlternativaResponse> alternativas = new ArrayList<>();
        for (Alternativa alternativa : questao.getAlternativas()) {
            alternativas.add(AlternativaResponse.from(alternativa));
        }
        String instituicao = questao.getAutor().getInstituicao() == null
                ? null
                : questao.getAutor().getInstituicao().getNome();
        return new QuestaoResponse(
                questao.getId(),
                questao.getEnunciado(),
                questao.getTopico().getId(),
                questao.getTopico().getNome(),
                questao.getNivelDificuldade(),
                questao.getAnoEscolar(),
                questao.getStatus(),
                questao.getAutor().getNome(),
                instituicao,
                alternativas,
                questao.getCriadoEm(),
                aprovacoes,
                rejeicoes,
                motivosRejeicao);
    }
}
