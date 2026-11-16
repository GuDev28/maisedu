package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.PainelPraticaResponse;
import br.com.maisedu.app.dto.PraticaAdaptativaResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.AlunoTurma;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.AtividadeAluno;
import br.com.maisedu.app.model.AtividadeAlunoQuestao;
import br.com.maisedu.app.model.DesempenhoTopico;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.StatusQuestao;
import br.com.maisedu.app.model.TipoAtividade;
import br.com.maisedu.app.model.Topico;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlunoTurmaRepository;
import br.com.maisedu.app.repository.AtividadeAlunoRepository;
import br.com.maisedu.app.repository.DesempenhoTopicoRepository;
import br.com.maisedu.app.repository.Projecoes;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.TopicoRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * RF18: gera a prática adaptativa do aluno a partir do desempenho acumulado (RN05/RN06), seja a
 * recomendada (o motor escolhe os tópicos mais fracos) ou a de um tópico escolhido pelo aluno.
 *
 * Ano escolar: as questões vêm do ano das turmas ativas do aluno. Se um tópico que ele precisa praticar
 * não tem nenhuma questão desse ano (por exemplo, o professor usou numa atividade uma questão de outro
 * ano), entram as questões desse tópico de qualquer ano, em vez de a prática sair vazia.
 */
@Service
public class PraticaAdaptativaService {

    private final UsuarioRepository usuarioRepository;
    private final DesempenhoTopicoRepository desempenhoTopicoRepository;
    private final QuestaoRepository questaoRepository;
    private final RespostaRepository respostaRepository;
    private final AtividadeAlunoRepository atividadeAlunoRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final TopicoRepository topicoRepository;
    private final AuditoriaService auditoriaService;
    private final MotorAdaptativo motor;
    private final Random random = new Random();

    public PraticaAdaptativaService(UsuarioRepository usuarioRepository,
                                    DesempenhoTopicoRepository desempenhoTopicoRepository,
                                    QuestaoRepository questaoRepository,
                                    RespostaRepository respostaRepository,
                                    AtividadeAlunoRepository atividadeAlunoRepository,
                                    AlunoTurmaRepository alunoTurmaRepository,
                                    TopicoRepository topicoRepository,
                                    AuditoriaService auditoriaService,
                                    MotorAdaptativo motor) {
        this.usuarioRepository = usuarioRepository;
        this.desempenhoTopicoRepository = desempenhoTopicoRepository;
        this.questaoRepository = questaoRepository;
        this.respostaRepository = respostaRepository;
        this.atividadeAlunoRepository = atividadeAlunoRepository;
        this.alunoTurmaRepository = alunoTurmaRepository;
        this.topicoRepository = topicoRepository;
        this.auditoriaService = auditoriaService;
        this.motor = motor;
    }

    /** Prática recomendada pelo motor (como antes da prática por tópico). */
    @Transactional
    public PraticaAdaptativaResponse gerarOuRetomar(Long alunoId) {
        return gerarOuRetomar(alunoId, null);
    }

    /**
     * Gera uma prática nova ou, se já houver uma em aberto do mesmo tipo (a recomendada, ou a do mesmo
     * tópico), retoma essa: o aluno não acumula práticas abertas nem descarta uma prática difícil
     * gerando outra. Com {@code topicoId}, a prática é só desse tópico.
     */
    @Transactional
    public PraticaAdaptativaResponse gerarOuRetomar(Long alunoId, Long topicoId) {
        Usuario aluno = usuarioRepository.findByIdParaAtualizacao(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
        if (!aluno.possuiRole(RoleNome.ALUNO)) {
            throw new AccessDeniedException("Só alunos geram práticas adaptativas.");
        }

        for (AtividadeAluno pratica : atividadeAlunoRepository.findByAlunoIdAndTipo(alunoId, TipoAtividade.ADAPTATIVA)) {
            if (!pratica.isConcluida() && Objects.equals(idDoTopico(pratica), topicoId)) {
                return resumir(pratica, false, false);
            }
        }

        List<MotorAdaptativo.DesempenhoNivel> desempenho = desempenhoDoAluno(alunoId);
        Set<AnoEscolar> anos = anosDoAluno(alunoId);
        return topicoId == null
                ? gerarRecomendada(aluno, desempenho, anos)
                : gerarDoTopico(aluno, topicoId, desempenho, anos);
    }

    private PraticaAdaptativaResponse gerarRecomendada(Usuario aluno, List<MotorAdaptativo.DesempenhoNivel> desempenho,
                                                       Set<AnoEscolar> anos) {
        List<MotorAdaptativo.TopicoPlanejado> plano = motor.planejar(desempenho);
        Map<Long, Questao> questoesPorId = new HashMap<>();
        List<MotorAdaptativo.Candidata> candidatas = new ArrayList<>();
        boolean diagnostica = plano.isEmpty();
        if (diagnostica) {
            // Sem nenhuma resposta ainda: prática diagnóstica com questões fáceis de vários tópicos do
            // ano escolar do aluno, em vez de mandá-lo esperar uma atividade da turma.
            Map<Long, List<Questao>> porTopico = new LinkedHashMap<>();
            for (Questao questao : aprovadasDosAnos(anos)) {
                porTopico.computeIfAbsent(questao.getTopico().getId(), id -> new ArrayList<>()).add(questao);
            }
            List<Long> topicoIds = new ArrayList<>(porTopico.keySet());
            Collections.shuffle(topicoIds, random);
            plano = motor.planejarDiagnostico(topicoIds);
            for (MotorAdaptativo.TopicoPlanejado topico : plano) {
                adicionarCandidatas(porTopico.get(topico.topicoId()), questoesPorId, candidatas);
            }
        } else {
            List<Long> topicoIds = new ArrayList<>();
            for (MotorAdaptativo.TopicoPlanejado topico : plano) {
                topicoIds.add(topico.topicoId());
            }
            adicionarCandidatas(doAnoOuDoTopico(aprovadasDosTopicos(topicoIds), anos), questoesPorId, candidatas);
        }

        Set<Long> jaRespondidas = new HashSet<>(respostaRepository.findQuestoesRespondidasPeloAluno(aluno.getId()));
        List<Long> selecionadas = motor.selecionarQuestoes(plano, candidatas, jaRespondidas, random);
        if (selecionadas.isEmpty()) {
            // RF18/CA03: sem questão aprovada compatível, a prática não é gerada e o aluno é avisado.
            throw new NegocioException(diagnostica
                    ? "Ainda não há questões aprovadas no banco para o seu ano escolar. Avise o seu professor."
                    : "Ainda não há questões aprovadas no banco para os tópicos em que você "
                    + "precisa praticar. Avise o seu professor.");
        }

        AtividadeAluno pratica = atividadeAlunoRepository.save(
                AtividadeAluno.adaptativa(aluno, null, emOrdem(selecionadas, questoesPorId)));
        registrarGeracao(aluno, pratica, plano, (diagnostica ? "diagnóstica: " : ""));
        return resumir(pratica, true, diagnostica);
    }

    private PraticaAdaptativaResponse gerarDoTopico(Usuario aluno, Long topicoId,
                                                    List<MotorAdaptativo.DesempenhoNivel> desempenho,
                                                    Set<AnoEscolar> anos) {
        Topico topico = topicoRepository.findById(topicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tópico não encontrado."));
        MotorAdaptativo.TopicoPlanejado planejado = motor.planejarTopico(topicoId, desempenho);

        List<Questao> doTopico = aprovadasDosTopicos(List.of(topicoId));
        List<Questao> doAno = doAno(doTopico, anos);
        if (doAno.isEmpty() && planejado.totalRespostas() == 0) {
            // sem questão do ano dele e sem nunca ter respondido o tópico: não é um tópico da série dele
            throw new NegocioException("Ainda não há questões aprovadas de " + topico.getNome()
                    + " para o seu ano escolar.");
        }
        List<Questao> questoes = doAno.isEmpty() ? doTopico : doAno;

        Map<Long, Questao> questoesPorId = new HashMap<>();
        List<MotorAdaptativo.Candidata> candidatas = new ArrayList<>();
        adicionarCandidatas(questoes, questoesPorId, candidatas);
        Set<Long> jaRespondidas = new HashSet<>(respostaRepository.findQuestoesRespondidasPeloAluno(aluno.getId()));
        List<Long> selecionadas = motor.selecionarQuestoes(List.of(planejado), candidatas, jaRespondidas, random,
                motor.parametros().questoesPorPraticaTopico());
        if (selecionadas.isEmpty()) {
            throw new NegocioException("Ainda não há questões aprovadas de " + topico.getNome()
                    + " no nível em que você está. Avise o seu professor.");
        }

        AtividadeAluno pratica = atividadeAlunoRepository.save(
                AtividadeAluno.adaptativa(aluno, topico, emOrdem(selecionadas, questoesPorId)));
        registrarGeracao(aluno, pratica, List.of(planejado), "escolhida pelo aluno: ");
        return resumir(pratica, true, false);
    }

    /**
     * Tela "Praticar": a recomendada (em andamento, ou o que o motor escolheria agora) e um card por
     * tópico. Os tópicos são os que têm questões aprovadas do ano escolar do aluno, mais os que ele já
     * respondeu (que podem ter vindo de questões de outro ano usadas pelo professor).
     */
    @Transactional(readOnly = true)
    public PainelPraticaResponse painel(Long alunoId) {
        List<MotorAdaptativo.DesempenhoNivel> desempenho = desempenhoDoAluno(alunoId);
        Set<AnoEscolar> anos = anosDoAluno(alunoId);

        Map<Long, Long> abertas = new HashMap<>();
        Long recomendadaAberta = null;
        for (AtividadeAluno pratica : atividadeAlunoRepository.findAbertasPorTipo(alunoId, TipoAtividade.ADAPTATIVA)) {
            if (pratica.getTopico() == null) {
                if (recomendadaAberta == null) {
                    recomendadaAberta = pratica.getId();
                }
            } else {
                abertas.putIfAbsent(pratica.getTopico().getId(), pratica.getId());
            }
        }

        // tópicos com questões do ano do aluno (ou de todos os anos, para aluno sem turma)
        Map<Long, String> nomes = new TreeMap<>();
        Map<Long, Long> questoesPorTopico = new HashMap<>();
        List<Projecoes.ContagemTopico> doAno = anos.isEmpty()
                ? questaoRepository.contarPorTopico(StatusQuestao.APROVADA)
                : questaoRepository.contarPorTopicoNosAnos(StatusQuestao.APROVADA, anos);
        for (Projecoes.ContagemTopico contagem : doAno) {
            nomes.put(contagem.topicoId(), contagem.topico());
            questoesPorTopico.put(contagem.topicoId(), contagem.total());
        }
        // tópicos que o aluno já respondeu, com as questões de qualquer ano (reserva quando falta do ano dele)
        Map<Long, String> nomesComHistorico = new HashMap<>();
        for (DesempenhoTopico registro : desempenhoTopicoRepository.findByAlunoId(alunoId)) {
            nomesComHistorico.put(registro.getTopico().getId(), registro.getTopico().getNome());
        }
        if (!nomesComHistorico.isEmpty()) {
            Map<Long, Long> todosOsAnos = new HashMap<>();
            for (Projecoes.ContagemTopico contagem : questaoRepository.contarPorTopico(StatusQuestao.APROVADA)) {
                todosOsAnos.put(contagem.topicoId(), contagem.total());
            }
            for (Map.Entry<Long, String> topico : nomesComHistorico.entrySet()) {
                if (!questoesPorTopico.containsKey(topico.getKey()) && todosOsAnos.containsKey(topico.getKey())) {
                    nomes.put(topico.getKey(), topico.getValue());
                    questoesPorTopico.put(topico.getKey(), todosOsAnos.get(topico.getKey()));
                }
            }
        }

        List<MotorAdaptativo.TopicoPlanejado> plano = motor.planejar(desempenho);
        Set<Long> recomendados = new HashSet<>();
        List<PainelPraticaResponse.TopicoPlanejado> planejados = new ArrayList<>();
        for (MotorAdaptativo.TopicoPlanejado topico : plano) {
            recomendados.add(topico.topicoId());
            String nome = nomes.getOrDefault(topico.topicoId(), nomesComHistorico.get(topico.topicoId()));
            planejados.add(new PainelPraticaResponse.TopicoPlanejado(topico.topicoId(), nome, topico.percentualTopico(),
                    topico.nivelAlvo().name()));
        }

        List<PainelPraticaResponse.TopicoParaPraticar> cards = new ArrayList<>();
        for (Map.Entry<Long, String> topico : nomes.entrySet()) {
            Long topicoId = topico.getKey();
            MotorAdaptativo.TopicoPlanejado planejado = motor.planejarTopico(topicoId, desempenho);
            cards.add(new PainelPraticaResponse.TopicoParaPraticar(topicoId, topico.getValue(),
                    questoesPorTopico.getOrDefault(topicoId, 0L).intValue(), planejado.totalRespostas(),
                    planejado.totalRespostas() == 0 ? null : planejado.percentualTopico(),
                    planejado.nivelAlvo().name(), niveis(topicoId, desempenho), abertas.get(topicoId),
                    recomendados.contains(topicoId)));
        }
        cards.sort((a, b) -> a.topico().compareToIgnoreCase(b.topico()));

        return new PainelPraticaResponse(
                new PainelPraticaResponse.Recomendada(recomendadaAberta, plano.isEmpty(), planejados), cards);
    }

    // ------------------------------------------------------------------ apoio

    private List<PainelPraticaResponse.Nivel> niveis(Long topicoId, List<MotorAdaptativo.DesempenhoNivel> desempenho) {
        Map<NivelDificuldade, MotorAdaptativo.DesempenhoNivel> porNivel = new EnumMap<>(NivelDificuldade.class);
        for (MotorAdaptativo.DesempenhoNivel registro : desempenho) {
            if (registro.topicoId().equals(topicoId)) {
                porNivel.put(registro.nivel(), registro);
            }
        }
        List<PainelPraticaResponse.Nivel> niveis = new ArrayList<>();
        for (NivelDificuldade nivel : NivelDificuldade.values()) {
            MotorAdaptativo.DesempenhoNivel registro = porNivel.get(nivel);
            if (registro == null) {
                niveis.add(new PainelPraticaResponse.Nivel(nivel.name(), 0, null, false));
            } else {
                niveis.add(new PainelPraticaResponse.Nivel(nivel.name(), registro.totalRespostas(),
                        DesempenhoTopico.percentual(registro.totalAcertos(), registro.totalRespostas()),
                        motor.dominado(registro)));
            }
        }
        return niveis;
    }

    private List<MotorAdaptativo.DesempenhoNivel> desempenhoDoAluno(Long alunoId) {
        List<MotorAdaptativo.DesempenhoNivel> desempenho = new ArrayList<>();
        for (DesempenhoTopico registro : desempenhoTopicoRepository.findByAlunoId(alunoId)) {
            desempenho.add(new MotorAdaptativo.DesempenhoNivel(registro.getTopico().getId(),
                    registro.getNivelDificuldade(), registro.getTotalRespostas(), registro.getTotalAcertos()));
        }
        return desempenho;
    }

    /** Anos escolares das turmas ativas do aluno (vazio se ele não tem turma ativa). */
    private Set<AnoEscolar> anosDoAluno(Long alunoId) {
        Set<AnoEscolar> anos = new HashSet<>();
        for (AlunoTurma vinculo : alunoTurmaRepository.findByAlunoIdComTurma(alunoId)) {
            if (vinculo.getTurma().isAtivo()) {
                anos.add(vinculo.getTurma().getAnoEscolar());
            }
        }
        return anos;
    }

    /** Questões aprovadas dos anos informados; de todos os anos se o aluno não tem turma. */
    private List<Questao> aprovadasDosAnos(Set<AnoEscolar> anos) {
        if (anos.isEmpty()) {
            return questaoRepository.buscarNoBanco(StatusQuestao.APROVADA, null, null, null);
        }
        List<Questao> questoes = new ArrayList<>();
        for (AnoEscolar ano : anos) {
            questoes.addAll(questaoRepository.buscarNoBanco(StatusQuestao.APROVADA, null, null, ano));
        }
        return questoes;
    }

    private List<Questao> aprovadasDosTopicos(List<Long> topicoIds) {
        if (topicoIds.isEmpty()) {
            return List.of();
        }
        return questaoRepository.findByStatusAndTopicoIdIn(StatusQuestao.APROVADA, topicoIds);
    }

    /** Só as questões dos anos do aluno (todas, se ele não tem turma). */
    private static List<Questao> doAno(List<Questao> questoes, Set<AnoEscolar> anos) {
        if (anos.isEmpty()) {
            return questoes;
        }
        List<Questao> doAno = new ArrayList<>();
        for (Questao questao : questoes) {
            if (questao.getAnoEscolar() != null && anos.contains(questao.getAnoEscolar())) {
                doAno.add(questao);
            }
        }
        return doAno;
    }

    /**
     * Por tópico: as questões do ano do aluno; se o tópico não tem nenhuma desse ano, as do tópico de
     * qualquer ano (reserva para não gerar uma prática vazia).
     */
    private static List<Questao> doAnoOuDoTopico(List<Questao> questoes, Set<AnoEscolar> anos) {
        Map<Long, List<Questao>> porTopico = new LinkedHashMap<>();
        for (Questao questao : questoes) {
            porTopico.computeIfAbsent(questao.getTopico().getId(), id -> new ArrayList<>()).add(questao);
        }
        List<Questao> resultado = new ArrayList<>();
        for (List<Questao> doTopico : porTopico.values()) {
            List<Questao> doAno = doAno(doTopico, anos);
            resultado.addAll(doAno.isEmpty() ? doTopico : doAno);
        }
        return resultado;
    }

    private static void adicionarCandidatas(List<Questao> questoes, Map<Long, Questao> questoesPorId,
                                            List<MotorAdaptativo.Candidata> candidatas) {
        if (questoes == null) {
            return;
        }
        for (Questao questao : questoes) {
            if (questoesPorId.putIfAbsent(questao.getId(), questao) == null) {
                candidatas.add(new MotorAdaptativo.Candidata(questao.getId(), questao.getTopico().getId(),
                        questao.getNivelDificuldade()));
            }
        }
    }

    private static List<Questao> emOrdem(List<Long> selecionadas, Map<Long, Questao> questoesPorId) {
        List<Questao> questoes = new ArrayList<>();
        for (Long id : selecionadas) {
            questoes.add(questoesPorId.get(id));
        }
        return questoes;
    }

    private void registrarGeracao(Usuario aluno, AtividadeAluno pratica, List<MotorAdaptativo.TopicoPlanejado> plano,
                                  String prefixo) {
        List<String> partes = new ArrayList<>();
        for (MotorAdaptativo.TopicoPlanejado topico : plano) {
            partes.add("tópico " + topico.topicoId() + " (" + topico.percentualTopico() + "%) -> " + topico.nivelAlvo());
        }
        auditoriaService.registrar(aluno.getId(), aluno.getNome(), null, AcaoAuditoria.GERACAO_PRATICA_ADAPTATIVA,
                "AtividadeAluno", pratica.getId(), true, prefixo + String.join("; ", partes));
    }

    private static Long idDoTopico(AtividadeAluno pratica) {
        return pratica.getTopico() == null ? null : pratica.getTopico().getId();
    }

    private PraticaAdaptativaResponse resumir(AtividadeAluno pratica, boolean nova, boolean diagnostica) {
        Map<String, int[]> porTopicoENivel = new LinkedHashMap<>();
        for (AtividadeAlunoQuestao entregue : pratica.getQuestoes()) {
            Questao questao = entregue.getQuestao();
            String chave = questao.getTopico().getNome() + "|" + questao.getNivelDificuldade().name();
            porTopicoENivel.computeIfAbsent(chave, c -> new int[1])[0]++;
        }
        List<PraticaAdaptativaResponse.TopicoDaPratica> topicos = new ArrayList<>();
        for (Map.Entry<String, int[]> entrada : porTopicoENivel.entrySet()) {
            String[] partes = entrada.getKey().split("\\|");
            topicos.add(new PraticaAdaptativaResponse.TopicoDaPratica(partes[0], partes[1], entrada.getValue()[0]));
        }
        return new PraticaAdaptativaResponse(pratica.getId(), nova, diagnostica,
                pratica.getTopico() == null ? null : pratica.getTopico().getNome(), topicos,
                pratica.getQuestoes().size());
    }
}
