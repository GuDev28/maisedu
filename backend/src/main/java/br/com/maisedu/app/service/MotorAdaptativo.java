package br.com.maisedu.app.service;

import br.com.maisedu.app.model.DesempenhoTopico;
import br.com.maisedu.app.model.NivelDificuldade;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Regra determinística da prática adaptativa (RN06), sem dependência de Spring ou banco, para ser
 * verificável por testes unitários e substituível por um modelo estatístico (BKT/TRI) no futuro.
 *
 * 1. Considera só os tópicos em que o aluno já tem respostas e os ordena do menor para o maior
 *    percentual de acerto (somando os três níveis); fica com até {@code maximoTopicos}.
 * 2. Para cada tópico, o nível-alvo é o mais baixo ainda não dominado. Um nível é dominado com pelo
 *    menos {@code minimoRespostasDominio} respostas e percentual acumulado >= {@code percentualDominio}.
 *    Se os três estiverem dominados, usa DIFÍCIL.
 * 3. Seleciona até {@code questoesPorPratica} questões aprovadas: em cada tópico, primeiro as do
 *    nível-alvo e depois as dos níveis adjacentes; dentro de cada nível, as que o aluno ainda não
 *    respondeu vêm antes das já respondidas. Os tópicos são intercalados por prioridade (o mais fraco
 *    primeiro), e as vagas que um tópico não preencher ficam para os demais.
 */
public final class MotorAdaptativo {

    public record Parametros(int maximoTopicos, int questoesPorPratica, int minimoRespostasDominio,
                             BigDecimal percentualDominio) {

        public static Parametros padrao() {
            return new Parametros(3, 10, 5, BigDecimal.valueOf(70));
        }
    }

    /** Contadores acumulados de um aluno em um tópico e nível (espelho de DesempenhoTopico). */
    public record DesempenhoNivel(Long topicoId, NivelDificuldade nivel, int totalRespostas, int totalAcertos) {
    }

    /** Tópico escolhido para a prática, com o percentual que o priorizou e o nível-alvo definido. */
    public record TopicoPlanejado(Long topicoId, BigDecimal percentualTopico, int totalRespostas,
                                  NivelDificuldade nivelAlvo) {
    }

    /** Questão aprovada candidata (só os dados que a regra usa). */
    public record Candidata(Long questaoId, Long topicoId, NivelDificuldade nivel) {
    }

    private final Parametros parametros;

    public MotorAdaptativo(Parametros parametros) {
        this.parametros = parametros;
    }

    /** Passos 1 e 2: tópicos prioritários e nível-alvo de cada um. Lista vazia se não houver histórico. */
    public List<TopicoPlanejado> planejar(List<DesempenhoNivel> desempenho) {
        Map<Long, Map<NivelDificuldade, DesempenhoNivel>> porTopico = new LinkedHashMap<>();
        for (DesempenhoNivel registro : desempenho) {
            if (registro.totalRespostas() <= 0) {
                continue;
            }
            porTopico.computeIfAbsent(registro.topicoId(), id -> new EnumMap<>(NivelDificuldade.class))
                    .put(registro.nivel(), registro);
        }

        List<TopicoPlanejado> topicos = new ArrayList<>();
        for (Map.Entry<Long, Map<NivelDificuldade, DesempenhoNivel>> entrada : porTopico.entrySet()) {
            int respostas = 0;
            int acertos = 0;
            for (DesempenhoNivel registro : entrada.getValue().values()) {
                respostas += registro.totalRespostas();
                acertos += registro.totalAcertos();
            }
            topicos.add(new TopicoPlanejado(entrada.getKey(), DesempenhoTopico.percentual(acertos, respostas),
                    respostas, nivelAlvo(entrada.getValue())));
        }

        // Menor percentual primeiro; no empate, o tópico com mais respostas (evidência mais forte) e depois o id.
        topicos.sort(Comparator.comparing(TopicoPlanejado::percentualTopico)
                .thenComparing(Comparator.comparingInt(TopicoPlanejado::totalRespostas).reversed())
                .thenComparing(TopicoPlanejado::topicoId));

        if (topicos.size() > parametros.maximoTopicos()) {
            return new ArrayList<>(topicos.subList(0, parametros.maximoTopicos()));
        }
        return topicos;
    }

    /** O nível mais baixo ainda não dominado; DIFÍCIL se todos estiverem dominados. */
    public NivelDificuldade nivelAlvo(Map<NivelDificuldade, DesempenhoNivel> porNivel) {
        for (NivelDificuldade nivel : NivelDificuldade.values()) {
            DesempenhoNivel registro = porNivel.get(nivel);
            if (registro == null || !dominado(registro)) {
                return nivel;
            }
        }
        return NivelDificuldade.DIFICIL;
    }

    public boolean dominado(DesempenhoNivel registro) {
        if (registro.totalRespostas() < parametros.minimoRespostasDominio()) {
            return false;
        }
        BigDecimal percentual = DesempenhoTopico.percentual(registro.totalAcertos(), registro.totalRespostas());
        return percentual.compareTo(parametros.percentualDominio()) >= 0;
    }

    /** Passo 3: ids das questões da prática, na ordem em que serão apresentadas. */
    public List<Long> selecionarQuestoes(List<TopicoPlanejado> plano, List<Candidata> candidatas,
                                         Set<Long> jaRespondidas, Random random) {
        List<Deque<Long>> filas = new ArrayList<>();
        for (TopicoPlanejado topico : plano) {
            filas.add(montarFila(topico, candidatas, jaRespondidas, random));
        }

        List<Long> selecionadas = new ArrayList<>();
        boolean algumaFilaTemQuestao = true;
        while (selecionadas.size() < parametros.questoesPorPratica() && algumaFilaTemQuestao) {
            algumaFilaTemQuestao = false;
            for (Deque<Long> fila : filas) {
                if (selecionadas.size() >= parametros.questoesPorPratica()) {
                    break;
                }
                Long proxima = fila.pollFirst();
                if (proxima != null) {
                    selecionadas.add(proxima);
                    algumaFilaTemQuestao = true;
                }
            }
        }
        return selecionadas;
    }

    private Deque<Long> montarFila(TopicoPlanejado topico, List<Candidata> candidatas, Set<Long> jaRespondidas,
                                   Random random) {
        Deque<Long> fila = new ArrayDeque<>();
        for (NivelDificuldade nivel : ordemDeNiveis(topico.nivelAlvo())) {
            List<Long> naoRespondidas = new ArrayList<>();
            List<Long> respondidas = new ArrayList<>();
            for (Candidata candidata : candidatas) {
                if (candidata.topicoId().equals(topico.topicoId()) && candidata.nivel() == nivel) {
                    if (jaRespondidas.contains(candidata.questaoId())) {
                        respondidas.add(candidata.questaoId());
                    } else {
                        naoRespondidas.add(candidata.questaoId());
                    }
                }
            }
            Collections.shuffle(naoRespondidas, random);
            Collections.shuffle(respondidas, random);
            fila.addAll(naoRespondidas);
            fila.addAll(respondidas);
        }
        return fila;
    }

    /** O nível-alvo primeiro e, em seguida, os adjacentes (o mais fácil antes do mais difícil). */
    public static List<NivelDificuldade> ordemDeNiveis(NivelDificuldade alvo) {
        return switch (alvo) {
            case FACIL -> List.of(NivelDificuldade.FACIL, NivelDificuldade.MEDIO);
            case MEDIO -> List.of(NivelDificuldade.MEDIO, NivelDificuldade.FACIL, NivelDificuldade.DIFICIL);
            case DIFICIL -> List.of(NivelDificuldade.DIFICIL, NivelDificuldade.MEDIO);
        };
    }
}
