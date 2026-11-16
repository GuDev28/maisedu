package br.com.maisedu.app.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Questões entregues ao aluno (snapshot da RN03), na ordem fixada ao iniciar, com as alternativas
 * escolhidas por ele. As alternativas NÃO indicam a correta: {@code correcao} só vem preenchida
 * quando a correção foi liberada (prática adaptativa: depois do envio; atividade regular: depois do prazo).
 *
 * {@code podeResponder}: a folha ainda aceita marcações e o envio (não enviada e dentro do prazo).
 * {@code prazo} é nulo na prática adaptativa. {@code acertos} é nulo enquanto a correção não foi liberada.
 */
public record FolhaAtividadeResponse(
        Long atividadeAlunoId,
        String titulo,
        String tipo,
        String topico,
        LocalDateTime iniciadaEm,
        LocalDateTime enviadaEm,
        LocalDateTime prazo,
        boolean prazoEncerrado,
        boolean podeResponder,
        boolean correcaoLiberada,
        int totalQuestoes,
        int escolhidas,
        Integer acertos,
        List<QuestaoEntregue> questoes) {

    /**
     * {@code alternativaEscolhidaId}: a marcada (ainda pode trocar) ou a enviada; nula se o aluno não
     * escolheu nada. {@code correcao}: nula até a correção ser liberada.
     */
    public record QuestaoEntregue(Long atividadeAlunoQuestaoId, int ordem, String enunciado, String topico,
                                  String nivelDificuldade, List<AlternativaAluno> alternativas,
                                  Long alternativaEscolhidaId, Correcao correcao) {
    }

    public record AlternativaAluno(Long id, char letra, String texto) {
    }

    /** {@code respondida}: a questão foi enviada (quem não entregou vê só o gabarito). */
    public record Correcao(boolean respondida, boolean acertou, Long alternativaCorretaId, char letraCorreta) {
    }
}
