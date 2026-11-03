package br.com.maisedu.app.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RN05 / RF17: contadores acumulados e percentual de acerto por aluno, tópico e nível. */
class DesempenhoTopicoTest {

    @Test
    void rf17Ca01_primeiroAcertoRegistraUmaRespostaUmAcertoE100PorCento() {
        DesempenhoTopico registro = new DesempenhoTopico(null, new Topico("Frações"), NivelDificuldade.FACIL);

        registro.registrarResposta(true);

        assertEquals(1, registro.getTotalRespostas());
        assertEquals(1, registro.getTotalAcertos());
        assertEquals(new BigDecimal("100.00"), registro.getPercentualAcerto());
    }

    @Test
    void rf17Ca02_tresAcertosEmQuatroMaisUmErroVira60PorCento() {
        DesempenhoTopico registro = new DesempenhoTopico(null, new Topico("Frações"), NivelDificuldade.FACIL);
        registro.registrarResposta(true);
        registro.registrarResposta(true);
        registro.registrarResposta(true);
        registro.registrarResposta(false);
        assertEquals(new BigDecimal("75.00"), registro.getPercentualAcerto());

        registro.registrarResposta(false);

        assertEquals(5, registro.getTotalRespostas());
        assertEquals(3, registro.getTotalAcertos());
        assertEquals(new BigDecimal("60.00"), registro.getPercentualAcerto());
    }

    @Test
    void percentualComDuasCasasArredondadoParaCima() {
        assertEquals(new BigDecimal("66.67"), DesempenhoTopico.percentual(2, 3));
        assertEquals(new BigDecimal("33.33"), DesempenhoTopico.percentual(1, 3));
        assertEquals(new BigDecimal("0.00"), DesempenhoTopico.percentual(0, 0));
    }

    @Test
    void dominioExigeMinimoDeRespostasEPercentual() {
        DesempenhoTopico registro = new DesempenhoTopico(null, new Topico("Frações"), NivelDificuldade.FACIL);
        for (int i = 0; i < 4; i++) {
            registro.registrarResposta(true);
        }
        assertFalse(registro.dominado(5, BigDecimal.valueOf(70)), "4 respostas ainda não bastam");

        registro.registrarResposta(false);
        assertTrue(registro.dominado(5, BigDecimal.valueOf(70)), "4 de 5 = 80% domina");
    }
}
