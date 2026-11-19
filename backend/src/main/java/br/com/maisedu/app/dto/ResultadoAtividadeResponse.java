package br.com.maisedu.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado de uma atividade regular por aluno da turma e análise por questão (visão do professor).
 * O professor vê os acertos de quem já entregou mesmo antes do prazo (o aluno, só depois).
 */
public record ResultadoAtividadeResponse(
        Long atividadeId,
        String titulo,
        String turmaNome,
        LocalDateTime prazo,
        boolean prazoEncerrado,
        List<QuestaoResponse> questoes,
        List<ResultadoAluno> alunos,
        List<AnaliseQuestao> analise) {

    /** {@code escolhidas}: marcadas (em andamento) ou enviadas (entregue). {@code acertos}: só de quem entregou. */
    public record ResultadoAluno(Long alunoId, String nome, String situacao, int escolhidas, int acertos,
                                 int totalQuestoes) {
    }

    /**
     * Uma questão da atividade: quantos responderam e acertaram e a alternativa errada mais escolhida
     * (o erro de raciocínio mais comum da turma). {@code erroMaisComum} é nulo se ninguém errou.
     */
    public record AnaliseQuestao(Long questaoId, int ordem, String enunciado, String topico, String nivelDificuldade,
                                 char letraCorreta, int respostas, int acertos, BigDecimal percentualAcerto,
                                 ErroComum erroMaisComum) {
    }

    /** Alternativa errada mais escolhida e a fatia dos erros que ela representa. */
    public record ErroComum(char letra, String texto, int vezes, BigDecimal percentualDosErros) {
    }
}
