package br.com.maisedu.app.model;

import br.com.maisedu.app.exception.NegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RN04: a alternativa precisa ser da questão entregue; o acerto é derivado, não armazenado. */
class RespostaTest {

    @Test
    void acertoEDerivadoDaAlternativaEscolhida() {
        Questao questao = questaoComAlternativas(1L, 2);
        AtividadeAlunoQuestao entregue = entregue(questao);

        Resposta certa = new Resposta(entregue, questao.getAlternativas().get(2));
        Resposta errada = new Resposta(entregue, questao.getAlternativas().get(0));

        assertTrue(certa.acertou());
        assertFalse(errada.acertou());
    }

    @Test
    void alternativaDeOutraQuestaoERecusada() {
        Questao entregueAoAluno = questaoComAlternativas(1L, 0);
        Questao outra = questaoComAlternativas(2L, 0);

        NegocioException ex = assertThrows(NegocioException.class,
                () -> new Resposta(entregue(entregueAoAluno), outra.getAlternativas().get(0)));

        assertEquals("A alternativa escolhida não pertence a esta questão", ex.getMessage());
    }

    private static AtividadeAlunoQuestao entregue(Questao questao) {
        AtividadeAluno atividadeAluno = AtividadeAluno.adaptativa(null, List.of(questao));
        return atividadeAluno.getQuestoes().get(0);
    }

    private static Questao questaoComAlternativas(Long id, int indiceCorreta) {
        Questao questao = new Questao("Questão " + id, null, null, NivelDificuldade.FACIL, AnoEscolar.SEXTO_ANO);
        ReflectionTestUtils.setField(questao, "id", id);
        questao.definirAlternativas(List.of("a", "b", "c", "d", "e"), indiceCorreta);
        questao.setStatus(StatusQuestao.APROVADA);
        return questao;
    }
}
