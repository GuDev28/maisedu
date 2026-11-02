package br.com.maisedu.app.dto;

/** Correção automática de uma resposta e o progresso da atividade depois dela. */
public record RespostaResultadoResponse(
        boolean acertou,
        Long alternativaEscolhidaId,
        Long alternativaCorretaId,
        char letraCorreta,
        boolean atividadeConcluida,
        int respondidas,
        int acertos,
        int totalQuestoes) {
}
