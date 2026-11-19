package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.DesempenhoTurmaResponse.EfeitoPratica;
import br.com.maisedu.app.dto.DesempenhoTurmaResponse.PontoEvolucaoTurma;
import br.com.maisedu.app.model.TipoAtividade;
import br.com.maisedu.app.repository.Projecoes.RespostaDoAluno;
import br.com.maisedu.app.repository.Projecoes.RespostaPorTopico;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF19, visão da turma: evolução semanal e efeito da prática adaptativa (antes e depois). */
class EfeitoPraticaEEvolucaoDaTurmaTest {

    private static final LocalDateTime SEGUNDA = LocalDateTime.of(2026, 10, 5, 10, 0);

    @Test
    void evolucaoDaTurmaContaRespostasEAlunosDistintosPorSemana() {
        List<PontoEvolucaoTurma> pontos = DesempenhoService.evolucaoSemanalDaTurma(List.of(
                new RespostaDoAluno(1L, SEGUNDA, true),
                new RespostaDoAluno(2L, SEGUNDA.plusDays(2), false),
                new RespostaDoAluno(1L, SEGUNDA.plusDays(3), true),
                new RespostaDoAluno(2L, SEGUNDA.plusDays(8), true)));

        assertEquals(2, pontos.size());
        assertEquals(LocalDate.of(2026, 10, 5), pontos.get(0).semana());
        assertEquals(3, pontos.get(0).respostas());
        assertEquals(2, pontos.get(0).alunosAtivos());
        assertEquals(0, new BigDecimal("66.67").compareTo(pontos.get(0).percentualSemana()));
        assertEquals(1, pontos.get(1).alunosAtivos());
        assertEquals(0, new BigDecimal("75.00").compareTo(pontos.get(1).percentualAcumulado()));
    }

    @Test
    void efeitoDaPraticaComparaAsAtividadesAntesEDepoisDaPrimeiraPraticaNoTopico() {
        List<EfeitoPratica> efeito = DesempenhoService.efeitoDaPratica(List.of(
                // aluno 1: errou 2 de 2 antes, praticou, acertou 2 de 2 depois
                regular(1L, SEGUNDA, false),
                regular(1L, SEGUNDA.plusDays(1), false),
                new RespostaPorTopico(1L, 3L, "Frações", SEGUNDA.plusDays(2), false, TipoAtividade.ADAPTATIVA),
                regular(1L, SEGUNDA.plusDays(5), true),
                regular(1L, SEGUNDA.plusDays(6), true),
                // aluno 2: só tem respostas antes da prática; não entra na comparação
                regular(2L, SEGUNDA, true),
                new RespostaPorTopico(2L, 3L, "Frações", SEGUNDA.plusDays(2), true, TipoAtividade.ADAPTATIVA),
                // aluno 3: nunca praticou; não entra
                regular(3L, SEGUNDA, true),
                regular(3L, SEGUNDA.plusDays(6), false)));

        assertEquals(1, efeito.size());
        EfeitoPratica fracoes = efeito.get(0);
        assertEquals("Frações", fracoes.topico());
        assertEquals(1, fracoes.alunos());
        assertEquals(2, fracoes.respostasAntes());
        assertEquals(0, BigDecimal.ZERO.compareTo(fracoes.percentualAntes()));
        assertEquals(2, fracoes.respostasDepois());
        assertEquals(0, new BigDecimal("100.00").compareTo(fracoes.percentualDepois()));
    }

    @Test
    void semPraticaNaoHaEfeito() {
        assertTrue(DesempenhoService.efeitoDaPratica(List.of(regular(1L, SEGUNDA, true))).isEmpty());
    }

    private static RespostaPorTopico regular(Long alunoId, LocalDateTime quando, boolean acertou) {
        return new RespostaPorTopico(alunoId, 3L, "Frações", quando, acertou, TipoAtividade.REGULAR);
    }
}
