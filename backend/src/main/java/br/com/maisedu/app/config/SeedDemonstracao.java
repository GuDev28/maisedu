package br.com.maisedu.app.config;

import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.NivelDificuldade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Cenário de demonstração do perfil "dev", criado só quando o banco está vazio. Todos os dados são
 * fictícios. Monta, numa única carga, o que a apresentação e os painéis precisam:
 *
 * - duas escolas com {@value #TOTAL_ALUNOS} alunos no total, divididos de forma desigual, em turmas do 6º ao
 *   9º ano com {@value #MINIMO_POR_TURMA} a {@value #MAXIMO_POR_TURMA} alunos (tamanho sorteado);
 * - um administrador por escola, só com o papel ADMIN (gestão, painéis e auditoria; não leciona, não cria
 *   questões nem vota), professores com turmas, um professor sem turma e contas de primeiro acesso;
 * - banco de questões compartilhado: aprovadas pelo quórum de 3 votos, pendentes na fila de avaliação
 *   (com 0 a 2 votos), rejeitadas com motivo e rascunhos;
 * - 8 semanas de atividades com prazo (as vencidas fechadas e contabilizadas, uma aberta por turma),
 *   práticas adaptativas, desempenho por tópico e nível e logins recentes para o painel do admin;
 * - uma professora e um aluno "vitrine", com as contas mais completas (ver {@link #PROFESSORA_VITRINE} e
 *   {@link #ALUNO_VITRINE}).
 *
 * Cada aluno tem um perfil de acerto (forte, médio ou fraco), um tópico mais difícil, uma variação por
 * turma, aprende um pouco a cada semana e melhora nos tópicos em que faz prática adaptativa. Nas questões,
 * um dos distratores atrai a maior parte dos erros (como um erro de raciocínio comum).
 *
 * Por ser muito volume, a gravação é feita em lote com JDBC, numa única transação (tudo ou nada), e não
 * pelos serviços. Por isso as regras que os serviços garantem são seguidas aqui à mão: quórum de votos,
 * prazo depois da criação, folha única por atividade, alternativas da própria questão, correção da
 * atividade só depois do prazo e desempenho calculado só com as respostas contabilizadas.
 */
@Component
@Profile("dev")
public class SeedDemonstracao implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDemonstracao.class);

    public static final String SENHA_DEMO = "MaisEdu@2026";
    public static final String SENHA_PROVISORIA_DEMO = "Provisoria1";

    /** Professora com questões em todos os status, fila de avaliação e atividades em 3 turmas. */
    public static final String PROFESSORA_VITRINE = "ana@maisedu.dev";
    /** Aluno com atividades feitas, aguardando correção, em andamento, a fazer, perdida e práticas por tópico. */
    public static final String ALUNO_VITRINE = "a700001";
    public static final String PROFESSORA_PRIMEIRO_ACESSO = "elisa@maisedu.dev";
    public static final String ALUNO_PRIMEIRO_ACESSO = "a700099";

    static final int TOTAL_ALUNOS = 1000;
    static final int MINIMO_POR_TURMA = 20;
    static final int MAXIMO_POR_TURMA = 30;
    /** Professores sem conta nomeada em cada escola (prof01 a prof04), que dividem as turmas restantes. */
    private static final int PROFESSORES_GENERICOS_POR_ESCOLA = 4;
    private static final int LOTE = 2000;

    /** Dias atrás em que cada atividade regular foi criada (todas com prazo de 5 dias, já vencido). */
    private static final int[] DIAS_DAS_ATIVIDADES = {55, 48, 34, 27, 13, 6};
    private static final int DIAS_DE_PRAZO = 5;
    /** A atividade aberta de cada turma foi criada há 2 dias e vence em 3. */
    private static final int DIAS_ATIVIDADE_ATUAL = 2;

    private static final String TURMA_DA_VITRINE = "7º ano A";
    private static final List<String> TURMAS_DA_PROFESSORA_VITRINE = List.of("7º ano A", "7º ano B", "8º ano A");
    /** Atividade vencida que o aluno vitrine não entregou (índice em {@link #DIAS_DAS_ATIVIDADES}). */
    private static final int ATIVIDADE_PERDIDA_PELA_VITRINE = 1;
    /** Práticas já enviadas pelo aluno vitrine: dias atrás e tópico escolhido (nulo: a recomendada pelo motor). */
    private static final int[] DIAS_DAS_PRATICAS_DA_VITRINE = {45, 33, 24, 16, 9, 3};
    private static final String[] TOPICOS_DAS_PRATICAS_DA_VITRINE =
            {null, "Equações do 1º grau", "Porcentagem", null, "Equações do 1º grau", "Números inteiros"};
    /** Prática do aluno vitrine começada hoje e ainda não enviada. */
    private static final String TOPICO_DA_PRATICA_EM_ANDAMENTO = "Média, moda e mediana";

    private static final DateTimeFormatter FORMATO_PRAZO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ------------------------------------------------------------------ escolas e equipe

    /**
     * Conta da equipe: professor ou, com {@code admin}, administrador (só o papel ADMIN, sem turma).
     * {@code turmas} = -1 para os professores genéricos, que dividem as turmas restantes.
     */
    private record Membro(String nome, String email, boolean admin, int turmas, boolean primeiroAcesso) {
    }

    private record EscolaPlanejada(String nome, String cnpj, String endereco, int alunos, List<Membro> equipe) {
    }

    private static final int ALUNOS_DA_PRIMEIRA_ESCOLA = 560;

    private static final List<EscolaPlanejada> ESCOLAS = List.of(
            new EscolaPlanejada("Escola Estadual Monteiro Lobato (fictícia)", "11.111.111/0001-11",
                    "Rua das Flores, 100 (fictício)", ALUNOS_DA_PRIMEIRA_ESCOLA, List.of(
                    new Membro("Marina Andrade", "admin.a@maisedu.dev", true, 0, false),
                    new Membro("Ana Ribeiro", PROFESSORA_VITRINE, false, TURMAS_DA_PROFESSORA_VITRINE.size(), false),
                    new Membro("Bruno Teixeira", "bruno@maisedu.dev", false, 4, false),
                    new Membro("Elisa Martins", PROFESSORA_PRIMEIRO_ACESSO, false, 0, true))),
            new EscolaPlanejada("Colégio Municipal Cecília Meireles (fictício)", "22.222.222/0001-22",
                    "Av. Central, 2000 (fictício)", TOTAL_ALUNOS - ALUNOS_DA_PRIMEIRA_ESCOLA, List.of(
                    new Membro("Roberto Lima", "admin.b@maisedu.dev", true, 0, false),
                    new Membro("Carla Souza", "carla@maisedu.dev", false, 4, false),
                    new Membro("Diego Santos", "diego@maisedu.dev", false, 0, false))));

    private static final String[] PREFIXOS_DO_ANO = {"6º", "7º", "8º", "9º"};

    private static final Map<AnoEscolar, List<String>> TOPICOS_DO_ANO = new EnumMap<>(AnoEscolar.class);

    static {
        TOPICOS_DO_ANO.put(AnoEscolar.SEXTO_ANO, List.of("Frações", "Perímetro e área", "Potenciação e radiciação"));
        TOPICOS_DO_ANO.put(AnoEscolar.SETIMO_ANO,
                List.of("Números inteiros", "Porcentagem", "Equações do 1º grau", "Média, moda e mediana"));
        TOPICOS_DO_ANO.put(AnoEscolar.OITAVO_ANO,
                List.of("Equações do 1º grau", "Potenciação e radiciação", "Média, moda e mediana", "Porcentagem"));
        TOPICOS_DO_ANO.put(AnoEscolar.NONO_ANO,
                List.of("Teorema de Pitágoras", "Potenciação e radiciação", "Equações do 1º grau"));
    }

    /**
     * Dificuldade própria de cada tópico (soma à probabilidade de acerto): deixa o mapa ano × tópico do
     * painel com contraste, por exemplo Frações e Equações como pontos fracos das escolas.
     */
    private static final Map<String, Double> DIFICULDADE_DO_TOPICO = Map.of(
            "Frações", -0.10,
            "Equações do 1º grau", -0.08,
            "Porcentagem", -0.05,
            "Teorema de Pitágoras", -0.05,
            "Potenciação e radiciação", -0.02,
            "Números inteiros", 0.03,
            "Perímetro e área", 0.04,
            "Média, moda e mediana", 0.07);
    /** Em ordem fixa: a ordem do Map.of muda a cada execução e mudaria os sorteios (a carga deve ser sempre a mesma). */
    private static final List<String> TOPICOS_SIMULADOS = List.copyOf(new TreeSet<>(DIFICULDADE_DO_TOPICO.keySet()));

    private static final String[] PRENOMES = {"Ana", "Bruna", "Camila", "Daniela", "Elaine", "Fernanda", "Giovana",
            "Helena", "Isadora", "Júlia", "Karina", "Laura", "Mariana", "Natália", "Olívia", "Patrícia", "Rafaela",
            "Sofia", "Tainá", "Valentina", "André", "Bernardo", "Carlos", "Diego", "Enzo", "Fábio", "Gustavo",
            "Henrique", "Igor", "João", "Kauã", "Lucas", "Miguel", "Nicolas", "Otávio", "Pedro", "Rafael",
            "Samuel", "Thiago", "Vinícius"};
    private static final String[] SOBRENOMES = {"Almeida", "Barbosa", "Cardoso", "Correia", "Costa", "Dias",
            "Farias", "Ferreira", "Gomes", "Lima", "Lopes", "Machado", "Martins", "Melo", "Mendes", "Moraes",
            "Nascimento", "Nunes", "Oliveira", "Pereira", "Pinto", "Ramos", "Reis", "Ribeiro", "Rocha", "Santos",
            "Silva", "Soares", "Souza", "Teixeira", "Vieira"};

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transacao;
    private final int versaoTermos;
    private final Random random = new Random(2026L);

    public SeedDemonstracao(JdbcTemplate jdbc, PasswordEncoder passwordEncoder,
                            PlatformTransactionManager transactionManager,
                            @Value("${maisedu.termos.versao-atual:1}") int versaoTermos) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.transacao = new TransactionTemplate(transactionManager);
        this.versaoTermos = versaoTermos;
    }

    // ------------------------------------------------------------------ modelo em memória

    private static final class Escola {
        long id;
        EscolaPlanejada plano;
        String sufixo;
        final List<Professor> professores = new ArrayList<>();
        final List<Turma> turmas = new ArrayList<>();
        final List<Aluno> alunos = new ArrayList<>();
    }

    /** Conta da equipe da escola: professor ou administrador ({@code admin}, só gestão). */
    private static final class Professor {
        long id;
        String nome;
        String email;
        Escola escola;
        boolean admin;
        boolean primeiroAcesso;
        int cota;
        final List<Turma> turmas = new ArrayList<>();
    }

    private static final class Turma {
        long id;
        String nome;
        AnoEscolar ano;
        Escola escola;
        Professor professor;
        int tamanho;
        double ajuste;
        String topicoFracoDaTurma;
        final List<Aluno> alunos = new ArrayList<>();
        final List<AtividadePlanejada> atividades = new ArrayList<>();
    }

    private static final class Aluno {
        long id;
        String nome;
        String login;
        Turma turma;
        double base;
        double engajamento;
        boolean vitrine;
        boolean primeiroAcesso;
        final Map<String, Double> ajustePorTopico = new HashMap<>();
        final Map<String, Double> efeitoDaPratica = new HashMap<>();
    }

    private record QuestaoDoBanco(long id, String topico, long topicoId, NivelDificuldade nivel, long correta,
                                  List<Long> erradas, long erroComum) {
    }

    /** Questão a gravar, com as alternativas e os votos. */
    private static final class QuestaoNova {
        long id;
        String enunciado;
        Professor autor;
        String status;
        LocalDateTime criada;
        String topico;
        NivelDificuldade nivel;
        AnoEscolar ano;
        List<String> alternativas;
        int indiceCorreta;
        final List<Long> idsAlternativas = new ArrayList<>();
    }

    private record Voto(QuestaoNova questao, Professor professor, String voto, LocalDateTime em, String comentario,
                        String statusDepois) {
    }

    /** Momento da atividade na linha do tempo da demonstração. */
    private enum Momento { VENCIDA, ATUAL, REVISAO, DESAFIO }

    private static final class AtividadePlanejada {
        long id;
        Turma turma;
        Momento momento;
        int indice;
        String titulo;
        LocalDateTime criada;
        LocalDateTime prazo;
        /** Quanto a participação cai em relação ao engajamento do aluno (atividades recentes têm menos folhas). */
        double reducaoParticipacao;
        /** Chance de o aluno que começou só marcar e não enviar. */
        double chanceSoMarcacao;
        final List<QuestaoDoBanco> questoes = new ArrayList<>();

        boolean aberta() {
            return momento != Momento.VENCIDA;
        }
    }

    private enum Situacao { SORTEADA, ENVIADA, EM_ANDAMENTO }

    /** Uma folha a simular: atividade regular ou, com {@code atividade} nula, prática adaptativa. */
    private record Evento(LocalDateTime quando, AtividadePlanejada atividade, String topicoEscolhido, Situacao situacao) {
    }

    /** Uma folha (atividade regular ou prática) a gravar, com as questões e o que o aluno escolheu. */
    private static final class Folha {
        long id;
        Aluno aluno;
        AtividadePlanejada atividade;
        Long topicoId;
        LocalDateTime iniciada;
        LocalDateTime enviada;
        LocalDateTime contabilizada;
        /** Para folhas não enviadas: fração das questões já marcadas (negativa: 60% sorteadas). */
        double fracaoMarcada = -1;
        final List<QuestaoDoBanco> questoes = new ArrayList<>();
        final List<Long> escolhidas = new ArrayList<>();
    }

    /** Números para o quadro do log. */
    private static final class Resumo {
        final List<String> linhas = new ArrayList<>();
        int folhas;
        int questoesEntregues;
        int respostas;
    }

    // ------------------------------------------------------------------ execução

    @Override
    public void run(String... args) {
        Integer existentes = jdbc.queryForObject("select count(*) from instituicao", Integer.class);
        if (existentes != null && existentes > 0) {
            log.info("SeedDemonstracao: banco já tem dados; nada foi criado.");
            return;
        }
        long inicio = System.currentTimeMillis();
        log.info("SeedDemonstracao: criando o cenário de demonstração ({} alunos em {} escolas, 8 semanas)...",
                TOTAL_ALUNOS, ESCOLAS.size());
        Resumo resumo = transacao.execute(status -> criar());

        StringBuilder quadro = new StringBuilder("\n\n");
        quadro.append("================= MaisEdu — cenário de demonstração (perfil dev) =================\n");
        quadro.append("Senha de todas as contas: ").append(SENHA_DEMO).append('\n');
        for (String linha : resumo.linhas) {
            quadro.append(linha).append('\n');
        }
        quadro.append("Primeiro acesso (senha provisória ").append(SENHA_PROVISORIA_DEMO).append("): ")
                .append(PROFESSORA_PRIMEIRO_ACESSO).append(" (professora) e ").append(ALUNO_PRIMEIRO_ACESSO)
                .append(" (aluno)\n");
        quadro.append(resumo.folhas).append(" folhas, ").append(resumo.questoesEntregues).append(" questões entregues e ")
                .append(resumo.respostas).append(" respostas, criadas em ")
                .append((System.currentTimeMillis() - inicio) / 1000).append(" s.\n");
        quadro.append("===================================================================================\n");
        log.warn(quadro.toString());
    }

    private Resumo criar() {
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        LocalDateTime inicioDoPeriodo = agora.minusDays(60);
        String hash = passwordEncoder.encode(SENHA_DEMO);
        String hashProvisorio = passwordEncoder.encode(SENHA_PROVISORIA_DEMO);
        Map<String, Long> roles = idsPorNome("select id, nome from role");
        Map<String, Long> topicos = idsPorNome("select id, nome from topico");

        // ---------------- escolas, equipe, turmas e alunos (em memória; a gravação é em lote, depois)
        List<Long> idsEscolas = reservar("instituicao_id_seq", ESCOLAS.size());
        List<Escola> escolas = new ArrayList<>();
        Set<String> logins = new HashSet<>(List.of(ALUNO_VITRINE, ALUNO_PRIMEIRO_ACESSO));
        for (int e = 0; e < ESCOLAS.size(); e++) {
            Escola escola = new Escola();
            escola.id = idsEscolas.get(e);
            escola.plano = ESCOLAS.get(e);
            escola.sufixo = String.valueOf((char) ('a' + e));
            escolas.add(escola);
            montarEquipe(escola);
            montarTurmas(escola, e == 0);
            montarAlunos(escola, e == 0, logins);
        }
        gravarEscolasEPessoas(escolas, roles, hash, hashProvisorio, inicioDoPeriodo, agora);

        // ---------------- banco de questões compartilhado (autores e avaliadores: só quem tem o papel de
        // professor e já fez o primeiro acesso; os administradores não participam da validação entre pares)
        List<Professor> autores = new ArrayList<>();
        for (Escola escola : escolas) {
            for (Professor professor : escola.professores) {
                if (!professor.primeiroAcesso && !professor.admin) {
                    autores.add(professor);
                }
            }
        }
        Professor vitrine = professorPorEmail(escolas, PROFESSORA_VITRINE);
        List<Voto> votos = new ArrayList<>();
        Map<String, Map<NivelDificuldade, List<QuestaoDoBanco>>> banco =
                criarQuestoes(autores, vitrine, topicos, votos, inicioDoPeriodo, agora);

        // ---------------- atividades: 6 vencidas e 1 aberta por turma; a turma do aluno vitrine tem mais 2 abertas
        List<AtividadePlanejada> atividades = planejarAtividades(escolas, banco, agora);
        gravarAtividades(atividades);

        // ---------------- folhas de cada aluno, em ordem de data: a prática adaptativa melhora o aluno no
        // tópico praticado, e isso aparece nas atividades seguintes (efeito da prática no painel)
        List<Folha> folhas = new ArrayList<>();
        for (Escola escola : escolas) {
            for (Aluno aluno : escola.alunos) {
                if (aluno.primeiroAcesso) {
                    continue;
                }
                List<Evento> eventos = aluno.vitrine ? eventosDaVitrine(aluno, agora) : eventos(aluno, agora);
                eventos.sort((a, b) -> a.quando().compareTo(b.quando()));
                for (Evento evento : eventos) {
                    Folha folha = evento.atividade() == null
                            ? praticar(aluno, evento, banco, topicos, inicioDoPeriodo)
                            : responderAtividade(aluno, evento, inicioDoPeriodo);
                    if (folha != null) {
                        folhas.add(folha);
                    }
                }
            }
        }
        Resumo resumo = new Resumo();
        gravarFolhas(folhas, resumo);
        gravarDesempenho(folhas);
        gravarAuditoria(escolas, atividades, votos, folhas, agora);

        montarResumo(escolas, resumo);
        return resumo;
    }

    // ------------------------------------------------------------------ escolas, equipe, turmas e alunos

    private void montarEquipe(Escola escola) {
        List<Membro> membros = new ArrayList<>(escola.plano.equipe());
        for (int g = 1; g <= PROFESSORES_GENERICOS_POR_ESCOLA; g++) {
            String nome = PRENOMES[random.nextInt(PRENOMES.length)] + " " + SOBRENOMES[random.nextInt(SOBRENOMES.length)];
            membros.add(new Membro(nome, String.format("prof%02d.%s@maisedu.dev", g, escola.sufixo), false, -1, false));
        }
        List<Long> ids = reservar("usuario_id_seq", membros.size());
        for (int i = 0; i < membros.size(); i++) {
            Membro membro = membros.get(i);
            Professor professor = new Professor();
            professor.id = ids.get(i);
            professor.nome = membro.nome();
            professor.email = membro.email();
            professor.escola = escola;
            professor.admin = membro.admin();
            professor.primeiroAcesso = membro.primeiroAcesso();
            professor.cota = membro.turmas();
            escola.professores.add(professor);
        }
    }

    /**
     * Turmas do 6º ao 9º ano com {@value #MINIMO_POR_TURMA} a {@value #MAXIMO_POR_TURMA} alunos: o tamanho de
     * cada uma é sorteado nessa faixa e depois ajustado, sem sair dela, para somar o total de alunos da escola.
     */
    private void montarTurmas(Escola escola, boolean temVitrine) {
        int alunos = escola.plano.alunos();
        int quantidade = Math.round(alunos / ((MINIMO_POR_TURMA + MAXIMO_POR_TURMA) / 2f));
        if (quantidade * MINIMO_POR_TURMA > alunos || quantidade * MAXIMO_POR_TURMA < alunos) {
            throw new IllegalStateException("Não dá para dividir " + alunos + " alunos em turmas de "
                    + MINIMO_POR_TURMA + " a " + MAXIMO_POR_TURMA);
        }
        int[] porAno = new int[AnoEscolar.values().length];
        for (int i = 0; i < quantidade; i++) {
            porAno[i % porAno.length]++;
        }
        List<Long> ids = reservar("turma_id_seq", quantidade);
        int proxima = 0;
        for (int a = 0; a < porAno.length; a++) {
            AnoEscolar ano = AnoEscolar.values()[a];
            List<String> doAno = TOPICOS_DO_ANO.get(ano);
            for (int letra = 0; letra < porAno[a]; letra++) {
                Turma turma = new Turma();
                turma.id = ids.get(proxima++);
                turma.nome = PREFIXOS_DO_ANO[a] + " ano " + (char) ('A' + letra);
                turma.ano = ano;
                turma.escola = escola;
                turma.tamanho = MINIMO_POR_TURMA + random.nextInt(MAXIMO_POR_TURMA - MINIMO_POR_TURMA + 1);
                turma.ajuste = gaussiana(0.05);
                turma.topicoFracoDaTurma = random.nextDouble() < 0.4 ? doAno.get(random.nextInt(doAno.size())) : null;
                escola.turmas.add(turma);
            }
        }
        // ajusta o sorteio, um aluno por vez em turmas sorteadas, até fechar o total da escola
        int diferenca = alunos;
        for (Turma turma : escola.turmas) {
            diferenca -= turma.tamanho;
        }
        while (diferenca != 0) {
            Turma turma = escola.turmas.get(random.nextInt(escola.turmas.size()));
            if (diferenca > 0 && turma.tamanho < MAXIMO_POR_TURMA) {
                turma.tamanho++;
                diferenca--;
            } else if (diferenca < 0 && turma.tamanho > MINIMO_POR_TURMA) {
                turma.tamanho--;
                diferenca++;
            }
        }

        // cotas: os professores nomeados têm número fixo de turmas; os genéricos dividem o resto
        int fixas = 0;
        List<Professor> genericos = new ArrayList<>();
        for (Professor professor : escola.professores) {
            if (professor.cota < 0) {
                genericos.add(professor);
            } else {
                fixas += professor.cota;
            }
        }
        int resto = quantidade - fixas;
        if (resto < genericos.size()) {
            throw new IllegalStateException("Turmas insuficientes para a equipe de " + escola.plano.nome());
        }
        for (int g = 0; g < genericos.size(); g++) {
            genericos.get(g).cota = resto / genericos.size() + (g < resto % genericos.size() ? 1 : 0);
        }

        if (temVitrine) {
            Professor vitrine = professorPorEmail(List.of(escola), PROFESSORA_VITRINE);
            for (String nome : TURMAS_DA_PROFESSORA_VITRINE) {
                atribuir(turmaPorNome(escola, nome), vitrine);
            }
        }
        List<Turma> livres = new ArrayList<>();
        for (Turma turma : escola.turmas) {
            if (turma.professor == null) {
                livres.add(turma);
            }
        }
        Collections.shuffle(livres, random);
        int proximaLivre = 0;
        for (Professor professor : escola.professores) {
            while (professor.turmas.size() < professor.cota) {
                atribuir(livres.get(proximaLivre++), professor);
            }
        }
        if (proximaLivre != livres.size()) {
            throw new IllegalStateException("Turma sem professor em " + escola.plano.nome());
        }
    }

    private static void atribuir(Turma turma, Professor professor) {
        turma.professor = professor;
        professor.turmas.add(turma);
    }

    private void montarAlunos(Escola escola, boolean temVitrine, Set<String> logins) {
        // uma vaga por aluno em cada turma, embaralhadas
        List<Turma> vagas = new ArrayList<>();
        for (Turma turma : escola.turmas) {
            for (int i = 0; i < turma.tamanho; i++) {
                vagas.add(turma);
            }
        }
        Collections.shuffle(vagas, random);
        List<Long> ids = reservar("usuario_id_seq", escola.plano.alunos());
        int proximo = 0;

        if (temVitrine) {
            Turma daVitrine = turmaPorNome(escola, TURMA_DA_VITRINE);
            vagas.remove(daVitrine);
            vagas.remove(daVitrine);

            Aluno vitrine = novoAluno(ids.get(proximo++), "Beatriz Alves", ALUNO_VITRINE, daVitrine, escola);
            vitrine.vitrine = true;
            vitrine.base = 0.66;
            vitrine.engajamento = 1.0;
            vitrine.ajustePorTopico.put("Números inteiros", 0.15);
            vitrine.ajustePorTopico.put("Porcentagem", 0.04);
            vitrine.ajustePorTopico.put("Equações do 1º grau", -0.22);
            vitrine.ajustePorTopico.put("Média, moda e mediana", 0.02);

            Aluno novato = novoAluno(ids.get(proximo++), "Rafael Tavares", ALUNO_PRIMEIRO_ACESSO, daVitrine, escola);
            novato.primeiroAcesso = true;
        }

        for (Turma turma : vagas) {
            String nome = PRENOMES[random.nextInt(PRENOMES.length)] + " " + SOBRENOMES[random.nextInt(SOBRENOMES.length)]
                    + " " + SOBRENOMES[random.nextInt(SOBRENOMES.length)];
            String login;
            do {
                login = "a" + (100000 + random.nextInt(900000));
            } while (!logins.add(login));
            Aluno aluno = novoAluno(ids.get(proximo++), nome, login, turma, escola);
            double perfil = random.nextDouble();
            aluno.base = perfil < 0.30 ? 0.80 : perfil < 0.75 ? 0.62 : 0.43;
            aluno.engajamento = 0.55 + random.nextDouble() * 0.43;
            List<String> doAno = TOPICOS_DO_ANO.get(turma.ano);
            String maisDificil = doAno.get(random.nextInt(doAno.size()));
            for (String topico : TOPICOS_SIMULADOS) {
                double ajuste = gaussiana(0.07);
                if (topico.equals(maisDificil)) {
                    ajuste -= 0.18;
                }
                if (topico.equals(turma.topicoFracoDaTurma)) {
                    ajuste -= 0.10;
                }
                aluno.ajustePorTopico.put(topico, ajuste);
            }
        }
    }

    private static Aluno novoAluno(long id, String nome, String login, Turma turma, Escola escola) {
        Aluno aluno = new Aluno();
        aluno.id = id;
        aluno.nome = nome;
        aluno.login = login;
        aluno.turma = turma;
        turma.alunos.add(aluno);
        escola.alunos.add(aluno);
        return aluno;
    }

    private void gravarEscolasEPessoas(List<Escola> escolas, Map<String, Long> roles, String hash,
                                       String hashProvisorio, LocalDateTime inicioDoPeriodo, LocalDateTime agora) {
        List<Object[]> linhasEscola = new ArrayList<>();
        List<Object[]> usuarios = new ArrayList<>();
        List<Object[]> papeis = new ArrayList<>();
        List<Object[]> linhasTurma = new ArrayList<>();
        List<Object[]> vinculosProfessor = new ArrayList<>();
        List<Object[]> vinculosAluno = new ArrayList<>();
        LocalDateTime cadastroEquipe = inicioDoPeriodo.minusDays(20);
        LocalDateTime cadastroAlunos = inicioDoPeriodo.minusDays(5);
        for (Escola escola : escolas) {
            linhasEscola.add(new Object[]{escola.id, escola.plano.nome(), escola.plano.cnpj(), escola.plano.endereco(),
                    inicioDoPeriodo.minusDays(30)});
            for (Professor professor : escola.professores) {
                LocalDateTime criado = professor.primeiroAcesso ? agora.minusDays(1) : cadastroEquipe;
                usuarios.add(usuario(professor.id, professor.nome, professor.email, null, null,
                        professor.primeiroAcesso ? hashProvisorio : hash, escola.id, criado, professor.primeiroAcesso));
                // o administrador tem só o papel ADMIN (menor privilégio); os demais, só PROFESSOR
                papeis.add(new Object[]{professor.id, papel(roles, professor.admin ? "ADMIN" : "PROFESSOR")});
            }
            for (Turma turma : escola.turmas) {
                linhasTurma.add(new Object[]{turma.id, turma.nome, escola.id, turma.ano.name(), inicioDoPeriodo.minusDays(10)});
                vinculosProfessor.add(new Object[]{turma.professor.id, turma.id});
            }
            for (Aluno aluno : escola.alunos) {
                LocalDateTime criado = aluno.primeiroAcesso ? agora.minusDays(1) : cadastroAlunos;
                usuarios.add(usuario(aluno.id, aluno.nome, null, aluno.login, "responsavel." + aluno.login + "@maisedu.dev",
                        aluno.primeiroAcesso ? hashProvisorio : hash, escola.id, criado, aluno.primeiroAcesso));
                papeis.add(new Object[]{aluno.id, papel(roles, "ALUNO")});
                vinculosAluno.add(new Object[]{aluno.id, aluno.turma.id});
            }
        }
        lote("insert into instituicao (id, nome, cnpj, endereco, ativo, criado_em) values (?, ?, ?, ?, true, ?)",
                linhasEscola);
        lote("insert into usuario (id, nome, email, login, email_responsavel, senha, instituicao_id, ativo, criado_em, "
                + "tentativas_falhas, senha_temporaria, versao_sessao, termos_versao, termos_aceitos_em) "
                + "values (?, ?, ?, ?, ?, ?, ?, true, ?, 0, ?, 0, ?, ?)", usuarios);
        lote("insert into usuario_role (usuario_id, role_id) values (?, ?)", papeis);
        lote("insert into turma (id, nome, instituicao_id, ano_escolar, ativo, criado_em) values (?, ?, ?, ?, true, ?)",
                linhasTurma);
        lote("insert into professor_turma (professor_id, turma_id) values (?, ?)", vinculosProfessor);
        lote("insert into aluno_turma (aluno_id, turma_id) values (?, ?)", vinculosAluno);
    }

    /** Linha de usuário: contas de primeiro acesso ficam com senha provisória e sem aceite dos termos. */
    private Object[] usuario(long id, String nome, String email, String login, String emailResponsavel, String senha,
                             long instituicaoId, LocalDateTime criado, boolean primeiroAcesso) {
        Integer termos = primeiroAcesso ? null : versaoTermos;
        LocalDateTime aceite = primeiroAcesso ? null : criado;
        return new Object[]{id, nome, email, login, emailResponsavel, senha, instituicaoId, criado, primeiroAcesso,
                termos, aceite};
    }

    private static long papel(Map<String, Long> roles, String nome) {
        Long id = roles.get(nome);
        if (id == null) {
            throw new IllegalStateException("Papel " + nome + " ausente (ver migrations)");
        }
        return id;
    }

    // ------------------------------------------------------------------ banco de questões

    /**
     * Aprovadas (3 votos APROVAR de colegas das duas escolas), pendentes na fila de avaliação (0 a 2 votos),
     * rejeitadas com motivo e rascunhos. A professora vitrine é autora de 1 em cada 6 aprovadas, tem
     * questões pendentes, uma rejeitada e dois rascunhos; e há pendentes de colegas em que ela ainda não votou.
     */
    private Map<String, Map<NivelDificuldade, List<QuestaoDoBanco>>> criarQuestoes(
            List<Professor> autores, Professor vitrine, Map<String, Long> topicos, List<Voto> votos,
            LocalDateTime inicioDoPeriodo, LocalDateTime agora) {
        List<Professor> colegas = new ArrayList<>(autores);
        colegas.remove(vitrine);
        List<QuestaoNova> questoes = new ArrayList<>();

        // aprovadas: 10 por tópico e nível
        List<GeradorQuestoesDemo.QuestaoDemo> aprovadas = new GeradorQuestoesDemo(99L).gerar(10);
        for (int i = 0; i < aprovadas.size(); i++) {
            Professor autor = i % 6 == 0 ? vitrine : colegas.get(i % colegas.size());
            QuestaoNova questao = nova(aprovadas.get(i), autor, "APROVADA",
                    inicioDoPeriodo.minusDays(25).plusHours(2L * i));
            questoes.add(questao);
            int dados = 0;
            for (int k = 1; dados < 3; k++) {
                Professor avaliador = autores.get((i + k) % autores.size());
                if (avaliador == autor) {
                    continue;
                }
                dados++;
                votos.add(new Voto(questao, avaliador, "APROVAR", questao.criada.plusDays(dados), null,
                        dados == 3 ? "APROVADA" : "PENDENTE"));
            }
        }

        // pendentes e rejeitadas de outra semente (enunciados diferentes), criadas nas últimas duas semanas
        List<GeradorQuestoesDemo.QuestaoDemo> outras = new GeradorQuestoesDemo(123L).gerar(1);
        for (int j = 0; j < 17; j++) {
            GeradorQuestoesDemo.QuestaoDemo demo = outras.get(j);
            LocalDateTime criada = agora.minusDays(14 - j / 2).withHour(8 + j % 9).withMinute(5 * j % 60).withSecond(0);
            if (j < 3) {
                // da vitrine, aguardando os colegas (0, 1 e 2 votos)
                QuestaoNova questao = nova(demo, vitrine, "PENDENTE", criada);
                questoes.add(questao);
                votar(questao, colegas, j, j, "APROVAR", null, votos);
            } else if (j < 13) {
                // de colegas, na fila de avaliação da vitrine (ela ainda não votou)
                Professor autor = colegas.get((j * 5) % colegas.size());
                QuestaoNova questao = nova(demo, autor, "PENDENTE", criada);
                questoes.add(questao);
                List<Professor> semVitrine = new ArrayList<>(colegas);
                semVitrine.remove(autor);
                votar(questao, semVitrine, j, j % 3, "APROVAR", null, votos);
            } else if (j < 15) {
                // de colegas, em que a vitrine já votou (somem da fila dela)
                Professor autor = colegas.get((j * 3) % colegas.size());
                QuestaoNova questao = nova(demo, autor, "PENDENTE", criada);
                questoes.add(questao);
                votos.add(new Voto(questao, vitrine, "APROVAR", criada.plusHours(5), null, "PENDENTE"));
                if (j == 14) {
                    List<Professor> outrosAvaliadores = new ArrayList<>(colegas);
                    outrosAvaliadores.remove(autor);
                    votar(questao, outrosAvaliadores, j, 1, "APROVAR", null, votos);
                }
            } else {
                // de colegas, rejeitadas pelo quórum com o motivo
                Professor autor = colegas.get((j * 7) % colegas.size());
                QuestaoNova questao = nova(demo, autor, "REJEITADA", criada);
                questoes.add(questao);
                List<Professor> avaliadores = new ArrayList<>(colegas);
                avaliadores.remove(autor);
                votar(questao, avaliadores, j, 3, "REJEITAR",
                        "O enunciado precisa ser revisto: faltam dados para chegar à alternativa correta.", votos);
            }
        }

        // rejeitada da vitrine: gabarito errado, barrado por 3 votos com o motivo
        QuestaoNova errada = manual("Quanto é 7 × 8?", vitrine, "REJEITADA", agora.minusDays(20).withHour(10),
                "Números naturais e operações", NivelDificuldade.FACIL, AnoEscolar.SEXTO_ANO,
                List.of("54", "56", "58", "64", "48"), 0);
        questoes.add(errada);
        String[] motivos = {
                "A alternativa marcada como correta está errada: 7 × 8 = 56.",
                "Gabarito incorreto. A resposta certa é 56 (alternativa B).",
                "Conferi a conta: 7 × 8 dá 56, não 54."};
        for (int v = 0; v < motivos.length; v++) {
            votos.add(new Voto(errada, colegas.get(v), "REJEITAR", errada.criada.plusHours(3L + 4L * v), motivos[v],
                    v == motivos.length - 1 ? "REJEITADA" : "PENDENTE"));
        }
        // rascunhos da vitrine, para mostrar a edição e o envio para validação
        questoes.add(manual("Qual é o MMC de 4 e 6?", vitrine, "RASCUNHO", agora.minusDays(2).withHour(19),
                "Frações", NivelDificuldade.MEDIO, AnoEscolar.SEXTO_ANO, List.of("12", "24", "2", "10", "6"), 0));
        questoes.add(manual("Uma camiseta de R$ 80,00 está com 15% de desconto. Qual é o valor do desconto?", vitrine,
                "RASCUNHO", agora.minusHours(20), "Porcentagem", NivelDificuldade.FACIL, AnoEscolar.SETIMO_ANO,
                List.of("R$ 12,00", "R$ 15,00", "R$ 8,00", "R$ 68,00", "R$ 10,00"), 0));

        // gravação
        List<Long> ids = reservar("questao_id_seq", questoes.size());
        int totalAlternativas = 0;
        for (QuestaoNova questao : questoes) {
            totalAlternativas += questao.alternativas.size();
        }
        List<Long> idsAlternativas = reservar("alternativa_id_seq", totalAlternativas);
        List<Object[]> linhasQuestao = new ArrayList<>();
        List<Object[]> linhasAlternativa = new ArrayList<>();
        int proximaAlternativa = 0;
        for (int i = 0; i < questoes.size(); i++) {
            QuestaoNova questao = questoes.get(i);
            questao.id = ids.get(i);
            Long topicoId = topicos.get(questao.topico);
            if (topicoId == null) {
                throw new IllegalStateException("Tópico não encontrado na lista fixa: " + questao.topico);
            }
            linhasQuestao.add(new Object[]{questao.id, questao.enunciado, questao.autor.id, questao.status,
                    questao.criada, topicoId, questao.nivel.name(), questao.ano.name()});
            for (int a = 0; a < questao.alternativas.size(); a++) {
                long idAlternativa = idsAlternativas.get(proximaAlternativa++);
                questao.idsAlternativas.add(idAlternativa);
                linhasAlternativa.add(new Object[]{idAlternativa, questao.id, questao.alternativas.get(a),
                        a == questao.indiceCorreta, a + 1});
            }
        }
        List<Object[]> linhasVoto = new ArrayList<>();
        for (Voto voto : votos) {
            linhasVoto.add(new Object[]{voto.questao().id, voto.professor().id, voto.voto(), voto.em(), voto.comentario()});
        }
        lote("insert into questao (id, enunciado, autor_id, status, criado_em, topico_id, nivel_dificuldade, ano_escolar) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?)", linhasQuestao);
        lote("insert into alternativa (id, questao_id, texto, correta, ordem) values (?, ?, ?, ?, ?)", linhasAlternativa);
        lote("insert into avaliacao_questao (questao_id, professor_id, voto, avaliado_em, comentario) "
                + "values (?, ?, ?, ?, ?)", linhasVoto);

        // banco em memória com as aprovadas (para as atividades e práticas)
        Map<String, Map<NivelDificuldade, List<QuestaoDoBanco>>> banco = new HashMap<>();
        for (QuestaoNova questao : questoes) {
            if (!questao.status.equals("APROVADA")) {
                continue;
            }
            long correta = questao.idsAlternativas.get(questao.indiceCorreta);
            List<Long> erradas = new ArrayList<>();
            for (int a = 0; a < questao.idsAlternativas.size(); a++) {
                if (a != questao.indiceCorreta) {
                    erradas.add(questao.idsAlternativas.get(a));
                }
            }
            long erroComum = erradas.get((int) (questao.id % erradas.size()));
            banco.computeIfAbsent(questao.topico, t -> new EnumMap<>(NivelDificuldade.class))
                    .computeIfAbsent(questao.nivel, n -> new ArrayList<>())
                    .add(new QuestaoDoBanco(questao.id, questao.topico, topicos.get(questao.topico), questao.nivel,
                            correta, erradas, erroComum));
        }
        return banco;
    }

    /** {@code quantidade} votos de avaliadores diferentes, a partir de uma posição da lista. */
    private static void votar(QuestaoNova questao, List<Professor> avaliadores, int deslocamento, int quantidade,
                              String voto, String comentario, List<Voto> votos) {
        for (int v = 0; v < quantidade; v++) {
            Professor avaliador = avaliadores.get((deslocamento + v) % avaliadores.size());
            String depois = v == 2 ? (voto.equals("APROVAR") ? "APROVADA" : "REJEITADA") : "PENDENTE";
            votos.add(new Voto(questao, avaliador, voto, questao.criada.plusHours(6L * (v + 1)), comentario, depois));
        }
    }

    private static QuestaoNova nova(GeradorQuestoesDemo.QuestaoDemo demo, Professor autor, String status,
                                    LocalDateTime criada) {
        return manual(demo.enunciado(), autor, status, criada, demo.topico(), demo.nivel(), demo.anoEscolar(),
                demo.alternativas(), demo.indiceCorreta());
    }

    private static QuestaoNova manual(String enunciado, Professor autor, String status, LocalDateTime criada,
                                      String topico, NivelDificuldade nivel, AnoEscolar ano, List<String> alternativas,
                                      int indiceCorreta) {
        QuestaoNova questao = new QuestaoNova();
        questao.enunciado = enunciado;
        questao.autor = autor;
        questao.status = status;
        questao.criada = criada;
        questao.topico = topico;
        questao.nivel = nivel;
        questao.ano = ano;
        questao.alternativas = alternativas;
        questao.indiceCorreta = indiceCorreta;
        return questao;
    }

    // ------------------------------------------------------------------ atividades

    private List<AtividadePlanejada> planejarAtividades(List<Escola> escolas,
                                                        Map<String, Map<NivelDificuldade, List<QuestaoDoBanco>>> banco,
                                                        LocalDateTime agora) {
        List<AtividadePlanejada> atividades = new ArrayList<>();
        for (Escola escola : escolas) {
            for (Turma turma : escola.turmas) {
                List<String> doAno = TOPICOS_DO_ANO.get(turma.ano);
                for (int a = 0; a <= DIAS_DAS_ATIVIDADES.length; a++) {
                    boolean atual = a == DIAS_DAS_ATIVIDADES.length;
                    LocalDateTime criada = agora.minusDays(atual ? DIAS_ATIVIDADE_ATUAL : DIAS_DAS_ATIVIDADES[a])
                            .withHour(8 + random.nextInt(4)).withMinute(random.nextInt(60)).withSecond(0);
                    String t1 = doAno.get(a % doAno.size());
                    String t2 = doAno.get((a + 1) % doAno.size());
                    AtividadePlanejada atividade = atividade(turma, atual ? Momento.ATUAL : Momento.VENCIDA, a,
                            "Lista " + (a + 1) + ": " + t1 + " · " + t2, criada,
                            criada.plusDays(DIAS_DE_PRAZO).withHour(23).withMinute(59));
                    atividade.reducaoParticipacao = atual ? 0.30 : 0.0;
                    atividade.chanceSoMarcacao = atual ? 0.20 : 0.05;
                    atividade.questoes.addAll(sortear(banco, t1, 2, 1, 1));
                    atividade.questoes.addAll(sortear(banco, t2, 2, 1, 1));
                    atividades.add(atividade);
                }
                if (turma.escola == escolas.get(0) && turma.nome.equals(TURMA_DA_VITRINE)) {
                    // mais duas abertas na turma do aluno vitrine: uma de ontem e uma de hoje
                    LocalDateTime ontem = agora.minusDays(1).withHour(9).withMinute(15).withSecond(0);
                    AtividadePlanejada revisao = atividade(turma, Momento.REVISAO, DIAS_DAS_ATIVIDADES.length + 1,
                            "Revisão para a avaliação: " + doAno.get(0) + " · " + doAno.get(2), ontem,
                            ontem.plusDays(4).withHour(23).withMinute(59));
                    revisao.reducaoParticipacao = 0.50;
                    revisao.chanceSoMarcacao = 0.50;
                    revisao.questoes.addAll(sortear(banco, doAno.get(0), 1, 2, 1));
                    revisao.questoes.addAll(sortear(banco, doAno.get(2), 1, 2, 1));
                    atividades.add(revisao);

                    LocalDateTime hoje = agora.minusHours(3).withSecond(0);
                    AtividadePlanejada desafio = atividade(turma, Momento.DESAFIO, DIAS_DAS_ATIVIDADES.length + 2,
                            "Desafio da semana: " + doAno.get(3), hoje, hoje.plusDays(6).withHour(23).withMinute(59));
                    desafio.reducaoParticipacao = 0.75;
                    desafio.chanceSoMarcacao = 0.50;
                    desafio.questoes.addAll(sortear(banco, doAno.get(3), 1, 2, 2));
                    atividades.add(desafio);
                }
            }
        }
        List<Long> ids = reservar("atividade_id_seq", atividades.size());
        for (int i = 0; i < atividades.size(); i++) {
            atividades.get(i).id = ids.get(i);
        }
        return atividades;
    }

    private static AtividadePlanejada atividade(Turma turma, Momento momento, int indice, String titulo,
                                                LocalDateTime criada, LocalDateTime prazo) {
        AtividadePlanejada atividade = new AtividadePlanejada();
        atividade.turma = turma;
        atividade.momento = momento;
        atividade.indice = indice;
        atividade.titulo = titulo;
        atividade.criada = criada;
        atividade.prazo = prazo;
        turma.atividades.add(atividade);
        return atividade;
    }

    private void gravarAtividades(List<AtividadePlanejada> atividades) {
        List<Object[]> linhas = new ArrayList<>();
        List<Object[]> questoes = new ArrayList<>();
        for (AtividadePlanejada atividade : atividades) {
            // as vencidas já foram fechadas pela rotina, um minuto depois do prazo
            linhas.add(new Object[]{atividade.id, atividade.titulo, atividade.turma.id, atividade.turma.professor.id,
                    atividade.criada, atividade.prazo, atividade.aberta() ? null : atividade.prazo.plusMinutes(1)});
            for (int q = 0; q < atividade.questoes.size(); q++) {
                questoes.add(new Object[]{atividade.id, atividade.questoes.get(q).id(), q + 1});
            }
        }
        lote("insert into atividade (id, titulo, turma_id, professor_id, criado_em, prazo, fechada_em) "
                + "values (?, ?, ?, ?, ?, ?, ?)", linhas);
        lote("insert into atividade_questao (atividade_id, questao_id, ordem) values (?, ?, ?)", questoes);
    }

    // ------------------------------------------------------------------ respostas simuladas

    /** Folhas de um aluno qualquer: participa conforme o engajamento e faz de 0 a 3 práticas. */
    private List<Evento> eventos(Aluno aluno, LocalDateTime agora) {
        List<Evento> eventos = new ArrayList<>();
        for (AtividadePlanejada atividade : aluno.turma.atividades) {
            if (random.nextDouble() > aluno.engajamento - atividade.reducaoParticipacao) {
                continue;
            }
            LocalDateTime quando = momentoDeComecar(atividade, agora);
            if (quando != null) {
                eventos.add(new Evento(quando, atividade, null, Situacao.SORTEADA));
            }
        }
        double sorteio = random.nextDouble() * aluno.engajamento;
        int praticas = sorteio < 0.25 ? 0 : sorteio < 0.50 ? 1 : sorteio < 0.70 ? 2 : 3;
        for (int p = 0; p < praticas; p++) {
            eventos.add(new Evento(agora.minusDays(3 + random.nextInt(48)).withHour(14 + random.nextInt(7))
                    .withMinute(random.nextInt(60)), null, null, Situacao.SORTEADA));
        }
        return eventos;
    }

    /**
     * Folhas do aluno vitrine: entregou as vencidas menos uma (aparece como "não entregue"), enviou a atual
     * (aguarda a correção, que só sai depois do prazo), começou a revisão e não abriu o desafio; fez práticas
     * recomendadas e por tópico e deixou uma prática por tópico em andamento.
     */
    private List<Evento> eventosDaVitrine(Aluno aluno, LocalDateTime agora) {
        List<Evento> eventos = new ArrayList<>();
        for (AtividadePlanejada atividade : aluno.turma.atividades) {
            switch (atividade.momento) {
                case VENCIDA -> {
                    if (atividade.indice != ATIVIDADE_PERDIDA_PELA_VITRINE) {
                        eventos.add(new Evento(atividade.criada.plusHours(20 + random.nextInt(30)), atividade, null,
                                Situacao.ENVIADA));
                    }
                }
                case ATUAL -> eventos.add(new Evento(atividade.criada.plusHours(6), atividade, null, Situacao.ENVIADA));
                case REVISAO -> eventos.add(new Evento(agora.minusHours(2), atividade, null, Situacao.EM_ANDAMENTO));
                case DESAFIO -> {
                    // ainda não começou: aparece como "a fazer"
                }
            }
        }
        for (int p = 0; p < DIAS_DAS_PRATICAS_DA_VITRINE.length; p++) {
            eventos.add(new Evento(agora.minusDays(DIAS_DAS_PRATICAS_DA_VITRINE[p]).withHour(15 + p % 4).withMinute(10 * p),
                    null, TOPICOS_DAS_PRATICAS_DA_VITRINE[p], Situacao.ENVIADA));
        }
        eventos.add(new Evento(agora.minusHours(5), null, TOPICO_DA_PRATICA_EM_ANDAMENTO, Situacao.EM_ANDAMENTO));
        return eventos;
    }

    /** Quando o aluno abre a atividade: nas vencidas, nos 4 dias seguintes; nas abertas, até 1 hora atrás. */
    private LocalDateTime momentoDeComecar(AtividadePlanejada atividade, LocalDateTime agora) {
        if (!atividade.aberta()) {
            return atividade.criada.plusMinutes(random.nextInt(96 * 60));
        }
        long janela = Math.min(Duration.between(atividade.criada, agora.minusHours(1)).toMinutes(), 36 * 60);
        if (janela <= 0) {
            return null;
        }
        return atividade.criada.plusMinutes(random.nextInt((int) janela));
    }

    /** Folha de uma atividade regular: a maioria envia; alguns só marcam e não enviam até o prazo. */
    private Folha responderAtividade(Aluno aluno, Evento evento, LocalDateTime inicioDoPeriodo) {
        AtividadePlanejada atividade = evento.atividade();
        Folha folha = new Folha();
        folha.aluno = aluno;
        folha.atividade = atividade;
        folha.iniciada = evento.quando();
        folha.questoes.addAll(atividade.questoes);
        boolean soMarcacao = switch (evento.situacao()) {
            case ENVIADA -> false;
            case EM_ANDAMENTO -> true;
            case SORTEADA -> random.nextDouble() < atividade.chanceSoMarcacao;
        };
        if (soMarcacao) {
            folha.fracaoMarcada = evento.situacao() == Situacao.EM_ANDAMENTO ? 0.5 : -1;
        } else {
            folha.enviada = folha.iniciada.plusMinutes(10 + random.nextInt(35));
            // correção e desempenho só depois do prazo, quando a rotina fecha a atividade
            folha.contabilizada = atividade.aberta() ? null : atividade.prazo.plusMinutes(1);
        }
        escolher(aluno, folha, folha.iniciada, inicioDoPeriodo);
        return folha;
    }

    /**
     * Prática adaptativa no tópico mais fraco do aluno naquele momento (ou no escolhido): a recomendada
     * (10 questões, com um segundo tópico) ou a do tópico (5 questões). Depois dela, o aluno melhora no tópico.
     */
    private Folha praticar(Aluno aluno, Evento evento, Map<String, Map<NivelDificuldade, List<QuestaoDoBanco>>> banco,
                           Map<String, Long> topicos, LocalDateTime inicioDoPeriodo) {
        List<String> doAno = TOPICOS_DO_ANO.get(aluno.turma.ano);
        boolean doTopico = evento.situacao() == Situacao.SORTEADA
                ? random.nextDouble() < 0.4
                : evento.topicoEscolhido() != null;
        String topico = evento.topicoEscolhido() != null ? evento.topicoEscolhido() : topicoMaisFraco(aluno, doAno);
        LocalDateTime quando = evento.quando();
        Folha folha = new Folha();
        folha.aluno = aluno;
        folha.topicoId = doTopico ? topicos.get(topico) : null;
        folha.iniciada = quando;
        boolean enviada = evento.situacao() != Situacao.EM_ANDAMENTO;
        if (enviada) {
            folha.enviada = quando.plusMinutes(8 + random.nextInt(20));
            folha.contabilizada = folha.enviada;
        } else {
            folha.fracaoMarcada = 0.4;
        }
        double habilidade = habilidade(aluno, topico, NivelDificuldade.MEDIO, quando, inicioDoPeriodo);
        NivelDificuldade alvo = habilidade < 0.5 ? NivelDificuldade.FACIL
                : habilidade < 0.75 ? NivelDificuldade.MEDIO : NivelDificuldade.DIFICIL;
        folha.questoes.addAll(sortearNoNivel(banco, topico, alvo, doTopico ? 5 : 6));
        if (!doTopico) {
            String segundo = doAno.get(random.nextInt(doAno.size()));
            folha.questoes.addAll(sortearNoNivel(banco, segundo, NivelDificuldade.MEDIO, 4));
        }
        removerRepetidas(folha.questoes);
        if (folha.questoes.isEmpty()) {
            return null;
        }
        escolher(aluno, folha, quando, inicioDoPeriodo);
        if (enviada) {
            aluno.efeitoDaPratica.merge(topico, 0.05, (atual, mais) -> Math.min(0.20, atual + mais));
        }
        return folha;
    }

    /** Probabilidade de acerto do aluno no tópico e nível, na data (aprende um pouco por semana). */
    private double habilidade(Aluno aluno, String topico, NivelDificuldade nivel, LocalDateTime quando,
                              LocalDateTime inicioDoPeriodo) {
        double semanas = Duration.between(inicioDoPeriodo, quando).toDays() / 7.0;
        double ajusteNivel = switch (nivel) {
            case FACIL -> 0.12;
            case MEDIO -> 0.0;
            case DIFICIL -> -0.16;
        };
        double p = aluno.base + aluno.turma.ajuste + DIFICULDADE_DO_TOPICO.getOrDefault(topico, 0.0)
                + aluno.ajustePorTopico.getOrDefault(topico, 0.0)
                + aluno.efeitoDaPratica.getOrDefault(topico, 0.0) + ajusteNivel + 0.01 * semanas;
        return Math.max(0.05, Math.min(0.97, p));
    }

    /** Escolhe a alternativa de cada questão; o distrator "comum" atrai 55% dos erros. */
    private void escolher(Aluno aluno, Folha folha, LocalDateTime quando, LocalDateTime inicioDoPeriodo) {
        for (QuestaoDoBanco questao : folha.questoes) {
            double p = habilidade(aluno, questao.topico(), questao.nivel(), quando, inicioDoPeriodo);
            long escolhida;
            if (random.nextDouble() < p) {
                escolhida = questao.correta();
            } else if (random.nextDouble() < 0.55) {
                escolhida = questao.erroComum();
            } else {
                escolhida = questao.erradas().get(random.nextInt(questao.erradas().size()));
            }
            folha.escolhidas.add(escolhida);
        }
    }

    private String topicoMaisFraco(Aluno aluno, List<String> doAno) {
        String pior = doAno.get(0);
        double menor = Double.MAX_VALUE;
        for (String topico : doAno) {
            double valor = aluno.ajustePorTopico.getOrDefault(topico, 0.0)
                    + aluno.efeitoDaPratica.getOrDefault(topico, 0.0) + gaussiana(0.04);
            if (valor < menor) {
                menor = valor;
                pior = topico;
            }
        }
        return pior;
    }

    private void gravarFolhas(List<Folha> folhas, Resumo resumo) {
        List<Long> ids = reservar("atividade_aluno_id_seq", folhas.size());
        int total = 0;
        for (Folha folha : folhas) {
            total += folha.questoes.size();
        }
        List<Long> idsQuestoes = reservar("atividade_aluno_questao_id_seq", total);
        List<Object[]> linhasFolha = new ArrayList<>();
        List<Object[]> linhasQuestao = new ArrayList<>();
        List<Object[]> linhasResposta = new ArrayList<>();
        int proxima = 0;
        for (int f = 0; f < folhas.size(); f++) {
            Folha folha = folhas.get(f);
            folha.id = ids.get(f);
            linhasFolha.add(new Object[]{folha.id, folha.aluno.id, folha.atividade == null ? null : folha.atividade.id,
                    folha.atividade == null ? "ADAPTATIVA" : "REGULAR", folha.topicoId, folha.iniciada, folha.enviada});
            long marcadas = Math.round(folha.questoes.size() * folha.fracaoMarcada);
            for (int q = 0; q < folha.questoes.size(); q++) {
                long idQuestaoEntregue = idsQuestoes.get(proxima++);
                Long marcada = null;
                if (folha.enviada == null) {
                    boolean marcar = folha.fracaoMarcada >= 0 ? q < marcadas : random.nextDouble() < 0.6;
                    marcada = marcar ? folha.escolhidas.get(q) : null;
                }
                linhasQuestao.add(new Object[]{idQuestaoEntregue, folha.id, folha.questoes.get(q).id(), q + 1, marcada,
                        marcada == null ? null : folha.iniciada.plusMinutes(q + 1)});
                if (folha.enviada != null) {
                    linhasResposta.add(new Object[]{idQuestaoEntregue, folha.escolhidas.get(q), folha.enviada,
                            folha.contabilizada});
                }
            }
        }
        lote("insert into atividade_aluno (id, aluno_id, atividade_id, tipo, topico_id, iniciada_em, concluida_em) "
                + "values (?, ?, ?, ?, ?, ?, ?)", linhasFolha);
        lote("insert into atividade_aluno_questao (id, atividade_aluno_id, questao_id, ordem, alternativa_marcada_id, "
                + "marcada_em) values (?, ?, ?, ?, ?, ?)", linhasQuestao);
        lote("insert into resposta (atividade_aluno_questao_id, alternativa_id, respondida_em, contabilizada_em) "
                + "values (?, ?, ?, ?)", linhasResposta);
        resumo.folhas = linhasFolha.size();
        resumo.questoesEntregues = linhasQuestao.size();
        resumo.respostas = linhasResposta.size();
        log.info("SeedDemonstracao: {} folhas, {} questões entregues e {} respostas gravadas.", linhasFolha.size(),
                linhasQuestao.size(), linhasResposta.size());
    }

    /** Desempenho por aluno, tópico e nível (RN05), só com as respostas já contabilizadas. */
    private void gravarDesempenho(List<Folha> folhas) {
        // [respostas, acertos] e a última contabilização de cada aluno, tópico e nível
        Map<String, int[]> contagem = new LinkedHashMap<>();
        Map<String, LocalDateTime> atualizado = new HashMap<>();
        Map<String, Object[]> chaves = new HashMap<>();
        for (Folha folha : folhas) {
            if (folha.contabilizada == null) {
                continue;
            }
            for (int q = 0; q < folha.questoes.size(); q++) {
                QuestaoDoBanco questao = folha.questoes.get(q);
                String chave = folha.aluno.id + "|" + questao.topicoId() + "|" + questao.nivel();
                int[] numeros = contagem.computeIfAbsent(chave, c -> new int[2]);
                numeros[0]++;
                if (folha.escolhidas.get(q) == questao.correta()) {
                    numeros[1]++;
                }
                atualizado.merge(chave, folha.contabilizada, (a, b) -> a.isAfter(b) ? a : b);
                chaves.putIfAbsent(chave, new Object[]{folha.aluno.id, questao.topicoId(), questao.nivel().name()});
            }
        }
        List<Object[]> linhas = new ArrayList<>();
        for (Map.Entry<String, int[]> entrada : contagem.entrySet()) {
            Object[] chave = chaves.get(entrada.getKey());
            int respostas = entrada.getValue()[0];
            int acertos = entrada.getValue()[1];
            BigDecimal percentual = BigDecimal.valueOf(acertos * 100L)
                    .divide(BigDecimal.valueOf(respostas), 2, RoundingMode.HALF_UP);
            linhas.add(new Object[]{chave[0], chave[1], chave[2], respostas, acertos, percentual,
                    atualizado.get(entrada.getKey())});
        }
        lote("insert into desempenho_topico (aluno_id, topico_id, nivel_dificuldade, total_respostas, total_acertos, "
                + "percentual_acerto, atualizado_em) values (?, ?, ?, ?, ?, ?, ?)", linhas);
    }

    /**
     * Auditoria no mesmo formato dos serviços: logins da última semana (painel do admin), criação das
     * atividades, votos nas questões e geração das práticas.
     */
    private void gravarAuditoria(List<Escola> escolas, List<AtividadePlanejada> atividades, List<Voto> votos,
                                 List<Folha> folhas, LocalDateTime agora) {
        List<Object[]> linhas = new ArrayList<>();
        for (Escola escola : escolas) {
            for (Professor professor : escola.professores) {
                if (professor.primeiroAcesso) {
                    continue;
                }
                int acessos = professor.email.equals(PROFESSORA_VITRINE) ? 6 : 1 + random.nextInt(4);
                for (int i = 0; i < acessos; i++) {
                    linhas.add(auditoria(professor.id, professor.nome, professor.email, escola.id, "LOGIN", null, null,
                            null, agora.minusMinutes(30 + random.nextInt(7 * 24 * 60 - 60))));
                }
            }
            for (Aluno aluno : escola.alunos) {
                if (aluno.primeiroAcesso) {
                    continue;
                }
                int acessos = aluno.vitrine ? 5 : random.nextDouble() < aluno.engajamento - 0.15 ? 1 : 0;
                for (int i = 0; i < acessos; i++) {
                    linhas.add(auditoria(aluno.id, aluno.nome, aluno.login, escola.id, "LOGIN", null, null, null,
                            agora.minusMinutes(30 + random.nextInt(7 * 24 * 60 - 60))));
                }
            }
        }
        for (AtividadePlanejada atividade : atividades) {
            Professor professor = atividade.turma.professor;
            linhas.add(auditoria(professor.id, professor.nome, professor.email, professor.escola.id, "CRIACAO_ATIVIDADE",
                    "Atividade", atividade.id, "turma " + atividade.turma.id + ", " + atividade.questoes.size()
                            + " questão(ões), prazo " + atividade.prazo.format(FORMATO_PRAZO), atividade.criada));
        }
        for (Voto voto : votos) {
            Professor professor = voto.professor();
            linhas.add(auditoria(professor.id, professor.nome, professor.email, professor.escola.id, "AVALIACAO_QUESTAO",
                    "Questao", voto.questao().id, voto.voto() + " -> " + voto.statusDepois(), voto.em()));
        }
        for (Folha folha : folhas) {
            if (folha.atividade != null) {
                continue;
            }
            Aluno aluno = folha.aluno;
            String detalhe = (folha.topicoId == null ? "recomendada" : "tópico " + folha.topicoId)
                    + ", " + folha.questoes.size() + " questão(ões)";
            linhas.add(auditoria(aluno.id, aluno.nome, null, aluno.turma.escola.id, "GERACAO_PRATICA_ADAPTATIVA",
                    "AtividadeAluno", folha.id, detalhe, folha.iniciada));
        }
        lote("insert into log_auditoria (usuario_id, usuario_nome, identificador_acesso, instituicao_id, acao, entidade, "
                + "entidade_id, sucesso, detalhes, criado_em) values (?, ?, ?, ?, ?, ?, ?, true, ?, ?)", linhas);
    }

    private static Object[] auditoria(long usuarioId, String nome, String identificador, long instituicaoId,
                                      String acao, String entidade, Long entidadeId, String detalhes,
                                      LocalDateTime quando) {
        return new Object[]{usuarioId, nome, identificador, instituicaoId, acao, entidade, entidadeId, detalhes, quando};
    }

    // ------------------------------------------------------------------ quadro do log

    private void montarResumo(List<Escola> escolas, Resumo resumo) {
        for (Escola escola : escolas) {
            int menor = Integer.MAX_VALUE;
            int maior = 0;
            for (Turma turma : escola.turmas) {
                menor = Math.min(menor, turma.alunos.size());
                maior = Math.max(maior, turma.alunos.size());
            }
            resumo.linhas.add("");
            resumo.linhas.add(escola.plano.nome() + ": " + escola.alunos.size() + " alunos em " + escola.turmas.size()
                    + " turmas (" + menor + " a " + maior + " alunos)");
            List<String> genericos = new ArrayList<>();
            for (Professor professor : escola.professores) {
                if (professor.primeiroAcesso) {
                    continue;
                }
                String turmas = professor.turmas.isEmpty() ? "sem turma" : professor.turmas.size() + " turmas";
                if (professor.admin) {
                    resumo.linhas.add(String.format("  %-24s administrador(a): gestão, painéis e auditoria (sem turma)",
                            professor.email));
                } else if (professor.email.equals(PROFESSORA_VITRINE)) {
                    resumo.linhas.add(String.format("  %-24s PROFESSORA VITRINE: %s, questões em todos os status "
                            + "e fila de avaliação", professor.email, String.join(", ", TURMAS_DA_PROFESSORA_VITRINE)));
                } else if (professor.email.startsWith("prof")) {
                    genericos.add(professor.email);
                } else {
                    resumo.linhas.add(String.format("  %-24s professor(a), %s", professor.email, turmas));
                }
            }
            if (!genericos.isEmpty()) {
                resumo.linhas.add("  " + genericos.get(0) + " … " + genericos.get(genericos.size() - 1) + "   professores");
            }
            List<String> exemplos = new ArrayList<>();
            for (Aluno aluno : escola.alunos) {
                if (aluno.vitrine) {
                    resumo.linhas.add(String.format("  %-24s ALUNO VITRINE (%s): atividades feitas, a fazer, em "
                            + "andamento e práticas por tópico", aluno.login, aluno.turma.nome));
                } else if (!aluno.primeiroAcesso && exemplos.size() < 3 && aluno.turma.alunos.indexOf(aluno) == 0) {
                    exemplos.add(aluno.login + " (" + aluno.turma.nome + ")");
                }
            }
            resumo.linhas.add("  alunos, por exemplo: " + String.join(", ", exemplos));
        }
        resumo.linhas.add("");
    }

    // ------------------------------------------------------------------ apoio

    private static Professor professorPorEmail(List<Escola> escolas, String email) {
        for (Escola escola : escolas) {
            for (Professor professor : escola.professores) {
                if (professor.email.equals(email)) {
                    return professor;
                }
            }
        }
        throw new IllegalStateException("Professor não encontrado: " + email);
    }

    private static Turma turmaPorNome(Escola escola, String nome) {
        for (Turma turma : escola.turmas) {
            if (turma.nome.equals(nome)) {
                return turma;
            }
        }
        throw new IllegalStateException("Turma não encontrada: " + nome);
    }

    private Map<String, Long> idsPorNome(String sql) {
        Map<String, Long> ids = new HashMap<>();
        for (Map<String, Object> linha : jdbc.queryForList(sql)) {
            ids.put((String) linha.get("nome"), ((Number) linha.get("id")).longValue());
        }
        return ids;
    }

    private List<QuestaoDoBanco> sortear(Map<String, Map<NivelDificuldade, List<QuestaoDoBanco>>> banco,
                                         String topico, int faceis, int medias, int dificeis) {
        List<QuestaoDoBanco> escolhidas = new ArrayList<>();
        escolhidas.addAll(sortearNoNivel(banco, topico, NivelDificuldade.FACIL, faceis));
        escolhidas.addAll(sortearNoNivel(banco, topico, NivelDificuldade.MEDIO, medias));
        escolhidas.addAll(sortearNoNivel(banco, topico, NivelDificuldade.DIFICIL, dificeis));
        return escolhidas;
    }

    private List<QuestaoDoBanco> sortearNoNivel(Map<String, Map<NivelDificuldade, List<QuestaoDoBanco>>> banco,
                                                String topico, NivelDificuldade nivel, int quantidade) {
        List<QuestaoDoBanco> disponiveis = new ArrayList<>(
                banco.getOrDefault(topico, Map.of()).getOrDefault(nivel, List.of()));
        Collections.shuffle(disponiveis, random);
        return new ArrayList<>(disponiveis.subList(0, Math.min(quantidade, disponiveis.size())));
    }

    private static void removerRepetidas(List<QuestaoDoBanco> questoes) {
        Set<Long> vistas = new HashSet<>();
        questoes.removeIf(questao -> !vistas.add(questao.id()));
    }

    /** Reserva ids da sequência (as tabelas usam BIGSERIAL), para gravar em lote já com as chaves. */
    private List<Long> reservar(String sequencia, int quantidade) {
        if (quantidade == 0) {
            return List.of();
        }
        return jdbc.queryForList("select nextval(cast(? as regclass)) from generate_series(1, ?)", Long.class,
                sequencia, quantidade);
    }

    private void lote(String sql, List<Object[]> linhas) {
        for (int inicio = 0; inicio < linhas.size(); inicio += LOTE) {
            jdbc.batchUpdate(sql, linhas.subList(inicio, Math.min(linhas.size(), inicio + LOTE)));
        }
    }

    private double gaussiana(double desvio) {
        return random.nextGaussian() * desvio;
    }
}
