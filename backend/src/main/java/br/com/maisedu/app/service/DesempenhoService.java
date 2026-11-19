package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.DesempenhoAlunoResponse;
import br.com.maisedu.app.dto.DesempenhoTurmaResponse;
import br.com.maisedu.app.dto.RespostaHistorico;
import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
import br.com.maisedu.app.model.AlunoTurma;
import br.com.maisedu.app.model.DesempenhoTopico;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.TipoAtividade;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlunoTurmaRepository;
import br.com.maisedu.app.repository.DesempenhoTopicoRepository;
import br.com.maisedu.app.repository.Projecoes;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.time.LocalDateTime;
import java.util.TreeMap;

/**
 * RN05 (atualização do desempenho acumulado) e RF19 (acompanhamento pelo aluno e pelo professor).
 * O desempenho só recebe respostas contabilizadas: as da prática adaptativa no envio e as das
 * atividades regulares no fechamento do prazo.
 */
@Service
public class DesempenhoService {

    private final DesempenhoTopicoRepository desempenhoTopicoRepository;
    private final RespostaRepository respostaRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AcessoTurma acessoTurma;
    private final MotorAdaptativo motorAdaptativo;

    public DesempenhoService(DesempenhoTopicoRepository desempenhoTopicoRepository,
                             RespostaRepository respostaRepository,
                             AlunoTurmaRepository alunoTurmaRepository,
                             UsuarioRepository usuarioRepository,
                             AcessoTurma acessoTurma,
                             MotorAdaptativo motorAdaptativo) {
        this.desempenhoTopicoRepository = desempenhoTopicoRepository;
        this.respostaRepository = respostaRepository;
        this.alunoTurmaRepository = alunoTurmaRepository;
        this.usuarioRepository = usuarioRepository;
        this.acessoTurma = acessoTurma;
        this.motorAdaptativo = motorAdaptativo;
    }

    /**
     * RN05: localiza (ou cria) o registro do aluno no tópico e nível da questão e soma a resposta.
     * Deve ser chamado na mesma transação que marca a resposta como contabilizada (envio da prática ou
     * fechamento da atividade), com a linha do aluno travada, para não perder incrementos.
     */
    @Transactional
    public DesempenhoTopico registrarResposta(Usuario aluno, Questao questao, boolean acertou) {
        DesempenhoTopico registro = desempenhoTopicoRepository
                .buscar(aluno.getId(), questao.getTopico().getId(), questao.getNivelDificuldade())
                .orElseGet(() -> new DesempenhoTopico(aluno, questao.getTopico(), questao.getNivelDificuldade()));
        registro.registrarResposta(acertou);
        return desempenhoTopicoRepository.save(registro);
    }

    /** Painel do próprio aluno (RF19, CA01: só os próprios dados). */
    @Transactional(readOnly = true)
    public DesempenhoAlunoResponse consultarDoAluno(Long alunoId) {
        Usuario aluno = usuarioRepository.findById(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
        return montarPainel(aluno);
    }

    /** Painel de um aluno visto pelo professor da turma (RF19, CA02) ou pelo admin da instituição. */
    @Transactional(readOnly = true)
    public DesempenhoAlunoResponse consultarAlunoDaTurma(UsuarioAutenticado ator, Long turmaId, Long alunoId) {
        acessoTurma.buscarComAcesso(ator, turmaId);
        if (!alunoTurmaRepository.existsByAlunoIdAndTurmaId(alunoId, turmaId)) {
            throw new AccessDeniedException("Este aluno não pertence à turma.");
        }
        Usuario aluno = usuarioRepository.findById(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
        return montarPainel(aluno);
    }

    /** Visão da turma: resumo por aluno e por tópico, destacando os de menor desempenho. */
    @Transactional(readOnly = true)
    public DesempenhoTurmaResponse consultarTurma(UsuarioAutenticado ator, Long turmaId) {
        Turma turma = acessoTurma.buscarComAcesso(ator, turmaId);
        List<AlunoTurma> vinculos = alunoTurmaRepository.findByTurmaIdComAluno(turmaId);

        Set<Long> alunoIds = new HashSet<>();
        for (AlunoTurma vinculo : vinculos) {
            alunoIds.add(vinculo.getAluno().getId());
        }
        Map<Long, List<DesempenhoTopico>> porAluno = new HashMap<>();
        if (!alunoIds.isEmpty()) {
            for (DesempenhoTopico registro : desempenhoTopicoRepository.findByAlunoIdIn(alunoIds)) {
                porAluno.computeIfAbsent(registro.getAluno().getId(), id -> new ArrayList<>()).add(registro);
            }
        }

        List<DesempenhoTurmaResponse.AlunoResumo> alunos = new ArrayList<>();
        Map<Long, int[]> somaPorTopico = new LinkedHashMap<>();
        Map<Long, String> nomeDoTopico = new HashMap<>();
        Map<Long, Set<Long>> alunosPorTopico = new HashMap<>();

        for (AlunoTurma vinculo : vinculos) {
            Usuario aluno = vinculo.getAluno();
            List<DesempenhoTopico> registros = porAluno.getOrDefault(aluno.getId(), List.of());
            List<DesempenhoAlunoResponse.TopicoDesempenho> topicos = agruparPorTopico(registros);

            int respostas = 0;
            int acertos = 0;
            for (DesempenhoTopico registro : registros) {
                respostas += registro.getTotalRespostas();
                acertos += registro.getTotalAcertos();
                Long topicoId = registro.getTopico().getId();
                int[] soma = somaPorTopico.computeIfAbsent(topicoId, id -> new int[2]);
                soma[0] += registro.getTotalRespostas();
                soma[1] += registro.getTotalAcertos();
                nomeDoTopico.put(topicoId, registro.getTopico().getNome());
                alunosPorTopico.computeIfAbsent(topicoId, id -> new HashSet<>()).add(aluno.getId());
            }

            String topicoMaisFraco = topicos.isEmpty() ? null : topicos.get(0).topico();
            BigDecimal percentualMaisFraco = topicos.isEmpty() ? null : topicos.get(0).percentual();
            alunos.add(new DesempenhoTurmaResponse.AlunoResumo(aluno.getId(), aluno.getNome(), respostas, acertos,
                    DesempenhoTopico.percentual(acertos, respostas), topicoMaisFraco, percentualMaisFraco));
        }

        List<DesempenhoTurmaResponse.TopicoTurma> topicosDaTurma = new ArrayList<>();
        for (Map.Entry<Long, int[]> entrada : somaPorTopico.entrySet()) {
            int[] soma = entrada.getValue();
            topicosDaTurma.add(new DesempenhoTurmaResponse.TopicoTurma(entrada.getKey(),
                    nomeDoTopico.get(entrada.getKey()), soma[0], soma[1], DesempenhoTopico.percentual(soma[1], soma[0]),
                    alunosPorTopico.get(entrada.getKey()).size()));
        }
        topicosDaTurma.sort(Comparator.comparing(DesempenhoTurmaResponse.TopicoTurma::percentual));

        // mapa de calor: o aluno em cada tópico, somando os níveis
        List<DesempenhoTurmaResponse.Celula> celulas = new ArrayList<>();
        for (Map.Entry<Long, List<DesempenhoTopico>> entrada : porAluno.entrySet()) {
            Map<Long, int[]> porTopico = new LinkedHashMap<>();
            for (DesempenhoTopico registro : entrada.getValue()) {
                int[] soma = porTopico.computeIfAbsent(registro.getTopico().getId(), id -> new int[2]);
                soma[0] += registro.getTotalRespostas();
                soma[1] += registro.getTotalAcertos();
            }
            for (Map.Entry<Long, int[]> topico : porTopico.entrySet()) {
                int[] soma = topico.getValue();
                celulas.add(new DesempenhoTurmaResponse.Celula(entrada.getKey(), topico.getKey(), soma[0], soma[1],
                        DesempenhoTopico.percentual(soma[1], soma[0])));
            }
        }

        List<DesempenhoTurmaResponse.PontoEvolucaoTurma> evolucao = List.of();
        List<DesempenhoTurmaResponse.EfeitoPratica> efeito = List.of();
        if (!alunoIds.isEmpty()) {
            evolucao = evolucaoSemanalDaTurma(respostaRepository.findHistoricoDosAlunos(alunoIds));
            efeito = efeitoDaPratica(respostaRepository.findPorTopicoDosAlunos(alunoIds));
        }

        return new DesempenhoTurmaResponse(turma.getId(), turma.getNome(), turma.getAnoEscolar().name(), alunos,
                topicosDaTurma, celulas, evolucao, efeito);
    }

    /** Evolução semanal da turma: respostas, alunos que responderam e percentuais da semana e acumulado. */
    static List<DesempenhoTurmaResponse.PontoEvolucaoTurma> evolucaoSemanalDaTurma(
            List<Projecoes.RespostaDoAluno> historico) {
        Map<LocalDate, int[]> porSemana = new TreeMap<>();
        Map<LocalDate, Set<Long>> alunosPorSemana = new HashMap<>();
        for (Projecoes.RespostaDoAluno resposta : historico) {
            LocalDate semana = resposta.respondidaEm().toLocalDate()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            int[] contagem = porSemana.computeIfAbsent(semana, s -> new int[2]);
            contagem[0]++;
            if (resposta.acertou()) {
                contagem[1]++;
            }
            alunosPorSemana.computeIfAbsent(semana, s -> new HashSet<>()).add(resposta.alunoId());
        }

        List<DesempenhoTurmaResponse.PontoEvolucaoTurma> pontos = new ArrayList<>();
        int respostasAcumuladas = 0;
        int acertosAcumulados = 0;
        for (Map.Entry<LocalDate, int[]> entrada : porSemana.entrySet()) {
            int[] contagem = entrada.getValue();
            respostasAcumuladas += contagem[0];
            acertosAcumulados += contagem[1];
            pontos.add(new DesempenhoTurmaResponse.PontoEvolucaoTurma(entrada.getKey(), contagem[0], contagem[1],
                    alunosPorSemana.get(entrada.getKey()).size(),
                    DesempenhoTopico.percentual(contagem[1], contagem[0]),
                    DesempenhoTopico.percentual(acertosAcumulados, respostasAcumuladas)));
        }
        return pontos;
    }

    /**
     * Efeito da prática adaptativa: para cada aluno e tópico com prática, o acerto nas atividades
     * regulares do tópico antes e depois da primeira prática nele. As respostas da própria prática não
     * entram (ela mira o nível em que o aluno está, então compararia coisas diferentes). Só conta quem
     * tem respostas nos dois períodos. O histórico chega em ordem de data.
     */
    static List<DesempenhoTurmaResponse.EfeitoPratica> efeitoDaPratica(List<Projecoes.RespostaPorTopico> historico) {
        // primeira prática de cada aluno em cada tópico
        Map<String, LocalDateTime> primeiraPratica = new HashMap<>();
        for (Projecoes.RespostaPorTopico resposta : historico) {
            if (resposta.tipo() == TipoAtividade.ADAPTATIVA) {
                primeiraPratica.merge(resposta.alunoId() + "|" + resposta.topicoId(), resposta.respondidaEm(),
                        (atual, nova) -> nova.isBefore(atual) ? nova : atual);
            }
        }
        // [respostasAntes, acertosAntes, respostasDepois, acertosDepois] por aluno e tópico
        Map<String, int[]> porAlunoETopico = new HashMap<>();
        Map<Long, String> nomes = new HashMap<>();
        for (Projecoes.RespostaPorTopico resposta : historico) {
            String chave = resposta.alunoId() + "|" + resposta.topicoId();
            LocalDateTime marco = primeiraPratica.get(chave);
            if (marco == null || resposta.tipo() != TipoAtividade.REGULAR) {
                continue;
            }
            nomes.put(resposta.topicoId(), resposta.topico());
            int[] numeros = porAlunoETopico.computeIfAbsent(chave, c -> new int[4]);
            int deslocamento = resposta.respondidaEm().isBefore(marco) ? 0 : 2;
            numeros[deslocamento]++;
            if (resposta.acertou()) {
                numeros[deslocamento + 1]++;
            }
        }
        // soma por tópico só dos alunos com respostas antes e depois
        Map<Long, int[]> porTopico = new TreeMap<>();
        for (Map.Entry<String, int[]> entrada : porAlunoETopico.entrySet()) {
            int[] numeros = entrada.getValue();
            if (numeros[0] == 0 || numeros[2] == 0) {
                continue;
            }
            Long topicoId = Long.valueOf(entrada.getKey().substring(entrada.getKey().indexOf('|') + 1));
            int[] soma = porTopico.computeIfAbsent(topicoId, id -> new int[5]);
            soma[0]++;
            for (int i = 0; i < 4; i++) {
                soma[i + 1] += numeros[i];
            }
        }
        List<DesempenhoTurmaResponse.EfeitoPratica> efeito = new ArrayList<>();
        for (Map.Entry<Long, int[]> entrada : porTopico.entrySet()) {
            int[] soma = entrada.getValue();
            efeito.add(new DesempenhoTurmaResponse.EfeitoPratica(entrada.getKey(), nomes.get(entrada.getKey()),
                    soma[0], soma[1], DesempenhoTopico.percentual(soma[2], soma[1]), soma[3],
                    DesempenhoTopico.percentual(soma[4], soma[3])));
        }
        efeito.sort(Comparator.comparing(DesempenhoTurmaResponse.EfeitoPratica::topico));
        return efeito;
    }

    private DesempenhoAlunoResponse montarPainel(Usuario aluno) {
        List<DesempenhoTopico> registros = desempenhoTopicoRepository.findByAlunoId(aluno.getId());
        int respostas = 0;
        int acertos = 0;
        for (DesempenhoTopico registro : registros) {
            respostas += registro.getTotalRespostas();
            acertos += registro.getTotalAcertos();
        }
        return new DesempenhoAlunoResponse(aluno.getId(), aluno.getNome(), respostas, acertos,
                DesempenhoTopico.percentual(acertos, respostas), agruparPorTopico(registros),
                evolucaoSemanal(respostaRepository.findHistoricoDoAluno(aluno.getId())));
    }

    /** Agrupa os registros por tópico (com o detalhe por nível), do menor para o maior percentual. */
    private List<DesempenhoAlunoResponse.TopicoDesempenho> agruparPorTopico(List<DesempenhoTopico> registros) {
        Map<Long, Map<NivelDificuldade, DesempenhoTopico>> porTopico = new LinkedHashMap<>();
        Map<Long, String> nomes = new HashMap<>();
        for (DesempenhoTopico registro : registros) {
            Long topicoId = registro.getTopico().getId();
            porTopico.computeIfAbsent(topicoId, id -> new EnumMap<>(NivelDificuldade.class))
                    .put(registro.getNivelDificuldade(), registro);
            nomes.put(topicoId, registro.getTopico().getNome());
        }

        List<DesempenhoAlunoResponse.TopicoDesempenho> topicos = new ArrayList<>();
        for (Map.Entry<Long, Map<NivelDificuldade, DesempenhoTopico>> entrada : porTopico.entrySet()) {
            int respostas = 0;
            int acertos = 0;
            List<DesempenhoAlunoResponse.NivelDesempenho> niveis = new ArrayList<>();
            for (DesempenhoTopico registro : entrada.getValue().values()) {
                respostas += registro.getTotalRespostas();
                acertos += registro.getTotalAcertos();
                niveis.add(new DesempenhoAlunoResponse.NivelDesempenho(registro.getNivelDificuldade().name(),
                        registro.getTotalRespostas(), registro.getTotalAcertos(), registro.getPercentualAcerto(),
                        motorAdaptativo.dominado(new MotorAdaptativo.DesempenhoNivel(entrada.getKey(),
                                registro.getNivelDificuldade(), registro.getTotalRespostas(),
                                registro.getTotalAcertos()))));
            }
            topicos.add(new DesempenhoAlunoResponse.TopicoDesempenho(entrada.getKey(), nomes.get(entrada.getKey()),
                    respostas, acertos, DesempenhoTopico.percentual(acertos, respostas), niveis));
        }
        topicos.sort(Comparator.comparing(DesempenhoAlunoResponse.TopicoDesempenho::percentual)
                .thenComparing(DesempenhoAlunoResponse.TopicoDesempenho::topico));
        return topicos;
    }

    /** Evolução ao longo do tempo, calculada a partir das datas das respostas (semanas de segunda a domingo). */
    static List<DesempenhoAlunoResponse.PontoEvolucao> evolucaoSemanal(List<RespostaHistorico> historico) {
        Map<LocalDate, int[]> porSemana = new LinkedHashMap<>();
        for (RespostaHistorico resposta : historico) {
            LocalDate semana = resposta.respondidaEm().toLocalDate()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            int[] contagem = porSemana.computeIfAbsent(semana, s -> new int[2]);
            contagem[0]++;
            if (resposta.acertou()) {
                contagem[1]++;
            }
        }

        List<DesempenhoAlunoResponse.PontoEvolucao> pontos = new ArrayList<>();
        int respostasAcumuladas = 0;
        int acertosAcumulados = 0;
        for (Map.Entry<LocalDate, int[]> entrada : porSemana.entrySet()) {
            int[] contagem = entrada.getValue();
            respostasAcumuladas += contagem[0];
            acertosAcumulados += contagem[1];
            pontos.add(new DesempenhoAlunoResponse.PontoEvolucao(entrada.getKey(), contagem[0], contagem[1],
                    DesempenhoTopico.percentual(contagem[1], contagem[0]),
                    DesempenhoTopico.percentual(acertosAcumulados, respostasAcumuladas)));
        }
        return pontos;
    }
}
