package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.PraticaAdaptativaResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.AtividadeAluno;
import br.com.maisedu.app.model.AtividadeAlunoQuestao;
import br.com.maisedu.app.model.DesempenhoTopico;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.StatusQuestao;
import br.com.maisedu.app.model.TipoAtividade;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AtividadeAlunoRepository;
import br.com.maisedu.app.repository.DesempenhoTopicoRepository;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
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
    private final AuditoriaService auditoriaService;
    private final MotorAdaptativo motor;
    private final Random random = new Random();

    public PraticaAdaptativaService(UsuarioRepository usuarioRepository,
                                    DesempenhoTopicoRepository desempenhoTopicoRepository,
                                    QuestaoRepository questaoRepository,
                                    RespostaRepository respostaRepository,
                                    AtividadeAlunoRepository atividadeAlunoRepository,
                                    AuditoriaService auditoriaService,
                                    @Value("${maisedu.adaptativo.maximo-topicos:3}") int maximoTopicos,
                                    @Value("${maisedu.adaptativo.questoes-por-pratica:10}") int questoesPorPratica,
                                    @Value("${maisedu.adaptativo.minimo-respostas-dominio:5}") int minimoRespostas,
                                    @Value("${maisedu.adaptativo.percentual-dominio:70}") BigDecimal percentualDominio) {
        this.usuarioRepository = usuarioRepository;
        this.desempenhoTopicoRepository = desempenhoTopicoRepository;
        this.questaoRepository = questaoRepository;
        this.respostaRepository = respostaRepository;
        this.atividadeAlunoRepository = atividadeAlunoRepository;
        this.auditoriaService = auditoriaService;
        this.motor = new MotorAdaptativo(new MotorAdaptativo.Parametros(
                maximoTopicos, questoesPorPratica, minimoRespostas, percentualDominio));
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
                return resumir(pratica, false);
            }
        }

        List<MotorAdaptativo.DesempenhoNivel> desempenho = new ArrayList<>();
        for (DesempenhoTopico registro : desempenhoTopicoRepository.findByAlunoId(alunoId)) {
            desempenho.add(new MotorAdaptativo.DesempenhoNivel(registro.getTopico().getId(),
                    registro.getNivelDificuldade(), registro.getTotalRespostas(), registro.getTotalAcertos()));
        }

        List<MotorAdaptativo.TopicoPlanejado> plano = motor.planejar(desempenho);
        if (plano.isEmpty()) {
            throw new NegocioException("Você ainda não tem desempenho registrado. Responda uma atividade da sua "
                    + "turma antes de gerar uma prática adaptativa.");
        }

        Map<Long, Questao> questoesPorId = new HashMap<>();
        List<MotorAdaptativo.Candidata> candidatas = new ArrayList<>();
        for (MotorAdaptativo.TopicoPlanejado topico : plano) {
            for (Questao questao : questaoRepository.findByStatusAndTopicoId(StatusQuestao.APROVADA, topico.topicoId())) {
                questoesPorId.put(questao.getId(), questao);
                candidatas.add(new MotorAdaptativo.Candidata(questao.getId(), topico.topicoId(),
                        questao.getNivelDificuldade()));
            }
        }

        Set<Long> jaRespondidas = new HashSet<>(respostaRepository.findQuestoesRespondidasPeloAluno(alunoId));
        List<Long> selecionadas = motor.selecionarQuestoes(plano, candidatas, jaRespondidas, random);
        if (selecionadas.isEmpty()) {
            // RF18/CA03: sem questão aprovada compatível, a prática não é gerada e o aluno é avisado.
            throw new NegocioException("Ainda não há questões aprovadas no banco para os tópicos em que você "
                    + "precisa praticar. Avise o seu professor.");
        }

        List<Questao> questoes = new ArrayList<>();
        for (Long id : selecionadas) {
            questoes.add(questoesPorId.get(id));
        }
        AtividadeAluno pratica = atividadeAlunoRepository.save(AtividadeAluno.adaptativa(aluno, questoes));

        StringBuilder detalhes = new StringBuilder();
        for (MotorAdaptativo.TopicoPlanejado topico : plano) {
            if (!detalhes.isEmpty()) {
                detalhes.append("; ");
            }
            detalhes.append("tópico ").append(topico.topicoId()).append(" (").append(topico.percentualTopico())
                    .append("%) -> ").append(topico.nivelAlvo());
        }
        auditoriaService.registrar(aluno.getId(), aluno.getNome(), AcaoAuditoria.GERACAO_PRATICA_ADAPTATIVA,
                "AtividadeAluno", pratica.getId(), true, detalhes.toString());

        return resumir(pratica, true);
    }

    private PraticaAdaptativaResponse resumir(AtividadeAluno pratica, boolean nova) {
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
        return new PraticaAdaptativaResponse(pratica.getId(), nova, topicos, pratica.getQuestoes().size());
    }
}
