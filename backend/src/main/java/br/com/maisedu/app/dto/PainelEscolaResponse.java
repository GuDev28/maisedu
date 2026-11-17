package br.com.maisedu.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Painel pedagógico da escola para o administrador. Minimização (LGPD): só números agregados por ano
 * escolar, turma e semana, sem nomes nem resultados de alunos. Grupos com menos de
 * {@code minimoAlunosPorGrupo} alunos aparecem sem percentual ("poucos dados"), para que um
 * percentual não revele o resultado de um aluno identificável.
 */
public record PainelEscolaResponse(
        String instituicao,
        int minimoAlunosPorGrupo,
        List<CelulaAnoTopico> mapaAnoTopico,
        List<TurmaComparada> turmas,
        List<SemanaEngajamento> engajamento,
        BancoCompartilhado banco) {

    /** Acerto da escola num tópico, entre os alunos das turmas ativas de um ano. {@code percentual} nulo: poucos dados. */
    public record CelulaAnoTopico(String anoEscolar, Long topicoId, String topico, int alunos, int respostas,
                                  BigDecimal percentual) {
    }

    /**
     * Uma turma ativa: alunos, quantos já responderam algo, quantos responderam nos últimos 14 dias,
     * atividades criadas, respostas contabilizadas e acerto médio ({@code percentual} nulo: poucos dados).
     */
    public record TurmaComparada(Long turmaId, String turma, String anoEscolar, int alunos, int alunosComRespostas,
                                 int alunosAtivosRecentes, int atividades, int respostas, BigDecimal percentual) {
    }

    /** Uma semana (a partir da segunda-feira) da escola. */
    public record SemanaEngajamento(LocalDate semana, int alunosAtivos, int respostas, int atividadesCriadas,
                                    int praticasIniciadas) {
    }

    /** Participação da escola no banco de questões compartilhado. */
    public record BancoCompartilhado(long rascunhos, long pendentes, long aprovadas, long rejeitadas, long votosDados,
                                     long usosPorOutrasEscolas, long usosNaPropriaEscola,
                                     long questoesDeOutrasEscolasUsadas) {
    }
}
