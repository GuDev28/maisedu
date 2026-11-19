package br.com.maisedu.app.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tela "Praticar" do aluno: a prática recomendada pelo motor adaptativo e um card por tópico que ele
 * pode praticar (tópicos com questões aprovadas do ano escolar dele, mais os que ele já respondeu).
 */
public record PainelPraticaResponse(Recomendada recomendada, List<TopicoParaPraticar> topicos) {

    /**
     * {@code praticaEmAndamentoId}: prática recomendada já gerada e ainda não enviada (para continuar).
     * {@code diagnostica}: o aluno ainda não tem respostas, então a próxima recomendada é a diagnóstica.
     * {@code topicos}: o que o motor escolheria agora, do tópico mais fraco para o mais forte.
     */
    public record Recomendada(Long praticaEmAndamentoId, boolean diagnostica, List<TopicoPlanejado> topicos) {
    }

    public record TopicoPlanejado(Long topicoId, String topico, BigDecimal percentual, String nivelAlvo) {
    }

    /**
     * Card de um tópico. {@code percentual} é nulo sem respostas no tópico. {@code nivelAtual} é o
     * nível-alvo do motor (o mais baixo ainda não dominado). {@code recomendado}: está entre os tópicos
     * que o motor escolheria para a prática recomendada.
     */
    public record TopicoParaPraticar(Long topicoId, String topico, int questoesDisponiveis, int totalRespostas,
                                     BigDecimal percentual, String nivelAtual, List<Nivel> niveis,
                                     Long praticaEmAndamentoId, boolean recomendado) {
    }

    /** Situação do aluno em um nível do tópico. */
    public record Nivel(String nivelDificuldade, int respostas, BigDecimal percentual, boolean dominado) {
    }
}
