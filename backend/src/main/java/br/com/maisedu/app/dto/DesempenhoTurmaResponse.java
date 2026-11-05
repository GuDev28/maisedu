package br.com.maisedu.app.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Painel de desempenho da turma (RF19): indicadores gerais, cada aluno, cada tópico (com o detalhe por
 * nível de dificuldade), do menor para o maior percentual, e a evolução semanal da turma.
 */
public record DesempenhoTurmaResponse(
        Long turmaId,
        String turmaNome,
        String anoEscolar,
        ResumoTurma resumo,
        List<AlunoResumo> alunos,
        List<TopicoTurma> topicos,
        List<DesempenhoAlunoResponse.PontoEvolucao> evolucao) {

    /** Indicadores gerais da turma (big numbers do painel). */
    public record ResumoTurma(int totalAlunos, int alunosComRespostas, long totalAtividades, int totalRespostas,
                              int totalAcertos, BigDecimal percentual) {
    }

    public record AlunoResumo(Long alunoId, String nome, int totalRespostas, int totalAcertos, BigDecimal percentual,
                              String topicoMaisFraco, BigDecimal percentualTopicoMaisFraco) {
    }

    public record TopicoTurma(Long topicoId, String topico, int totalRespostas, int totalAcertos,
                              BigDecimal percentual, int alunosComRespostas, List<NivelTurma> niveis) {
    }

    /** Soma dos alunos da turma em um tópico e nível de dificuldade. */
    public record NivelTurma(String nivelDificuldade, int totalRespostas, int totalAcertos, BigDecimal percentual) {
    }
}
