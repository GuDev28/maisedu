package br.com.maisedu.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Visão da turma para o professor (RF19): cada aluno e cada tópico, do menor para o maior
 * percentual, o mapa de calor aluno × tópico, a evolução semanal da turma e o efeito da prática
 * adaptativa. Só entram respostas já contabilizadas (atividades fechadas e práticas enviadas).
 */
public record DesempenhoTurmaResponse(
        Long turmaId,
        String turmaNome,
        String anoEscolar,
        List<AlunoResumo> alunos,
        List<TopicoTurma> topicos,
        List<Celula> celulas,
        List<PontoEvolucaoTurma> evolucao,
        List<EfeitoPratica> efeitoPratica) {

    public record AlunoResumo(Long alunoId, String nome, int totalRespostas, int totalAcertos, BigDecimal percentual,
                              String topicoMaisFraco, BigDecimal percentualTopicoMaisFraco) {
    }

    public record TopicoTurma(Long topicoId, String topico, int totalRespostas, int totalAcertos,
                              BigDecimal percentual, int alunosComRespostas) {
    }

    /** Uma célula do mapa de calor: o aluno no tópico (somando os níveis). */
    public record Celula(Long alunoId, Long topicoId, int respostas, int acertos, BigDecimal percentual) {
    }

    /** Uma semana (a partir da segunda-feira): respostas da turma, alunos que responderam e percentuais. */
    public record PontoEvolucaoTurma(LocalDate semana, int respostas, int acertos, int alunosAtivos,
                                     BigDecimal percentualSemana, BigDecimal percentualAcumulado) {
    }

    /**
     * Acerto nas atividades regulares do tópico antes e depois da primeira prática adaptativa do aluno
     * nesse tópico (as respostas da própria prática não entram). Só conta quem tem respostas nos dois
     * períodos; {@code alunos} diz quantos são.
     */
    public record EfeitoPratica(Long topicoId, String topico, int alunos, int respostasAntes,
                                BigDecimal percentualAntes, int respostasDepois, BigDecimal percentualDepois) {
    }
}
