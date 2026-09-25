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
        questao.setStatus(StatusQuestao.APROVADA);
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
}
