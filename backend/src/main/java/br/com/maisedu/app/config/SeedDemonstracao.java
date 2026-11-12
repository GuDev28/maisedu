package br.com.maisedu.app.config;

import br.com.maisedu.app.model.Alternativa;
import br.com.maisedu.app.model.AlunoTurma;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.AtividadeAluno;
import br.com.maisedu.app.model.AtividadeAlunoQuestao;
import br.com.maisedu.app.model.AvaliacaoQuestao;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.ProfessorTurma;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.StatusQuestao;
import br.com.maisedu.app.model.Topico;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.model.VotoAvaliacao;
import br.com.maisedu.app.repository.AlunoTurmaRepository;
import br.com.maisedu.app.repository.AtividadeAlunoRepository;
import br.com.maisedu.app.repository.AvaliacaoQuestaoRepository;
import br.com.maisedu.app.repository.InstituicaoRepository;
import br.com.maisedu.app.repository.ProfessorTurmaRepository;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.RoleRepository;
import br.com.maisedu.app.repository.TopicoRepository;
import br.com.maisedu.app.repository.TurmaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.AtividadeService;
import br.com.maisedu.app.service.PraticaAdaptativaService;
import br.com.maisedu.app.service.RespostaService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Cenário de demonstração (perfil "dev"), criado só quando o banco está vazio. Todos os dados são
 * fictícios. Monta o que a apresentação precisa mostrar funcionando:
 *
 * - duas instituições, para o banco de questões compartilhado e a validação entre escolas;
 * - administradores, professores (6 contas com papel PROFESSOR, o bastante para o quórum de 3 votos),
 *   turmas e alunos já vinculados;
 * - 96 questões aprovadas (8 tópicos x 3 níveis x 4), algumas pendentes, uma rejeitada e um rascunho;
 * - atividades com respostas simuladas em semanas diferentes, para o painel de desempenho e a evolução;
 * - um professor e um aluno ainda com senha provisória, para demonstrar o primeiro acesso.
 */
@Slf4j
@Component
@Profile("dev")
public class SeedDemonstracao implements CommandLineRunner {

    public static final String SENHA_DEMO = "MaisEdu@2026";
    public static final String SENHA_PROVISORIA_DEMO = "Provisoria1";

    private final InstituicaoRepository instituicaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final TurmaRepository turmaRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final ProfessorTurmaRepository professorTurmaRepository;
    private final TopicoRepository topicoRepository;
    private final QuestaoRepository questaoRepository;
    private final AvaliacaoQuestaoRepository avaliacaoQuestaoRepository;
    private final AtividadeAlunoRepository atividadeAlunoRepository;
    private final AtividadeService atividadeService;
    private final RespostaService respostaService;
    private final PraticaAdaptativaService praticaAdaptativaService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transacao;
    private final int versaoTermos;

    private final Random random = new Random(2026L);

    @PersistenceContext
    private EntityManager entityManager;

    public SeedDemonstracao(InstituicaoRepository instituicaoRepository, UsuarioRepository usuarioRepository,
                            RoleRepository roleRepository, TurmaRepository turmaRepository,
                            AlunoTurmaRepository alunoTurmaRepository,
                            ProfessorTurmaRepository professorTurmaRepository, TopicoRepository topicoRepository,
                            QuestaoRepository questaoRepository,
                            AvaliacaoQuestaoRepository avaliacaoQuestaoRepository,
                            AtividadeAlunoRepository atividadeAlunoRepository, AtividadeService atividadeService,
                            RespostaService respostaService, PraticaAdaptativaService praticaAdaptativaService,
                            PasswordEncoder passwordEncoder, PlatformTransactionManager transactionManager,
                            @Value("${maisedu.termos.versao-atual:1}") int versaoTermos) {
        this.instituicaoRepository = instituicaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.turmaRepository = turmaRepository;
        this.alunoTurmaRepository = alunoTurmaRepository;
        this.professorTurmaRepository = professorTurmaRepository;
        this.topicoRepository = topicoRepository;
        this.questaoRepository = questaoRepository;
        this.avaliacaoQuestaoRepository = avaliacaoQuestaoRepository;
        this.atividadeAlunoRepository = atividadeAlunoRepository;
        this.atividadeService = atividadeService;
        this.respostaService = respostaService;
        this.praticaAdaptativaService = praticaAdaptativaService;
        this.passwordEncoder = passwordEncoder;
        this.transacao = new TransactionTemplate(transactionManager);
        this.versaoTermos = versaoTermos;
    }

    /** Ids do que foi criado, para as etapas seguintes (que rodam em transações separadas). */
    private static final class Cenario {
        final Map<String, Long> usuarios = new LinkedHashMap<>();
        final Map<String, Long> turmas = new LinkedHashMap<>();
        final Map<String, List<Long>> aprovadasPorTopicoENivel = new HashMap<>();
    }

    @Override
    public void run(String... args) {
        if (instituicaoRepository.count() > 0) {
            log.info("SeedDemonstracao: banco já tem dados; nada foi criado.");
            return;
        }
        log.info("SeedDemonstracao: criando o cenário de demonstração (dados fictícios)...");

        Cenario cenario = transacao.execute(status -> criarEstrutura());
        transacao.executeWithoutResult(status -> criarBancoDeQuestoes(cenario));
        simularAtividades(cenario);

        log.warn("""

                ================= MaisEdu — cenário de demonstração (perfil dev) =================
                Senha de todas as contas abaixo: {}

                Escola Estadual Monteiro Lobato (fictícia)
                  admin.a@maisedu.dev   (administrador + professor)
                  ana@maisedu.dev       (professora do 7º ano A)
                  bruno@maisedu.dev     (professor do 6º ano B)
                  alunos: a7001 a a7006 (7º ano A), a6001 a a6004 (6º ano B)

                Colégio Municipal Cecília Meireles (fictício)
                  admin.b@maisedu.dev   (administrador + professor)
                  carla@maisedu.dev     (professora do 8º ano C)
                  diego@maisedu.dev     (professor, sem turma)
                  alunos: b8001 a b8004 (8º ano C)

                Primeiro acesso (senha provisória {}): elisa@maisedu.dev (professora) e a7099 (aluno)
                ===================================================================================
                """, SENHA_DEMO, SENHA_PROVISORIA_DEMO);
    }

    // ------------------------------------------------------------------ estrutura

    private Cenario criarEstrutura() {
        Cenario cenario = new Cenario();
        String hash = passwordEncoder.encode(SENHA_DEMO);
        String hashProvisorio = passwordEncoder.encode(SENHA_PROVISORIA_DEMO);

        Instituicao escolaA = instituicaoRepository.save(new Instituicao(
                "Escola Estadual Monteiro Lobato (fictícia)", "11.111.111/0001-11", "Rua das Flores, 100 (fictício)"));
        Instituicao escolaB = instituicaoRepository.save(new Instituicao(
                "Colégio Municipal Cecília Meireles (fictício)", "22.222.222/0001-22", "Av. Central, 2000 (fictício)"));

        Usuario adminA = equipe("Marina Andrade", "admin.a@maisedu.dev", hash, escolaA, true);
        Usuario ana = equipe("Ana Ribeiro", "ana@maisedu.dev", hash, escolaA, false);
        Usuario bruno = equipe("Bruno Teixeira", "bruno@maisedu.dev", hash, escolaA, false);
        Usuario adminB = equipe("Roberto Lima", "admin.b@maisedu.dev", hash, escolaB, true);
        Usuario carla = equipe("Carla Souza", "carla@maisedu.dev", hash, escolaB, false);
        Usuario diego = equipe("Diego Santos", "diego@maisedu.dev", hash, escolaB, false);
        Usuario elisa = usuarioRepository.save(comPapel(
                new Usuario("Elisa Martins", "elisa@maisedu.dev", hashProvisorio, escolaA), RoleNome.PROFESSOR));
        for (Usuario u : List.of(adminA, ana, bruno, adminB, carla, diego, elisa)) {
            cenario.usuarios.put(u.getEmail(), u.getId());
        }

        Turma setimoA = turmaRepository.save(new Turma("7º ano A", escolaA, AnoEscolar.SETIMO_ANO));
        Turma sextoB = turmaRepository.save(new Turma("6º ano B", escolaA, AnoEscolar.SEXTO_ANO));
        Turma oitavoC = turmaRepository.save(new Turma("8º ano C", escolaB, AnoEscolar.OITAVO_ANO));
        cenario.turmas.put("7A", setimoA.getId());
        cenario.turmas.put("6B", sextoB.getId());
        cenario.turmas.put("8C", oitavoC.getId());

        professorTurmaRepository.save(new ProfessorTurma(ana, setimoA));
        professorTurmaRepository.save(new ProfessorTurma(adminA, setimoA));
        professorTurmaRepository.save(new ProfessorTurma(bruno, sextoB));
        professorTurmaRepository.save(new ProfessorTurma(carla, oitavoC));

        String[] nomes7A = {"Beatriz Alves", "Caio Moreira", "Davi Nogueira", "Eduarda Pires", "Felipe Rocha",
                "Gabriela Dias"};
        for (int i = 0; i < nomes7A.length; i++) {
            Usuario aluno = aluno(nomes7A[i], "a700" + (i + 1), hash, escolaA, setimoA, cenario);
            cenario.usuarios.put(aluno.getLogin(), aluno.getId());
        }
        String[] nomes6B = {"Heitor Campos", "Isabela Freitas", "João Pedro Lima", "Larissa Melo"};
        for (int i = 0; i < nomes6B.length; i++) {
            aluno(nomes6B[i], "a600" + (i + 1), hash, escolaA, sextoB, cenario);
        }
        String[] nomes8C = {"Mateus Barros", "Nicole Duarte", "Otávio Reis", "Paula Cardoso"};
        for (int i = 0; i < nomes8C.length; i++) {
            aluno(nomes8C[i], "b800" + (i + 1), hash, escolaB, oitavoC, cenario);
        }
        // Aluno ainda no primeiro acesso: senha provisória, sem aceite dos termos
        Usuario novato = usuarioRepository.save(comPapel(
                Usuario.novoAluno("Rafael Tavares", "a7099", hashProvisorio, escolaA), RoleNome.ALUNO));
        alunoTurmaRepository.save(new AlunoTurma(novato, setimoA));
        return cenario;
    }

    private Usuario equipe(String nome, String email, String hash, Instituicao instituicao, boolean admin) {
        Usuario usuario = new Usuario(nome, email, hash, instituicao);
        if (admin) {
            comPapel(usuario, RoleNome.ADMIN);
        }
        comPapel(usuario, RoleNome.PROFESSOR);
        ativar(usuario, hash);
        return usuarioRepository.save(usuario);
    }

    private Usuario aluno(String nome, String login, String hash, Instituicao instituicao, Turma turma,
                          Cenario cenario) {
        Usuario aluno = comPapel(Usuario.novoAluno(nome, login, hash, instituicao), RoleNome.ALUNO);
        ativar(aluno, hash);
        Usuario salvo = usuarioRepository.save(aluno);
        alunoTurmaRepository.save(new AlunoTurma(salvo, turma));
        cenario.usuarios.put(login, salvo.getId());
        return salvo;
    }

    /** Conta que já passou pelo primeiro acesso: senha definitiva e termos aceitos. */
    private void ativar(Usuario usuario, String hash) {
        usuario.trocarSenha(hash);
        usuario.aceitarTermos(versaoTermos);
    }

    private Usuario comPapel(Usuario usuario, RoleNome papel) {
        Role role = roleRepository.findByNome(papel)
                .orElseThrow(() -> new IllegalStateException("Papel " + papel + " ausente (ver migrations)"));
        usuario.adicionarRole(role);
        return usuario;
    }

    // ------------------------------------------------------------------ banco de questões

    private void criarBancoDeQuestoes(Cenario cenario) {
        Map<String, Topico> topicos = new HashMap<>();
        for (Topico topico : topicoRepository.findAll()) {
            topicos.put(topico.getNome(), topico);
        }
        List<Usuario> professores = new ArrayList<>();
        for (String email : List.of("ana@maisedu.dev", "bruno@maisedu.dev", "carla@maisedu.dev", "diego@maisedu.dev",
                "admin.a@maisedu.dev", "admin.b@maisedu.dev")) {
            professores.add(usuarioRepository.findById(cenario.usuarios.get(email)).orElseThrow());
        }

        // Aprovadas: cada uma recebeu 3 votos APROVAR de professores diferentes do autor (quórum da RN02)
        int i = 0;
        for (GeradorQuestoesDemo.QuestaoDemo demo : new GeradorQuestoesDemo(2026L).gerar(4)) {
            Usuario autor = professores.get(i % professores.size());
            Questao questao = salvarQuestao(demo, autor, topicos);
            questao.enviarParaValidacao();
            int votos = 0;
            for (int k = 1; votos < 3; k++) {
                Usuario avaliador = professores.get((i + k) % professores.size());
                avaliacaoQuestaoRepository.save(new AvaliacaoQuestao(questao, avaliador, VotoAvaliacao.APROVAR));
                votos++;
            }
            questao.setStatus(StatusQuestao.APROVADA);
            cenario.aprovadasPorTopicoENivel
                    .computeIfAbsent(demo.topico() + "|" + demo.nivel(), c -> new ArrayList<>())
                    .add(questao.getId());
            i++;
        }

        // Pendentes de outra semente (conteúdo diferente), de autoria da Carla, com 1 voto do Diego:
        // aparecem na fila de avaliação da Ana, do Bruno e dos admins.
        Usuario carla = professores.get(2);
        Usuario diego = professores.get(3);
        int pendentes = 0;
        for (GeradorQuestoesDemo.QuestaoDemo demo : new GeradorQuestoesDemo(7L).gerar(1)) {
            if (pendentes >= 4) {
                break;
            }
            Questao questao = salvarQuestao(demo, carla, topicos);
            questao.enviarParaValidacao();
            avaliacaoQuestaoRepository.save(new AvaliacaoQuestao(questao, diego, VotoAvaliacao.APROVAR));
            pendentes++;
        }

        // Rejeitada: gabarito errado, barrado por 3 votos REJEITAR
        Usuario bruno = professores.get(1);
        Questao errada = new Questao("Quanto é 7 × 8?", bruno, topicos.get("Números naturais e operações"),
                NivelDificuldade.FACIL, AnoEscolar.SEXTO_ANO);
        errada.definirAlternativas(List.of("54", "56", "58", "64", "48"), 0);
        questaoRepository.save(errada);
        errada.enviarParaValidacao();
        for (int k = 2; k <= 4; k++) {
            avaliacaoQuestaoRepository.save(new AvaliacaoQuestao(errada, professores.get(k), VotoAvaliacao.REJEITAR));
        }
        errada.setStatus(StatusQuestao.REJEITADA);

        // Rascunho da Ana, para mostrar o envio para validação
        Questao rascunho = new Questao("Qual é o MMC de 4 e 6?", professores.get(0), topicos.get("Frações"),
                NivelDificuldade.MEDIO, AnoEscolar.SEXTO_ANO);
        rascunho.definirAlternativas(List.of("12", "24", "2", "10", "6"), 0);
        questaoRepository.save(rascunho);
    }

    private Questao salvarQuestao(GeradorQuestoesDemo.QuestaoDemo demo, Usuario autor, Map<String, Topico> topicos) {
        Topico topico = topicos.get(demo.topico());
        if (topico == null) {
            throw new IllegalStateException("Tópico não encontrado na lista fixa: " + demo.topico());
        }
        Questao questao = new Questao(demo.enunciado(), autor, topico, demo.nivel(), demo.anoEscolar());
        questao.definirAlternativas(demo.alternativas(), demo.indiceCorreta());
        return questaoRepository.save(questao);
    }

    // ------------------------------------------------------------------ atividades e respostas

    /**
     * Cria atividades pelos serviços reais (as mesmas regras da API) e simula respostas com um
     * "perfil" de acerto por aluno e tópico. Depois recua as datas para que a evolução semanal do
     * painel de desempenho tenha mais de um ponto.
     */
    private void simularAtividades(Cenario cenario) {
        UsuarioAutenticado ana = ator(cenario, "ana@maisedu.dev");
        UsuarioAutenticado bruno = ator(cenario, "bruno@maisedu.dev");
        UsuarioAutenticado carla = ator(cenario, "carla@maisedu.dev");

        Long revisao7A = atividadeService.criar(ana, cenario.turmas.get("7A"),
                "Revisão: inteiros, frações e porcentagem",
                selecionar(cenario, new String[]{"Números inteiros", "Frações", "Porcentagem"}, 2, 1, 0)).getId();
        Long equacoes7A = atividadeService.criar(ana, cenario.turmas.get("7A"), "Equações do 1º grau",
                selecionar(cenario, new String[]{"Equações do 1º grau"}, 2, 2, 1)).getId();
        Long fracoes6B = atividadeService.criar(bruno, cenario.turmas.get("6B"), "Frações e perímetro",
                selecionar(cenario, new String[]{"Frações", "Perímetro e área"}, 2, 2, 0)).getId();
        Long pitagoras8C = atividadeService.criar(carla, cenario.turmas.get("8C"), "Pitágoras e potências",
                selecionar(cenario, new String[]{"Teorema de Pitágoras", "Potenciação e radiciação"}, 2, 1, 1)).getId();

        // Perfil de acerto (0 a 1) de cada aluno por tópico: cria alunos com dificuldades diferentes.
        Map<String, double[]> perfil7A = new LinkedHashMap<>();
        perfil7A.put("a7001", new double[]{0.9, 0.85, 0.9, 0.8});
        perfil7A.put("a7002", new double[]{0.7, 0.3, 0.6, 0.5});
        perfil7A.put("a7003", new double[]{0.4, 0.6, 0.2, 0.4});
        perfil7A.put("a7004", new double[]{0.8, 0.5, 0.7, 0.3});
        perfil7A.put("a7005", new double[]{0.6, 0.4, 0.5, 0.6});
        // a7006 não iniciou nenhuma atividade (aparece como "não iniciada" para a professora)
        String[] topicos7A = {"Números inteiros", "Frações", "Porcentagem", "Equações do 1º grau"};

        for (Map.Entry<String, double[]> aluno : perfil7A.entrySet()) {
            Long alunoId = cenario.usuarios.get(aluno.getKey());
            responder(alunoId, revisao7A, topicos7A, aluno.getValue(), 1.0);
        }
        // Equações: dois alunos concluem, um para no meio
        responder(cenario.usuarios.get("a7001"), equacoes7A, topicos7A, perfil7A.get("a7001"), 1.0);
        responder(cenario.usuarios.get("a7002"), equacoes7A, topicos7A, perfil7A.get("a7002"), 1.0);
        responder(cenario.usuarios.get("a7004"), equacoes7A, topicos7A, perfil7A.get("a7004"), 0.5);

        String[] topicos6B = {"Frações", "Perímetro e área"};
        double[][] perfis6B = {{0.5, 0.8}, {0.8, 0.4}, {0.3, 0.6}};
        for (int i = 0; i < perfis6B.length; i++) {
            responder(cenario.usuarios.get("a600" + (i + 1)), fracoes6B, topicos6B, perfis6B[i], 1.0);
        }

        String[] topicos8C = {"Teorema de Pitágoras", "Potenciação e radiciação"};
        double[][] perfis8C = {{0.4, 0.8}, {0.7, 0.5}, {0.9, 0.9}, {0.5, 0.3}};
        for (int i = 0; i < perfis8C.length; i++) {
            responder(cenario.usuarios.get("b800" + (i + 1)), pitagoras8C, topicos8C, perfis8C[i], 1.0);
        }

        // Datas: a revisão do 7º A foi há 3 semanas, as demais atividades há 2 semanas
        recuarDatas(revisao7A, 21);
        recuarDatas(equacoes7A, 14);
        recuarDatas(fracoes6B, 14);
        recuarDatas(pitagoras8C, 14);

        // Duas práticas adaptativas já feitas na semana passada (evolução com 3 pontos);
        // os demais alunos podem gerar a deles ao vivo na demonstração.
        for (String login : List.of("a7002", "a7003")) {
            Long alunoId = cenario.usuarios.get(login);
            Long praticaId = praticaAdaptativaService.gerarOuRetomar(alunoId).atividadeAlunoId();
            responderFolha(alunoId, praticaId, topicos7A, perfil7A.get(login), 1.0);
            recuarDatasDaFolha(praticaId, 7);
        }
    }

    private UsuarioAutenticado ator(Cenario cenario, String email) {
        return transacao.execute(status -> UsuarioAutenticado.de(
                usuarioRepository.findById(cenario.usuarios.get(email)).orElseThrow()));
    }

    /** Ids de questões aprovadas: {@code [facil, medio, dificil]} questões de cada tópico informado. */
    private List<Long> selecionar(Cenario cenario, String[] topicos, int faceis, int medias, int dificeis) {
        List<Long> ids = new ArrayList<>();
        for (String topico : topicos) {
            ids.addAll(primeiras(cenario, topico, NivelDificuldade.FACIL, faceis));
            ids.addAll(primeiras(cenario, topico, NivelDificuldade.MEDIO, medias));
            ids.addAll(primeiras(cenario, topico, NivelDificuldade.DIFICIL, dificeis));
        }
        return ids;
    }

    private List<Long> primeiras(Cenario cenario, String topico, NivelDificuldade nivel, int quantidade) {
        List<Long> disponiveis = cenario.aprovadasPorTopicoENivel.getOrDefault(topico + "|" + nivel, List.of());
        return new ArrayList<>(disponiveis.subList(0, Math.min(quantidade, disponiveis.size())));
    }

    private void responder(Long alunoId, Long atividadeId, String[] topicos, double[] perfil, double fracaoRespondida) {
        AtividadeAluno atividadeAluno = atividadeService.iniciar(alunoId, atividadeId);
        responderFolha(alunoId, atividadeAluno.getId(), topicos, perfil, fracaoRespondida);
    }

    /** Responde as questões da folha: acerta com a probabilidade do perfil do aluno no tópico da questão. */
    private void responderFolha(Long alunoId, Long atividadeAlunoId, String[] topicos, double[] perfil,
                                double fracaoRespondida) {
        record Escolha(Long questaoEntregueId, Long correta, List<Long> erradas, String topico) {
        }
        List<Escolha> escolhas = transacao.execute(status -> {
            List<Escolha> lista = new ArrayList<>();
            AtividadeAluno atividadeAluno = atividadeAlunoRepository.findById(atividadeAlunoId).orElseThrow();
            for (AtividadeAlunoQuestao entregue : atividadeAluno.getQuestoes()) {
                Long correta = null;
                List<Long> erradas = new ArrayList<>();
                for (Alternativa alternativa : entregue.getQuestao().getAlternativas()) {
                    if (alternativa.isCorreta()) {
                        correta = alternativa.getId();
                    } else {
                        erradas.add(alternativa.getId());
                    }
                }
                lista.add(new Escolha(entregue.getId(), correta, erradas, entregue.getQuestao().getTopico().getNome()));
            }
            return lista;
        });

        int limite = (int) Math.round(escolhas.size() * fracaoRespondida);
        for (int i = 0; i < limite; i++) {
            Escolha escolha = escolhas.get(i);
            double chance = 0.5;
            for (int t = 0; t < topicos.length; t++) {
                if (topicos[t].equals(escolha.topico())) {
                    chance = perfil[Math.min(t, perfil.length - 1)];
                }
            }
            Long alternativa = random.nextDouble() < chance
                    ? escolha.correta()
                    : escolha.erradas().get(random.nextInt(escolha.erradas().size()));
            respostaService.responder(alunoId, atividadeAlunoId, escolha.questaoEntregueId(), alternativa);
        }
    }

    /** Recuo das datas (só para a demonstração), mantendo a ordem entre início, respostas e conclusão. */
    private void recuarDatas(Long atividadeId, int dias) {
        executarSql("UPDATE atividade SET criado_em = criado_em - make_interval(days => :dias) WHERE id = :id",
                dias + 1, atividadeId);
        executarSql("UPDATE atividade_aluno SET iniciada_em = iniciada_em - make_interval(days => :dias), "
                + "concluida_em = concluida_em - make_interval(days => :dias) WHERE atividade_id = :id",
                dias, atividadeId);
        executarSql("UPDATE resposta r SET respondida_em = r.respondida_em - make_interval(days => :dias) "
                + "FROM atividade_aluno_questao aaq JOIN atividade_aluno aa ON aa.id = aaq.atividade_aluno_id "
                + "WHERE r.atividade_aluno_questao_id = aaq.id AND aa.atividade_id = :id", dias, atividadeId);
    }

    private void recuarDatasDaFolha(Long atividadeAlunoId, int dias) {
        executarSql("UPDATE atividade_aluno SET iniciada_em = iniciada_em - make_interval(days => :dias), "
                + "concluida_em = concluida_em - make_interval(days => :dias) WHERE id = :id", dias, atividadeAlunoId);
        executarSql("UPDATE resposta r SET respondida_em = r.respondida_em - make_interval(days => :dias) "
                + "FROM atividade_aluno_questao aaq WHERE r.atividade_aluno_questao_id = aaq.id "
                + "AND aaq.atividade_aluno_id = :id", dias, atividadeAlunoId);
    }

    /** SQL nativo do PostgreSQL (o perfil dev sempre usa PostgreSQL). */
    private void executarSql(String sql, int dias, Long id) {
        transacao.executeWithoutResult(status -> entityManager.createNativeQuery(sql)
                .setParameter("dias", dias)
                .setParameter("id", id)
                .executeUpdate());
    }
}
