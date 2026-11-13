package br.com.maisedu.app.service;

import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.service.MotorAdaptativo.Candidata;
import br.com.maisedu.app.service.MotorAdaptativo.DesempenhoNivel;
import br.com.maisedu.app.service.MotorAdaptativo.TopicoPlanejado;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static br.com.maisedu.app.model.NivelDificuldade.DIFICIL;
import static br.com.maisedu.app.model.NivelDificuldade.FACIL;
import static br.com.maisedu.app.model.NivelDificuldade.MEDIO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RN06 / RF18: regra determinística do motor adaptativo, sem Spring nem banco. */
class MotorAdaptativoTest {

    private static final long FRACOES = 3L;
    private static final long PORCENTAGEM = 6L;
    private static final long EQUACOES = 9L;
    private static final long ANGULOS = 12L;

    private final MotorAdaptativo motor = new MotorAdaptativo(MotorAdaptativo.Parametros.padrao());

    @Test
    void rf18Ca01_topicoComMenorPercentualTemPrioridade() {
        // Frações 40% (4 de 10), Porcentagem 80% (8 de 10)
        List<TopicoPlanejado> plano = motor.planejar(List.of(
                new DesempenhoNivel(PORCENTAGEM, FACIL, 10, 8),
                new DesempenhoNivel(FRACOES, FACIL, 10, 4)));

        assertEquals(2, plano.size());
        assertEquals(FRACOES, plano.get(0).topicoId());
        assertEquals(new BigDecimal("40.00"), plano.get(0).percentualTopico());
        assertEquals(PORCENTAGEM, plano.get(1).topicoId());
    }

    @Test
    void rf18Ca02_nivelFacilDominadoLevaAoNivelMedio() {
        // 6 respostas e 83% no fácil, sem histórico no médio
        List<TopicoPlanejado> plano = motor.planejar(List.of(new DesempenhoNivel(FRACOES, FACIL, 6, 5)));

        assertEquals(MEDIO, plano.get(0).nivelAlvo());
    }

    @Test
    void nivelSoEDominadoComOMinimoDeRespostas() {
        // 4 de 4 (100%) ainda não basta: são necessárias pelo menos 5 respostas
        List<TopicoPlanejado> plano = motor.planejar(List.of(new DesempenhoNivel(FRACOES, FACIL, 4, 4)));

        assertEquals(FACIL, plano.get(0).nivelAlvo());
    }

    @Test
    void nivelComPercentualAbaixoDoLimiteNaoEDominado() {
        // 3 de 5 = 60% < 70%: continua no fácil, mesmo com médio já respondido
        List<TopicoPlanejado> plano = motor.planejar(List.of(
                new DesempenhoNivel(FRACOES, FACIL, 5, 3),
                new DesempenhoNivel(FRACOES, MEDIO, 5, 5)));

        assertEquals(FACIL, plano.get(0).nivelAlvo());
    }

    @Test
    void exatamenteSetentaPorCentoContaComoDominado() {
        List<TopicoPlanejado> plano = motor.planejar(List.of(new DesempenhoNivel(FRACOES, FACIL, 10, 7)));

        assertEquals(MEDIO, plano.get(0).nivelAlvo());
    }

    @Test
    void todosOsNiveisDominadosUsaODificil() {
        List<TopicoPlanejado> plano = motor.planejar(List.of(
                new DesempenhoNivel(FRACOES, FACIL, 5, 5),
                new DesempenhoNivel(FRACOES, MEDIO, 6, 5),
                new DesempenhoNivel(FRACOES, DIFICIL, 10, 9)));

        assertEquals(DIFICIL, plano.get(0).nivelAlvo());
    }

    @Test
    void consideraNoMaximoTresTopicosEIgnoraTopicosSemRespostas() {
        List<TopicoPlanejado> plano = motor.planejar(List.of(
                new DesempenhoNivel(FRACOES, FACIL, 10, 2),
                new DesempenhoNivel(PORCENTAGEM, FACIL, 10, 5),
                new DesempenhoNivel(EQUACOES, FACIL, 10, 9),
                new DesempenhoNivel(ANGULOS, FACIL, 10, 1),
                new DesempenhoNivel(99L, FACIL, 0, 0)));

        assertEquals(3, plano.size());
        assertEquals(ANGULOS, plano.get(0).topicoId());
        assertEquals(FRACOES, plano.get(1).topicoId());
        assertEquals(PORCENTAGEM, plano.get(2).topicoId());
    }

    @Test
    void semHistoricoNaoHaPlano() {
        assertTrue(motor.planejar(List.of()).isEmpty());
    }

    @Test
    void selecionaDezQuestoesIntercalandoOsTopicosPorPrioridade() {
        List<TopicoPlanejado> plano = List.of(
                new TopicoPlanejado(FRACOES, new BigDecimal("40.00"), 10, FACIL),
                new TopicoPlanejado(PORCENTAGEM, new BigDecimal("60.00"), 10, FACIL),
                new TopicoPlanejado(EQUACOES, new BigDecimal("70.00"), 10, FACIL));
        List<Candidata> candidatas = new ArrayList<>();
        candidatas.addAll(candidatas(FRACOES, FACIL, 100, 6));
        candidatas.addAll(candidatas(PORCENTAGEM, FACIL, 200, 6));
        candidatas.addAll(candidatas(EQUACOES, FACIL, 300, 6));

        List<Long> selecionadas = motor.selecionarQuestoes(plano, candidatas, Set.of(), new Random(1));

        assertEquals(10, selecionadas.size());
        assertEquals(10, new HashSet<>(selecionadas).size());
        Map<Long, Integer> porTopico = contarPorTopico(selecionadas);
        // 10 vagas intercaladas a partir do tópico mais fraco: 4, 3, 3
        assertEquals(4, porTopico.get(FRACOES));
        assertEquals(3, porTopico.get(PORCENTAGEM));
        assertEquals(3, porTopico.get(EQUACOES));
        assertEquals(FRACOES, topicoDe(selecionadas.get(0)));
    }

    @Test
    void priorizaAsQuestoesQueOAlunoAindaNaoRespondeu() {
        List<TopicoPlanejado> plano = List.of(new TopicoPlanejado(FRACOES, new BigDecimal("40.00"), 10, FACIL));
        List<Candidata> candidatas = candidatas(FRACOES, FACIL, 100, 12);
        Set<Long> respondidas = Set.of(100L, 101L, 102L, 103L, 104L);

        List<Long> selecionadas = motor.selecionarQuestoes(plano, candidatas, respondidas, new Random(7));

        assertEquals(10, selecionadas.size());
        // As 7 não respondidas vêm primeiro; só depois entram 3 das já respondidas
        for (int i = 0; i < 7; i++) {
            assertFalse(respondidas.contains(selecionadas.get(i)), "posição " + i + " deveria ser inédita");
        }
        for (int i = 7; i < 10; i++) {
            assertTrue(respondidas.contains(selecionadas.get(i)), "posição " + i + " deveria ser repetida");
        }
    }

    @Test
    void completaComNiveisAdjacentesQuandoFaltamQuestoesNoNivelAlvo() {
        List<TopicoPlanejado> plano = List.of(new TopicoPlanejado(FRACOES, new BigDecimal("40.00"), 10, FACIL));
        List<Candidata> candidatas = new ArrayList<>();
        candidatas.addAll(candidatas(FRACOES, FACIL, 100, 3));
        candidatas.addAll(candidatas(FRACOES, MEDIO, 200, 3));
        candidatas.addAll(candidatas(FRACOES, DIFICIL, 300, 3));

        List<Long> selecionadas = motor.selecionarQuestoes(plano, candidatas, Set.of(), new Random(3));

        // Alvo FÁCIL: as 3 fáceis primeiro, depois o adjacente (MÉDIO); DIFÍCIL não é adjacente de FÁCIL
        assertEquals(6, selecionadas.size());
        for (int i = 0; i < 3; i++) {
            assertTrue(selecionadas.get(i) < 200, "as primeiras devem ser do nível-alvo");
        }
        for (Long id : selecionadas) {
            assertTrue(id < 300, "nível difícil não é adjacente ao fácil");
        }
    }

    @Test
    void vagasDeUmTopicoSemQuestoesFicamParaOsOutros() {
        List<TopicoPlanejado> plano = List.of(
                new TopicoPlanejado(FRACOES, new BigDecimal("10.00"), 10, FACIL),
                new TopicoPlanejado(PORCENTAGEM, new BigDecimal("50.00"), 10, FACIL));
        List<Candidata> candidatas = candidatas(PORCENTAGEM, FACIL, 200, 15);

        List<Long> selecionadas = motor.selecionarQuestoes(plano, candidatas, Set.of(), new Random(5));

        assertEquals(10, selecionadas.size());
        assertEquals(10, contarPorTopico(selecionadas).get(PORCENTAGEM));
    }

    @Test
    void rf18Ca03_semQuestoesCompativeisNadaESelecionado() {
        List<TopicoPlanejado> plano = List.of(new TopicoPlanejado(FRACOES, new BigDecimal("40.00"), 10, FACIL));

        List<Long> selecionadas = motor.selecionarQuestoes(plano, List.of(), Set.of(), new Random(1));

        assertTrue(selecionadas.isEmpty());
    }

    @Test
    void ordemDosNiveisAdjacentes() {
        assertEquals(List.of(FACIL, MEDIO), MotorAdaptativo.ordemDeNiveis(FACIL));
        assertEquals(List.of(MEDIO, FACIL, DIFICIL), MotorAdaptativo.ordemDeNiveis(MEDIO));
        assertEquals(List.of(DIFICIL, MEDIO), MotorAdaptativo.ordemDeNiveis(DIFICIL));
    }

    @Test
    void parametrosSaoConfiguraveis() {
        MotorAdaptativo exigente = new MotorAdaptativo(
                new MotorAdaptativo.Parametros(1, 4, 8, BigDecimal.valueOf(90)));

        List<TopicoPlanejado> plano = exigente.planejar(List.of(
                new DesempenhoNivel(FRACOES, FACIL, 6, 6),
                new DesempenhoNivel(PORCENTAGEM, FACIL, 6, 1)));

        // Só 1 tópico por prática: Porcentagem (1 de 6) é o mais fraco
        assertEquals(1, plano.size());
        assertEquals(PORCENTAGEM, plano.get(0).topicoId());
        assertEquals(FACIL, plano.get(0).nivelAlvo());
        // Com mínimo de 8 respostas, nem 6 de 6 domina o nível fácil
        assertFalse(exigente.dominado(new DesempenhoNivel(FRACOES, FACIL, 6, 6)));
        List<Long> selecionadas = exigente.selecionarQuestoes(plano, candidatas(PORCENTAGEM, FACIL, 200, 10),
                Set.of(), new Random(2));
        assertEquals(4, selecionadas.size());
    }

    // ---------- helpers ----------

    private static List<Candidata> candidatas(long topicoId, NivelDificuldade nivel, long primeiroId, int quantidade) {
        List<Candidata> lista = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            lista.add(new Candidata(primeiroId + i, topicoId, nivel));
        }
        return lista;
    }

    /** Nos testes, os ids de cada tópico ficam numa faixa própria: 1xx frações, 2xx porcentagem, 3xx equações. */
    private static long topicoDe(long questaoId) {
        if (questaoId < 200) {
            return FRACOES;
        }
        return questaoId < 300 ? PORCENTAGEM : EQUACOES;
    }

    private static Map<Long, Integer> contarPorTopico(List<Long> ids) {
        Map<Long, Integer> contagem = new HashMap<>();
        for (Long id : ids) {
            contagem.merge(topicoDe(id), 1, Integer::sum);
        }
        return contagem;
    }
}
