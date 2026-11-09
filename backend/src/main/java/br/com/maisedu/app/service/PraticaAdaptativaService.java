package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.PraticaAdaptativaResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.AlunoTurma;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.AtividadeAluno;
import br.com.maisedu.app.model.AtividadeAlunoQuestao;
import br.com.maisedu.app.model.DesempenhoTopico;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.StatusQuestao;
import br.com.maisedu.app.model.TipoAtividade;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlunoTurmaRepository;
import br.com.maisedu.app.repository.AtividadeAlunoRepository;
import br.com.maisedu.app.repository.DesempenhoTopicoRepository;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** RF18: gera a prática adaptativa do aluno a partir do desempenho acumulado (RN05/RN06). */
@Service
public class PraticaAdaptativaService {

    private final UsuarioRepository usuarioRepository;
    private final DesempenhoTopicoRepository desempenhoTopicoRepository;
    private final QuestaoRepository questaoRepository;
    private final RespostaRepository respostaRepository;
    private final AtividadeAlunoRepository atividadeAlunoRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final AuditoriaService auditoriaService;
    private final MotorAdaptativo motor;
    private final Random random = new Random();

    public PraticaAdaptativaService(UsuarioRepository usuarioRepository,
                                    DesempenhoTopicoRepository desempenhoTopicoRepository,
                                    QuestaoRepository questaoRepository,
                                    RespostaRepository respostaRepository,
                                    AtividadeAlunoRepository atividadeAlunoRepository,
                                    AlunoTurmaRepository alunoTurmaRepository,
                                    AuditoriaService auditoriaService,
                                    MotorAdaptativo motor) {
        this.usuarioRepository = usuarioRepository;
        this.desempenhoTopicoRepository = desempenhoTopicoRepository;
        this.questaoRepository = questaoRepository;
        this.respostaRepository = respostaRepository;
        this.atividadeAlunoRepository = atividadeAlunoRepository;
        this.alunoTurmaRepository = alunoTurmaRepository;
        this.auditoriaService = auditoriaService;
        this.motor = motor;
    }

    /**
     * Gera uma prática nova ou, se houver uma prática adaptativa ainda não concluída, retoma essa
     * (o aluno não acumula práticas abertas nem descarta uma prática difícil gerando outra).
     */
    @Transactional
    public PraticaAdaptativaResponse gerarOuRetomar(Long alunoId) {
        Usuario aluno = usuarioRepository.findByIdParaAtualizacao(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
        if (!aluno.possuiRole(RoleNome.ALUNO)) {
            throw new AccessDeniedException("Só alunos geram práticas adaptativas.");
        }

        for (AtividadeAluno pratica : atividadeAlunoRepository.findByAlunoIdAndTipo(alunoId, TipoAtividade.ADAPTATIVA)) {
            if (!pratica.isConcluida()) {
                return resumir(pratica, false, false);
            }
        }

        List<MotorAdaptativo.DesempenhoNivel> desempenho = new ArrayList<>();
        for (DesempenhoTopico registro : desempenhoTopicoRepository.findByAlunoId(alunoId)) {
            desempenho.add(new MotorAdaptativo.DesempenhoNivel(registro.getTopico().getId(),
                    registro.getNivelDificuldade(), registro.getTotalRespostas(), registro.getTotalAcertos()));
        }

        List<MotorAdaptativo.TopicoPlanejado> plano = motor.planejar(desempenho);
        Map<Long, Questao> questoesPorId = new HashMap<>();
        List<MotorAdaptativo.Candidata> candidatas = new ArrayList<>();
        boolean diagnostica = plano.isEmpty();
        if (diagnostica) {
            // Sem nenhuma resposta ainda: prática diagnóstica com questões fáceis de vários tópicos do
            // ano escolar do aluno, em vez de mandá-lo esperar uma atividade da turma.
            Map<Long, List<Questao>> porTopico = new LinkedHashMap<>();
            for (Questao questao : aprovadasDoAnoDoAluno(alunoId)) {
                porTopico.computeIfAbsent(questao.getTopico().getId(), id -> new ArrayList<>()).add(questao);
            }
            List<Long> topicoIds = new ArrayList<>(porTopico.keySet());
            Collections.shuffle(topicoIds, random);
            plano = motor.planejarDiagnostico(topicoIds);
            for (MotorAdaptativo.TopicoPlanejado topico : plano) {
                for (Questao questao : porTopico.get(topico.topicoId())) {
                    questoesPorId.put(questao.getId(), questao);
                    candidatas.add(new MotorAdaptativo.Candidata(questao.getId(), topico.topicoId(),
                            questao.getNivelDificuldade()));
                }
            }
        } else {
            for (MotorAdaptativo.TopicoPlanejado topico : plano) {
                for (Questao questao : questaoRepository.findByStatusAndTopicoId(StatusQuestao.APROVADA, topico.topicoId())) {
                    questoesPorId.put(questao.getId(), questao);
                    candidatas.add(new MotorAdaptativo.Candidata(questao.getId(), topico.topicoId(),
                            questao.getNivelDificuldade()));
                }
            }
        }

        Set<Long> jaRespondidas = new HashSet<>(respostaRepository.findQuestoesRespondidasPeloAluno(alunoId));
        List<Long> selecionadas = motor.selecionarQuestoes(plano, candidatas, jaRespondidas, random);
        if (selecionadas.isEmpty()) {
            // RF18/CA03: sem questão aprovada compatível, a prática não é gerada e o aluno é avisado.
            throw new NegocioException(diagnostica
                    ? "Ainda não há questões aprovadas no banco para o seu ano escolar. Avise o seu professor."
                    : "Ainda não há questões aprovadas no banco para os tópicos em que você "
                    + "precisa praticar. Avise o seu professor.");
        }

        List<Questao> questoes = new ArrayList<>();
        for (Long id : selecionadas) {
            questoes.add(questoesPorId.get(id));
        }
        AtividadeAluno pratica = atividadeAlunoRepository.save(AtividadeAluno.adaptativa(aluno, questoes));

        List<String> partes = new ArrayList<>();
        for (MotorAdaptativo.TopicoPlanejado topico : plano) {
            partes.add("tópico " + topico.topicoId() + " (" + topico.percentualTopico() + "%) -> " + topico.nivelAlvo());
        }
        String detalhes = (diagnostica ? "diagnóstica: " : "") + String.join("; ", partes);
        auditoriaService.registrar(aluno.getId(), aluno.getNome(), null, AcaoAuditoria.GERACAO_PRATICA_ADAPTATIVA,
                "AtividadeAluno", pratica.getId(), true, detalhes);

        return resumir(pratica, true, diagnostica);
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
        return new PraticaAdaptativaResponse(pratica.getId(), nova, diagnostica, topicos,
                pratica.getQuestoes().size());
    }

    /** Questões aprovadas do(s) ano(s) escolar(es) das turmas ativas do aluno; de todos os anos se ele não tem turma. */
    private List<Questao> aprovadasDoAnoDoAluno(Long alunoId) {
        Set<AnoEscolar> anos = new HashSet<>();
        for (AlunoTurma vinculo : alunoTurmaRepository.findByAlunoIdComTurma(alunoId)) {
            if (vinculo.getTurma().isAtivo()) {
                anos.add(vinculo.getTurma().getAnoEscolar());
            }
        }
        if (anos.isEmpty()) {
            return questaoRepository.buscarNoBanco(StatusQuestao.APROVADA, null, null, null);
        }
        List<Questao> questoes = new ArrayList<>();
        for (AnoEscolar ano : anos) {
            questoes.addAll(questaoRepository.buscarNoBanco(StatusQuestao.APROVADA, null, null, ano));
        }
        return questoes;
    }
}
