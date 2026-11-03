package br.com.maisedu.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Painel de desempenho de um aluno (RF19). Os percentuais vêm dos mesmos registros usados pelo
 * motor adaptativo (RN05); os tópicos vêm do menor para o maior percentual.
 */
public record DesempenhoAlunoResponse(
        Long alunoId,
        String nome,
        int totalRespostas,
        int totalAcertos,
        BigDecimal percentualGeral,
        List<TopicoDesempenho> topicos,
        List<PontoEvolucao> evolucao) {

    public record TopicoDesempenho(Long topicoId, String topico, int totalRespostas, int totalAcertos,
                                   BigDecimal percentual, List<NivelDesempenho> niveis) {
    }

    public record NivelDesempenho(String nivelDificuldade, int totalRespostas, int totalAcertos,
                                  BigDecimal percentual, boolean dominado) {
    }

    /** Uma semana (a partir da segunda-feira): respostas da semana e percentual acumulado até ela. */
    public record PontoEvolucao(LocalDate semana, int respostas, int acertos, BigDecimal percentualSemana,
                                BigDecimal percentualAcumulado) {
    }
}
