package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.DesempenhoAlunoResponse.PontoEvolucao;
import br.com.maisedu.app.dto.RespostaHistorico;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF19: evolução ao longo do tempo calculada a partir das datas das respostas. */
class EvolucaoDesempenhoTest {

    @Test
    void agrupaPorSemanaEAcumulaOPercentual() {
        // 2026-10-05 é segunda-feira
        List<RespostaHistorico> historico = List.of(
                new RespostaHistorico(LocalDateTime.of(2026, 10, 5, 10, 0), true),
                new RespostaHistorico(LocalDateTime.of(2026, 10, 7, 10, 0), false),
                new RespostaHistorico(LocalDateTime.of(2026, 10, 11, 23, 59), false),  // domingo, mesma semana
                new RespostaHistorico(LocalDateTime.of(2026, 10, 12, 8, 0), true),     // segunda seguinte
                new RespostaHistorico(LocalDateTime.of(2026, 10, 13, 8, 0), true));

        List<PontoEvolucao> pontos = DesempenhoService.evolucaoSemanal(historico);

        assertEquals(2, pontos.size());
        assertEquals(LocalDate.of(2026, 10, 5), pontos.get(0).semana());
        assertEquals(3, pontos.get(0).respostas());
        assertEquals(1, pontos.get(0).acertos());
        assertEquals(new BigDecimal("33.33"), pontos.get(0).percentualSemana());
        assertEquals(new BigDecimal("33.33"), pontos.get(0).percentualAcumulado());

        assertEquals(LocalDate.of(2026, 10, 12), pontos.get(1).semana());
        assertEquals(new BigDecimal("100.00"), pontos.get(1).percentualSemana());
        // 3 acertos em 5 respostas no total
        assertEquals(new BigDecimal("60.00"), pontos.get(1).percentualAcumulado());
    }

    @Test
    void semRespostasNaoHaPontos() {
        assertTrue(DesempenhoService.evolucaoSemanal(List.of()).isEmpty());
    }
}
