package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.VotoAvaliacao;

public record AvaliacaoQuestaoRequest(Long professorId, VotoAvaliacao voto) {
}
