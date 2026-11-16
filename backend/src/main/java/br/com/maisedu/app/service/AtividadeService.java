package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.AtividadeAlunoResumoResponse;
import br.com.maisedu.app.dto.AtividadeResumoResponse;
import br.com.maisedu.app.dto.FolhaAtividadeResponse;
import br.com.maisedu.app.dto.QuestaoResponse;
import br.com.maisedu.app.dto.ResultadoAtividadeResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.Alternativa;
import br.com.maisedu.app.model.AlunoTurma;
import br.com.maisedu.app.model.Atividade;
import br.com.maisedu.app.model.AtividadeAluno;
import br.com.maisedu.app.model.AtividadeAlunoQuestao;
import br.com.maisedu.app.model.AtividadeQuestao;
import br.com.maisedu.app.model.DesempenhoTopico;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.Resposta;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.TipoAtividade;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlunoTurmaRepository;
import br.com.maisedu.app.repository.AtividadeAlunoRepository;
import br.com.maisedu.app.repository.AtividadeRepository;
import br.com.maisedu.app.repository.PlacarAtividade;
import br.com.maisedu.app.repository.Projecoes;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RF15 (atividade regular com prazo), início da atividade pelo aluno com snapshot (RN03), consulta
 * das folhas e resultados. Até o prazo o aluno não vê a correção; o professor vê os resultados de
 * quem já entregou e a análise por questão.
 */
@Service
@RequiredArgsConstructor
public class AtividadeService {

    private static final DateTimeFormatter FORMATO_PRAZO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AtividadeRepository atividadeRepository;
    private final AtividadeAlunoRepository atividadeAlunoRepository;
    private final QuestaoRepository questaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final RespostaRepository respostaRepository;
    private final AcessoTurma acessoTurma;
    private final AuditoriaService auditoriaService;

    // ---------- Professor ----------

    /**
     * RF15: só o professor que leciona na turma cria atividades para ela (RN01), com questões
     * APROVADAS do banco compartilhado, na ordem informada, sem repetição, e com prazo de entrega.
     */
    @Transactional
    public Atividade criar(UsuarioAutenticado ator, Long turmaId, String titulo, LocalDateTime prazo,
                           List<Long> questaoIds) {
        Turma turma = acessoTurma.buscarComoProfessorDaTurma(ator, turmaId,
                "Só o professor que leciona na turma pode criar atividades para ela.");
        if (!turma.isAtivo()) {
            throw new NegocioException("Turma inativa não recebe novas atividades.");
        }
        if (questaoIds == null || questaoIds.isEmpty()) {
            throw new NegocioException("Selecione pelo menos uma questão para a atividade");
        }

        Map<Long, Questao> encontradas = new HashMap<>();
        for (Questao questao : questaoRepository.findAllByIdIn(questaoIds)) {
            encontradas.put(questao.getId(), questao);
        }
        List<Questao> emOrdem = new ArrayList<>();
        for (Long id : questaoIds) {
            Questao questao = encontradas.get(id);
            if (questao == null) {
                throw new NegocioException("Questão " + id + " não encontrada");
            }
            emOrdem.add(questao);
        }

        Usuario professor = usuarioRepository.findById(ator.id())
                .orElseThrow(() -> new NegocioException("Professor não encontrado"));
        Atividade atividade = new Atividade(titulo, turma, professor, prazo);
        atividade.definirQuestoes(emOrdem);
        Atividade salva = atividadeRepository.save(atividade);

        auditoriaService.registrar(ator, AcaoAuditoria.CRIACAO_ATIVIDADE, "Atividade", salva.getId(), true,
                "turma " + turmaId + ", " + emOrdem.size() + " questão(ões), prazo " + salva.getPrazo().format(FORMATO_PRAZO));
        return salva;
    }

    /** Estende ou antecipa o prazo (só o professor da turma e só antes do prazo atual terminar). */
    @Transactional
    public Atividade alterarPrazo(UsuarioAutenticado ator, Long atividadeId, LocalDateTime novoPrazo) {
        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada."));
        acessoTurma.buscarComoProfessorDaTurma(ator, atividade.getTurma().getId(),
                "Só o professor que leciona na turma pode alterar o prazo da atividade.");
        LocalDateTime anterior = atividade.getPrazo();
        atividade.alterarPrazo(novoPrazo, LocalDateTime.now());
        auditoriaService.registrar(ator, AcaoAuditoria.ALTERACAO_PRAZO_ATIVIDADE, "Atividade", atividade.getId(), true,
                "de " + anterior.format(FORMATO_PRAZO) + " para " + atividade.getPrazo().format(FORMATO_PRAZO));
        return atividade;
    }

    @Transactional(readOnly = true)
    public List<AtividadeResumoResponse> listarDaTurma(UsuarioAutenticado ator, Long turmaId) {
        Turma turma = acessoTurma.buscarComAcesso(ator, turmaId);
        List<Atividade> atividades = atividadeRepository.findByTurmaId(turmaId);
        List<Long> ids = new ArrayList<>();
        for (Atividade atividade : atividades) {
            ids.add(atividade.getId());
        }
        int alunosDaTurma = (int) alunoTurmaRepository.countByTurmaId(turmaId);

        // [iniciaram, entregaram] por atividade, com uma única consulta para todas as folhas
        Map<Long, int[]> contagem = new HashMap<>();
        if (!ids.isEmpty()) {
            for (AtividadeAluno atividadeAluno : atividadeAlunoRepository.findByAtividadeIdIn(ids)) {
                int[] numeros = contagem.computeIfAbsent(atividadeAluno.getAtividade().getId(), id -> new int[2]);
                numeros[0]++;
                if (atividadeAluno.isConcluida()) {
                    numeros[1]++;
                }
            }
        }

        LocalDateTime agora = LocalDateTime.now();
        List<AtividadeResumoResponse> resumo = new ArrayList<>();
        for (Atividade atividade : atividades) {
            int[] numeros = contagem.getOrDefault(atividade.getId(), new int[2]);
            resumo.add(resumir(atividade, turma, alunosDaTurma, numeros[0], numeros[1], agora));
        }
        return resumo;
    }

    /** Resumo de uma atividade para o professor (também usado logo depois de criar). */
    public AtividadeResumoResponse resumir(Atividade atividade, Turma turma, int alunosDaTurma, int iniciaram,
                                           int entregaram, LocalDateTime agora) {
        return new AtividadeResumoResponse(atividade.getId(), atividade.getTitulo(), turma.getId(), turma.getNome(),
                atividade.getProfessor().getNome(), atividade.getQuestoes().size(), alunosDaTurma, iniciaram,
                entregaram, atividade.getCriadoEm(), atividade.getPrazo(), atividade.prazoEncerrado(agora),
                atividade.isFechada());
    }

    @Transactional(readOnly = true)
    public Atividade buscarParaResumo(Long atividadeId) {
        return atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada."));
    }

    @Transactional(readOnly = true)
    public int contarAlunosDaTurma(Long turmaId) {
        return (int) alunoTurmaRepository.countByTurmaId(turmaId);
    }

    /** Resultado da atividade por aluno da turma (situação, escolhidas, acertos) e análise por questão. */
    @Transactional(readOnly = true)
    public ResultadoAtividadeResponse resultados(UsuarioAutenticado ator, Long atividadeId) {
        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada."));
        acessoTurma.autorizar(ator, atividade.getTurma());
        LocalDateTime agora = LocalDateTime.now();
        boolean encerrado = atividade.prazoEncerrado(agora);

        Map<Long, AtividadeAluno> porAluno = new HashMap<>();
        Set<Long> folhas = new HashSet<>();
        for (AtividadeAluno atividadeAluno : atividadeAlunoRepository.findByAtividadeId(atividadeId)) {
            porAluno.put(atividadeAluno.getAluno().getId(), atividadeAluno);
            folhas.add(atividadeAluno.getId());
        }
        Map<Long, Placar> placares = placares(folhas);

        int totalQuestoes = atividade.getQuestoes().size();
        List<ResultadoAtividadeResponse.ResultadoAluno> alunos = new ArrayList<>();
        for (AlunoTurma vinculo : alunoTurmaRepository.findByTurmaIdComAluno(atividade.getTurma().getId())) {
            Usuario aluno = vinculo.getAluno();
            AtividadeAluno atividadeAluno = porAluno.get(aluno.getId());
            if (atividadeAluno == null) {
                alunos.add(new ResultadoAtividadeResponse.ResultadoAluno(aluno.getId(), aluno.getNome(),
                        encerrado ? AtividadeAlunoResumoResponse.NAO_ENTREGUE : AtividadeAlunoResumoResponse.NAO_INICIADA,
                        0, 0, totalQuestoes));
                continue;
            }
            Placar placar = placares.getOrDefault(atividadeAluno.getId(), Placar.VAZIO);
            int escolhidas = atividadeAluno.isConcluida() ? placar.respondidas() : escolhidas(atividadeAluno, placar);
            alunos.add(new ResultadoAtividadeResponse.ResultadoAluno(aluno.getId(), aluno.getNome(),
                    situacao(atividadeAluno, agora), escolhidas, placar.acertos(), atividadeAluno.getQuestoes().size()));
        }

        List<QuestaoResponse> questoes = new ArrayList<>();
        for (AtividadeQuestao atividadeQuestao : atividade.getQuestoes()) {
            questoes.add(QuestaoResponse.from(atividadeQuestao.getQuestao()));
        }
        return new ResultadoAtividadeResponse(atividade.getId(), atividade.getTitulo(), atividade.getTurma().getNome(),
                atividade.getPrazo(), encerrado, questoes, alunos, analisar(atividade));
    }

    /**
     * Análise por questão: respostas, acertos e a alternativa errada mais escolhida, contadas no banco
     * numa única consulta agrupada por questão e alternativa.
     */
    private List<ResultadoAtividadeResponse.AnaliseQuestao> analisar(Atividade atividade) {
        Map<Long, Map<Long, Long>> vezesPorQuestao = new HashMap<>();
        for (Projecoes.ContagemAlternativa contagem : respostaRepository.contarAlternativasDaAtividade(atividade.getId())) {
            vezesPorQuestao.computeIfAbsent(contagem.questaoId(), id -> new HashMap<>())
                    .put(contagem.alternativaId(), contagem.total());
        }

        List<ResultadoAtividadeResponse.AnaliseQuestao> analise = new ArrayList<>();
        for (AtividadeQuestao atividadeQuestao : atividade.getQuestoes()) {
            Questao questao = atividadeQuestao.getQuestao();
            Map<Long, Long> vezes = vezesPorQuestao.getOrDefault(questao.getId(), Map.of());
            Alternativa correta = questao.alternativaCorreta();
            int respostas = 0;
            int acertos = 0;
            Alternativa maisEscolhidaErrada = null;
            long vezesMaisEscolhida = 0;
            for (Alternativa alternativa : questao.getAlternativas()) {
                long quantas = vezes.getOrDefault(alternativa.getId(), 0L);
                respostas += (int) quantas;
                if (alternativa.isCorreta()) {
                    acertos += (int) quantas;
                } else if (quantas > vezesMaisEscolhida) {
                    maisEscolhidaErrada = alternativa;
                    vezesMaisEscolhida = quantas;
                }
            }
            ResultadoAtividadeResponse.ErroComum erro = null;
            int erros = respostas - acertos;
            if (maisEscolhidaErrada != null && erros > 0) {
                erro = new ResultadoAtividadeResponse.ErroComum(maisEscolhidaErrada.letra(),
                        maisEscolhidaErrada.getTexto(), (int) vezesMaisEscolhida,
                        DesempenhoTopico.percentual(vezesMaisEscolhida, erros));
            }
            analise.add(new ResultadoAtividadeResponse.AnaliseQuestao(questao.getId(), atividadeQuestao.getOrdem(),
                    questao.getEnunciado(), questao.getTopico().getNome(), questao.getNivelDificuldade().name(),
                    correta.letra(), respostas, acertos, DesempenhoTopico.percentual(acertos, respostas), erro));
        }
        return analise;
    }

    // ---------- Aluno ----------

    /** Atividades das turmas do aluno (com a situação de cada uma) e as práticas adaptativas dele. */
    @Transactional(readOnly = true)
    public List<AtividadeAlunoResumoResponse> listarDoAluno(Long alunoId) {
        // Todas as folhas do aluno e os placares delas em duas consultas, em vez de duas por atividade.
        Map<Long, AtividadeAluno> regularesPorAtividade = new HashMap<>();
        List<AtividadeAluno> praticas = new ArrayList<>();
        Set<Long> folhas = new HashSet<>();
        for (AtividadeAluno atividadeAluno : atividadeAlunoRepository.findByAlunoIdComAtividade(alunoId)) {
            folhas.add(atividadeAluno.getId());
            if (atividadeAluno.getTipo() == TipoAtividade.ADAPTATIVA) {
                praticas.add(atividadeAluno);
            } else {
                regularesPorAtividade.put(atividadeAluno.getAtividade().getId(), atividadeAluno);
            }
        }
        Map<Long, Placar> placares = placares(folhas);

        LocalDateTime agora = LocalDateTime.now();
        List<AtividadeAlunoResumoResponse> lista = new ArrayList<>();
        for (Atividade atividade : atividadeRepository.findDisponiveisParaAluno(alunoId)) {
            AtividadeAluno atividadeAluno = regularesPorAtividade.get(atividade.getId());
            if (atividadeAluno == null) {
                boolean encerrado = atividade.prazoEncerrado(agora);
                lista.add(new AtividadeAlunoResumoResponse(atividade.getId(), null, atividade.getTitulo(),
                        TipoAtividade.REGULAR.name(), atividade.getTurma().getNome(), null,
                        encerrado ? AtividadeAlunoResumoResponse.NAO_ENTREGUE : AtividadeAlunoResumoResponse.NAO_INICIADA,
                        atividade.getQuestoes().size(), 0, null, atividade.getPrazo(), encerrado, null, null));
            } else {
                lista.add(resumir(atividadeAluno, placares.getOrDefault(atividadeAluno.getId(), Placar.VAZIO), agora));
            }
        }
        for (AtividadeAluno pratica : praticas) {
            lista.add(resumir(pratica, placares.getOrDefault(pratica.getId(), Placar.VAZIO), agora));
        }
        return lista;
    }

    /**
     * RF16, passo 1 e 2: inicia a atividade e fixa as questões entregues (RN03). Se o aluno já a
     * iniciou, devolve a mesma folha, com as mesmas questões e a mesma ordem (CA03). Depois do prazo,
     * só reabre a folha de quem já tinha começado (para ver a correção); não inicia folha nova.
     */
    @Transactional
    public AtividadeAluno iniciar(Long alunoId, Long atividadeId) {
        Usuario aluno = usuarioRepository.findByIdParaAtualizacao(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada."));

        if (!aluno.possuiRole(RoleNome.ALUNO)
                || !alunoTurmaRepository.existsByAlunoIdAndTurmaId(alunoId, atividade.getTurma().getId())) {
            throw new AccessDeniedException("Esta atividade não é de uma turma sua.");
        }

        var existente = atividadeAlunoRepository.findByAlunoIdAndAtividadeId(alunoId, atividadeId);
        if (existente.isPresent()) {
            return existente.get();
        }
        if (!atividade.getTurma().isAtivo()) {
            throw new NegocioException("A turma desta atividade foi desativada.");
        }
        if (atividade.prazoEncerrado(LocalDateTime.now())) {
            throw new NegocioException("O prazo desta atividade terminou em " + atividade.getPrazo().format(FORMATO_PRAZO)
                    + ". Ela não pode mais ser iniciada.");
        }

        AtividadeAluno nova = atividadeAlunoRepository.save(AtividadeAluno.regular(aluno, atividade));
        auditoriaService.registrar(aluno.getId(), aluno.getNome(), null, AcaoAuditoria.INICIO_ATIVIDADE, "AtividadeAluno",
                nova.getId(), true, "atividade " + atividadeId);
        return nova;
    }

    /** Folha da atividade (regular ou adaptativa) do próprio aluno. */
    @Transactional(readOnly = true)
    public FolhaAtividadeResponse obterFolha(Long alunoId, Long atividadeAlunoId) {
        AtividadeAluno atividadeAluno = buscarDoAluno(alunoId, atividadeAlunoId);
        return montarFolha(atividadeAluno);
    }

    public AtividadeAluno buscarDoAluno(Long alunoId, Long atividadeAlunoId) {
        AtividadeAluno atividadeAluno = atividadeAlunoRepository.findById(atividadeAlunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada."));
        if (!atividadeAluno.pertenceAo(alunoId)) {
            // RN04: o aluno só acessa o que foi entregue a ele; 404 para não revelar folhas de outros alunos.
            throw new RecursoNaoEncontradoException("Atividade não encontrada.");
        }
        return atividadeAluno;
    }

    /**
     * Monta a folha como o aluno a vê agora: as alternativas escolhidas (marcadas ou enviadas) e, só
     * quando a correção foi liberada, o gabarito e os acertos.
     */
    @Transactional(readOnly = true)
    public FolhaAtividadeResponse montarFolha(AtividadeAluno atividadeAluno) {
        LocalDateTime agora = LocalDateTime.now();
        boolean liberada = atividadeAluno.correcaoLiberada(agora);

        Map<Long, Resposta> respostas = new HashMap<>();
        for (Resposta resposta : respostaRepository.findByAtividadeAluno(atividadeAluno.getId())) {
            respostas.put(resposta.getAtividadeAlunoQuestao().getId(), resposta);
        }

        List<FolhaAtividadeResponse.QuestaoEntregue> questoes = new ArrayList<>();
        int acertos = 0;
        int escolhidas = 0;
        for (AtividadeAlunoQuestao entregue : atividadeAluno.getQuestoes()) {
            Questao questao = entregue.getQuestao();
            List<FolhaAtividadeResponse.AlternativaAluno> alternativas = new ArrayList<>();
            for (Alternativa alternativa : questao.getAlternativas()) {
                alternativas.add(new FolhaAtividadeResponse.AlternativaAluno(alternativa.getId(), alternativa.letra(),
                        alternativa.getTexto()));
            }

            Resposta resposta = respostas.get(entregue.getId());
            Long escolhida = null;
            if (resposta != null) {
                escolhida = resposta.getAlternativa().getId();
            } else if (entregue.getAlternativaMarcada() != null && !atividadeAluno.isConcluida()) {
                escolhida = entregue.getAlternativaMarcada().getId();
            }
            if (escolhida != null) {
                escolhidas++;
            }

            FolhaAtividadeResponse.Correcao correcao = null;
            if (liberada) {
                Alternativa correta = questao.alternativaCorreta();
                boolean acertou = resposta != null && resposta.acertou();
                if (acertou) {
                    acertos++;
                }
                correcao = new FolhaAtividadeResponse.Correcao(resposta != null, acertou, correta.getId(),
                        correta.letra());
            }
            questoes.add(new FolhaAtividadeResponse.QuestaoEntregue(entregue.getId(), entregue.getOrdem(),
                    questao.getEnunciado(), questao.getTopico().getNome(), questao.getNivelDificuldade().name(),
                    alternativas, escolhida, correcao));
        }

        LocalDateTime prazo = atividadeAluno.prazo();
        return new FolhaAtividadeResponse(atividadeAluno.getId(), titulo(atividadeAluno),
                atividadeAluno.getTipo().name(), nomeDoTopico(atividadeAluno), atividadeAluno.getIniciadaEm(),
                atividadeAluno.getConcluidaEm(), prazo, prazo != null && !agora.isBefore(prazo),
                atividadeAluno.aceitaRespostas(agora), liberada, questoes.size(), escolhidas,
                liberada ? acertos : null, questoes);
    }

    private AtividadeAlunoResumoResponse resumir(AtividadeAluno atividadeAluno, Placar placar, LocalDateTime agora) {
        Atividade origem = atividadeAluno.getAtividade();
        boolean liberada = atividadeAluno.correcaoLiberada(agora);
        return new AtividadeAlunoResumoResponse(
                origem == null ? null : origem.getId(),
                atividadeAluno.getId(),
                titulo(atividadeAluno),
                atividadeAluno.getTipo().name(),
                origem == null ? null : origem.getTurma().getNome(),
                nomeDoTopico(atividadeAluno),
                situacao(atividadeAluno, agora),
                atividadeAluno.getQuestoes().size(),
                atividadeAluno.isConcluida() ? placar.respondidas() : escolhidas(atividadeAluno, placar),
                liberada ? placar.acertos() : null,
                atividadeAluno.prazo(),
                liberada,
                atividadeAluno.getIniciadaEm(),
                atividadeAluno.getConcluidaEm());
    }

    /** Questões com alternativa escolhida numa folha em andamento: respondidas (fluxo antigo) + marcadas. */
    private static int escolhidas(AtividadeAluno atividadeAluno, Placar placar) {
        int marcadas = 0;
        for (AtividadeAlunoQuestao entregue : atividadeAluno.getQuestoes()) {
            if (entregue.getAlternativaMarcada() != null) {
                marcadas++;
            }
        }
        return placar.respondidas() + marcadas;
    }

    private static String titulo(AtividadeAluno atividadeAluno) {
        if (atividadeAluno.getAtividade() != null) {
            return atividadeAluno.getAtividade().getTitulo();
        }
        return atividadeAluno.getTopico() != null
                ? "Prática: " + atividadeAluno.getTopico().getNome()
                : "Prática adaptativa";
    }

    private static String nomeDoTopico(AtividadeAluno atividadeAluno) {
        return atividadeAluno.getTopico() == null ? null : atividadeAluno.getTopico().getNome();
    }

    private static String situacao(AtividadeAluno atividadeAluno, LocalDateTime agora) {
        if (atividadeAluno.isConcluida()) {
            return AtividadeAlunoResumoResponse.ENTREGUE;
        }
        return atividadeAluno.aceitaRespostas(agora)
                ? AtividadeAlunoResumoResponse.EM_ANDAMENTO
                : AtividadeAlunoResumoResponse.NAO_ENTREGUE;
    }

    /** Placar de cada folha informada, contado no banco numa única consulta. */
    private Map<Long, Placar> placares(Set<Long> atividadeAlunoIds) {
        Map<Long, Placar> placares = new HashMap<>();
        if (atividadeAlunoIds.isEmpty()) {
            return placares;
        }
        for (PlacarAtividade placar : respostaRepository.contarPorAtividadeAluno(atividadeAlunoIds)) {
            placares.put(placar.atividadeAlunoId(),
                    new Placar(placar.respondidas().intValue(), placar.acertos().intValue()));
        }
        return placares;
    }
}
