package br.com.maisedu.app.dto;

import java.math.BigDecimal;
import java.util.List;

/** Visão da turma para o professor: cada aluno e cada tópico, do menor para o maior percentual. */
public record DesempenhoTurmaResponse(
        Long turmaId,
        String turmaNome,
        String anoEscolar,
        List<AlunoResumo> alunos,
        List<TopicoTurma> topicos) {

    public record AlunoResumo(Long alunoId, String nome, int totalRespostas, int totalAcertos, BigDecimal percentual,
                              String topicoMaisFraco, BigDecimal percentualTopicoMaisFraco) {
    }

    public record TopicoTurma(Long topicoId, String topico, int totalRespostas, int totalAcertos,
                              BigDecimal percentual, int alunosComRespostas) {
    }
}
