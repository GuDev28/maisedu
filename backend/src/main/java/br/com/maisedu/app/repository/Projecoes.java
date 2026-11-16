package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.StatusQuestao;
import br.com.maisedu.app.model.TipoAtividade;

import java.time.LocalDateTime;

/**
 * Projeções das consultas agregadas (contagens e somas feitas no banco), usadas pelos painéis e pela
 * prática por tópico. Cada uma é preenchida por uma expressão {@code select new ...} nos repositórios.
 */
public final class Projecoes {

    private Projecoes() {
    }

    /** Uma contagem por id (turma, atividade...). */
    public record ContagemPorId(Long id, Long total) {
    }

    /** Questões aprovadas por tópico. */
    public record ContagemTopico(Long topicoId, String topico, Long total) {
    }

    /** Questões por status. */
    public record ContagemStatusQuestao(StatusQuestao status, Long total) {
    }

    /** Quantas vezes cada alternativa foi escolhida em cada questão de uma atividade. */
    public record ContagemAlternativa(Long questaoId, Long alternativaId, Long total) {
    }

    /**
     * Data e acerto de uma resposta, com o aluno (evolução da turma e engajamento). Acerto como
     * wrapper porque o Hibernate instancia o record direto da consulta (como em RespostaHistorico).
     */
    public record RespostaDoAluno(Long alunoId, LocalDateTime respondidaEm, Boolean correta) {
        public boolean acertou() {
            return Boolean.TRUE.equals(correta);
        }
    }

    /** Resposta com tópico e tipo da folha (efeito da prática adaptativa). */
    public record RespostaPorTopico(Long alunoId, Long topicoId, String topico, LocalDateTime respondidaEm,
                                    Boolean correta, TipoAtividade tipo) {
        public boolean acertou() {
            return Boolean.TRUE.equals(correta);
        }
    }

    /** Desempenho somado por ano escolar (da turma) e tópico, com o número de alunos distintos. */
    public record DesempenhoAnoTopico(AnoEscolar anoEscolar, Long topicoId, String topico, Long respostas,
                                      Long acertos, Long alunos) {
    }

    /** Desempenho somado dos alunos de uma turma (somas nulas quando ninguém respondeu). */
    public record DesempenhoDaTurma(Long turmaId, String turma, AnoEscolar anoEscolar, Long alunos,
                                    Long alunosComRespostas, Long respostas, Long acertos) {
    }
}
