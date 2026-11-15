package br.com.maisedu.app.model;

import br.com.maisedu.app.exception.NegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class QuestaoTest {

    private Questao questao;

    @BeforeEach
    void setUp() {
        questao = new Questao("Quanto é 3/4 de 20?", mock(Usuario.class),
                new Topico("Frações"), NivelDificuldade.MEDIO);
    }

    @Test
    void defineAlternativasComUmaCorretaEOrdem() {
        questao.definirAlternativas(List.of("5", "10", "15", "20", "25"), 2);

        assertThat(questao.getAlternativas()).hasSize(5);
        assertThat(questao.getAlternativas()).filteredOn(Alternativa::isCorreta)
                .singleElement().satisfies(a -> {
                    assertThat(a.getTexto()).isEqualTo("15");
                    assertThat(a.letra()).isEqualTo('C');
                });
    }

    @Test
    void recusaMenosDeCincoAlternativas() {
        assertThatThrownBy(() -> questao.definirAlternativas(List.of("5", "10", "15", "20"), 0))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void recusaMaisDeCincoAlternativas() {
        assertThatThrownBy(() -> questao.definirAlternativas(List.of("1", "2", "3", "4", "5", "6"), 0))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void recusaAlternativaVazia() {
        assertThatThrownBy(() -> questao.definirAlternativas(List.of("5", " ", "15", "20", "25"), 0))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void recusaSemIndicarACorreta() {
        assertThatThrownBy(() -> questao.definirAlternativas(List.of("5", "10", "15", "20", "25"), 5))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void naoAlteraAlternativasDeQuestaoJaDecidida() {
        questao.definirAlternativas(List.of("5", "10", "15", "20", "25"), 0);
        questao.enviarParaValidacao();
        questao.apurarVotos(Questao.VOTOS_PARA_DECIDIR, 0);
        assertThatThrownBy(() -> questao.definirAlternativas(List.of("5", "10", "15", "20", "25"), 0))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void nasceComoRascunho() {
        assertThat(questao.getStatus()).isEqualTo(StatusQuestao.RASCUNHO);
    }

    @Test
    void naoAlteraAlternativasDeQuestaoJaEnviadaParaValidacao() {
        questao.definirAlternativas(List.of("5", "10", "15", "20", "25"), 0);
        questao.enviarParaValidacao();

        assertThatThrownBy(() -> questao.definirAlternativas(List.of("1", "2", "3", "4", "5"), 0))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void enviaParaValidacaoQuandoTemAsCincoAlternativas() {
        questao.definirAlternativas(List.of("5", "10", "15", "20", "25"), 0);

        questao.enviarParaValidacao();

        assertThat(questao.getStatus()).isEqualTo(StatusQuestao.PENDENTE);
    }

    @Test
    void naoEnviaParaValidacaoSemAlternativas() {
        assertThatThrownBy(questao::enviarParaValidacao)
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void naoEnviaParaValidacaoDeQuestaoQueNaoEstaEmRascunho() {
        questao.definirAlternativas(List.of("5", "10", "15", "20", "25"), 0);
        questao.enviarParaValidacao();

        assertThatThrownBy(questao::enviarParaValidacao)
                .isInstanceOf(NegocioException.class);
    }

    private void enviada() {
        questao.definirAlternativas(List.of("5", "10", "15", "20", "25"), 2);
        questao.enviarParaValidacao();
    }

    @Test
    void terceiraAprovacaoAprovaATerceiraRejeicaoRejeita() {
        enviada();
        questao.apurarVotos(2, 1);
        assertThat(questao.getStatus()).isEqualTo(StatusQuestao.PENDENTE);
        questao.apurarVotos(3, 1);
        assertThat(questao.getStatus()).isEqualTo(StatusQuestao.APROVADA);

        Questao outra = new Questao("Quanto é 7 × 8?", mock(Usuario.class), new Topico("Multiplicação"),
                NivelDificuldade.FACIL);
        outra.definirAlternativas(List.of("54", "56", "58", "60", "64"), 0);
        outra.enviarParaValidacao();
        outra.apurarVotos(1, 3);
        assertThat(outra.getStatus()).isEqualTo(StatusQuestao.REJEITADA);
    }

    @Test
    void doisADoisContinuaPendente() {
        enviada();
        questao.apurarVotos(2, 2);
        assertThat(questao.getStatus()).isEqualTo(StatusQuestao.PENDENTE);
    }

    @Test
    void naoApuraVotosDeQuestaoQueNaoEstaPendente() {
        assertThatThrownBy(() -> questao.apurarVotos(3, 0)).isInstanceOf(NegocioException.class);

        enviada();
        questao.apurarVotos(3, 0);
        assertThatThrownBy(() -> questao.apurarVotos(0, 3))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("encerrada");
        assertThat(questao.getStatus()).isEqualTo(StatusQuestao.APROVADA);
    }

    @Test
    void editaTodosOsCamposDoRascunho() {
        Topico porcentagem = new Topico("Porcentagem");

        questao.editar("Quanto é 10% de 50?", porcentagem, NivelDificuldade.FACIL, AnoEscolar.OITAVO_ANO,
                List.of("1", "5", "10", "15", "50"), 1);

        assertThat(questao.getEnunciado()).isEqualTo("Quanto é 10% de 50?");
        assertThat(questao.getTopico()).isSameAs(porcentagem);
        assertThat(questao.getNivelDificuldade()).isEqualTo(NivelDificuldade.FACIL);
        assertThat(questao.getAnoEscolar()).isEqualTo(AnoEscolar.OITAVO_ANO);
        assertThat(questao.alternativaCorreta().getTexto()).isEqualTo("5");
    }

    @Test
    void naoEditaDepoisDeEnviarParaValidacao() {
        enviada();

        assertThatThrownBy(() -> questao.editar("Outro", new Topico("Outro"), NivelDificuldade.DIFICIL,
                AnoEscolar.NONO_ANO, List.of("1", "2", "3", "4", "5"), 0))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("rascunho");
        assertThat(questao.getEnunciado()).isEqualTo("Quanto é 3/4 de 20?");
    }
}
