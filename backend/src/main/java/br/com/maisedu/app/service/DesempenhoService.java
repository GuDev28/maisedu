package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.DesempenhoAlunoResponse;
import br.com.maisedu.app.dto.DesempenhoTurmaResponse;
import br.com.maisedu.app.dto.RespostaHistorico;
import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
import br.com.maisedu.app.model.AlunoTurma;
import br.com.maisedu.app.model.DesempenhoTopico;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlunoTurmaRepository;
import br.com.maisedu.app.repository.AtividadeRepository;
import br.com.maisedu.app.repository.DesempenhoTopicoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import org.springframework.beans.factory.annotation.Value;
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

/** RN05 (atualização do desempenho acumulado) e RF19 (acompanhamento pelo aluno e pelo professor). */
@Service
public class DesempenhoService {

    private final DesempenhoTopicoRepository desempenhoTopicoRepository;
    private final RespostaRepository respostaRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AtividadeRepository atividadeRepository;
    private final AcessoTurma acessoTurma;
    private final int minimoRespostasDominio;
    private final BigDecimal percentualDominio;

    public DesempenhoService(DesempenhoTopicoRepository desempenhoTopicoRepository,
                             RespostaRepository respostaRepository,
                             AlunoTurmaRepository alunoTurmaRepository,
                             UsuarioRepository usuarioRepository,
                             AtividadeRepository atividadeRepository,
                             AcessoTurma acessoTurma,
                             @Value("${maisedu.adaptativo.minimo-respostas-dominio:5}") int minimoRespostasDominio,
                             @Value("${maisedu.adaptativo.percentual-dominio:70}") BigDecimal percentualDominio) {
        this.desempenhoTopicoRepository = desempenhoTopicoRepository;
        this.respostaRepository = respostaRepository;
        this.alunoTurmaRepository = alunoTurmaRepository;
        this.usuarioRepository = usuarioRepository;
        this.atividadeRepository = atividadeRepository;
        this.acessoTurma = acessoTurma;
        this.minimoRespostasDominio = minimoRespostasDominio;
        this.percentualDominio = percentualDominio;
    }

    /**
     * RN05: localiza (ou cria) o registro do aluno no tópico e nível da questão e soma a resposta.
     * Deve ser chamado na mesma transação que grava a resposta (RF17, CA03), com a linha do aluno
     * travada, para que respostas simultâneas do mesmo aluno não percam incrementos.
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

    /**
     * Painel da turma (RF19): indicadores gerais, resumo por aluno, desempenho por tópico e nível (do mais
     * fraco para o mais forte) e evolução semanal. Tudo é calculado a partir dos mesmos registros de
     * desempenho dos alunos da turma (RN05), com o histórico completo de cada aluno.
     */
    @Transactional(readOnly = true)
    public DesempenhoTurmaResponse consultarTurma(UsuarioAutenticado ator, Long turmaId) {
        Turma turma = acessoTurma.buscarComAcesso(ator, turmaId);
        List<AlunoTurma> vinculos = alunoTurmaRepository.findByTurmaIdComAluno(turmaId);

        Set<Long> alunoIds = new HashSet<>();
        for (AlunoTurma vinculo : vinculos) {
            alunoIds.add(vinculo.getAluno().getId());
        }
        Map<Long, List<DesempenhoTopico>> porAluno = new HashMap<>();
        List<RespostaHistorico> historico = List.of();
        if (!alunoIds.isEmpty()) {
            for (DesempenhoTopico registro : desempenhoTopicoRepository.findByAlunoIdIn(alunoIds)) {
                porAluno.computeIfAbsent(registro.getAluno().getId(), id -> new ArrayList<>()).add(registro);
            }
            historico = respostaRepository.findHistoricoDosAlunos(alunoIds);
        }

        List<DesempenhoTurmaResponse.AlunoResumo> alunos = new ArrayList<>();
        Map<Long, Map<NivelDificuldade, int[]>> somaPorTopico = new LinkedHashMap<>();
        Map<Long, String> nomeDoTopico = new HashMap<>();
        Map<Long, Set<Long>> alunosPorTopico = new HashMap<>();
        int respostasDaTurma = 0;
        int acertosDaTurma = 0;
        int alunosComRespostas = 0;

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
                int[] soma = somaPorTopico
                        .computeIfAbsent(topicoId, id -> new EnumMap<>(NivelDificuldade.class))
                        .computeIfAbsent(registro.getNivelDificuldade(), nivel -> new int[2]);
                soma[0] += registro.getTotalRespostas();
                soma[1] += registro.getTotalAcertos();
                nomeDoTopico.put(topicoId, registro.getTopico().getNome());
                alunosPorTopico.computeIfAbsent(topicoId, id -> new HashSet<>()).add(aluno.getId());
            }
            respostasDaTurma += respostas;
            acertosDaTurma += acertos;
            if (respostas > 0) {
                alunosComRespostas++;
            }

            String topicoMaisFraco = topicos.isEmpty() ? null : topicos.get(0).topico();
            BigDecimal percentualMaisFraco = topicos.isEmpty() ? null : topicos.get(0).percentual();
            alunos.add(new DesempenhoTurmaResponse.AlunoResumo(aluno.getId(), aluno.getNome(), respostas, acertos,
                    DesempenhoTopico.percentual(acertos, respostas), topicoMaisFraco, percentualMaisFraco));
        }

        List<DesempenhoTurmaResponse.TopicoTurma> topicosDaTurma = new ArrayList<>();
        for (Map.Entry<Long, Map<NivelDificuldade, int[]>> entrada : somaPorTopico.entrySet()) {
            int respostas = 0;
            int acertos = 0;
            List<DesempenhoTurmaResponse.NivelTurma> niveis = new ArrayList<>();
            for (Map.Entry<NivelDificuldade, int[]> nivel : entrada.getValue().entrySet()) {
                int[] soma = nivel.getValue();
                respostas += soma[0];
                acertos += soma[1];
                niveis.add(new DesempenhoTurmaResponse.NivelTurma(nivel.getKey().name(), soma[0], soma[1],
                        DesempenhoTopico.percentual(soma[1], soma[0])));
            }
            topicosDaTurma.add(new DesempenhoTurmaResponse.TopicoTurma(entrada.getKey(),
                    nomeDoTopico.get(entrada.getKey()), respostas, acertos, DesempenhoTopico.percentual(acertos, respostas),
                    alunosPorTopico.get(entrada.getKey()).size(), niveis));
        }
        topicosDaTurma.sort(Comparator.comparing(DesempenhoTurmaResponse.TopicoTurma::percentual)
                .thenComparing(DesempenhoTurmaResponse.TopicoTurma::topico));

        DesempenhoTurmaResponse.ResumoTurma resumo = new DesempenhoTurmaResponse.ResumoTurma(vinculos.size(),
                alunosComRespostas, atividadeRepository.contarDaTurma(turmaId), respostasDaTurma, acertosDaTurma,
                DesempenhoTopico.percentual(acertosDaTurma, respostasDaTurma));

        return new DesempenhoTurmaResponse(turma.getId(), turma.getNome(), turma.getAnoEscolar().name(), resumo,
                alunos, topicosDaTurma, evolucaoSemanal(historico));
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
                        registro.dominado(minimoRespostasDominio, percentualDominio)));
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
