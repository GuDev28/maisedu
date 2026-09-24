package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.StatusQuestao;


public record AvaliacaoQuestaoResponse(
        Long questaoId,
        StatusQuestao status,
        long aprovacoes,
        long rejeicoes,
        int votosParaDecidir,
        int maximoVotos) {
}
