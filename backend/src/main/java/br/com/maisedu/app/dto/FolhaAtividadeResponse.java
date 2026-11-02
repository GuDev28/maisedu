package br.com.maisedu.app.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Questões entregues ao aluno (snapshot da RN03), na ordem fixada ao iniciar. As alternativas NÃO
 * indicam a correta; o gabarito de uma questão só aparece em {@code resposta} depois que ela foi respondida.
 */
public record FolhaAtividadeResponse(
        Long atividadeAlunoId,
        String titulo,
        String tipo,
        LocalDateTime iniciadaEm,
        LocalDateTime concluidaEm,
        int totalQuestoes,
        int respondidas,
        int acertos,
        List<QuestaoEntregue> questoes) {

    public record QuestaoEntregue(Long atividadeAlunoQuestaoId, int ordem, String enunciado, String topico,
                                  String nivelDificuldade, List<AlternativaAluno> alternativas,
                                  RespostaDada resposta) {
    }

    public record AlternativaAluno(Long id, char letra, String texto) {
    }

    public record RespostaDada(Long alternativaEscolhidaId, char letraEscolhida, boolean acertou,
                               Long alternativaCorretaId, char letraCorreta) {
    }
}
