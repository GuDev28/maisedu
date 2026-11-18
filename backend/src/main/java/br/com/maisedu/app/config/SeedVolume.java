package br.com.maisedu.app.config;

import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.NivelDificuldade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Perfil "demo-volume" (use junto com o dev: SPRING_PROFILES_ACTIVE=dev,demo-volume): cria uma
 * terceira escola fictícia de porte médio, com 30 turmas do 6º ao 9º ano, 13 professores, 1000 alunos
 * simulados e 8 semanas de atividades e práticas adaptativas. Serve aos painéis do professor e do
 * admin e à avaliação por simulação do capítulo de Resultados. Todos os dados são fictícios.
 *
 * Cada aluno tem um perfil de acerto (forte, médio ou fraco), um tópico mais difícil, uma variação por
 * turma, aprende um pouco a cada semana e melhora nos tópicos em que faz prática adaptativa. Nas
 * questões, um dos distratores atrai a maior parte dos erros (como um erro de raciocínio comum).
 *
 * Por ser muito volume, a gravação é feita em lote com JDBC (alguns segundos), e não pelos serviços.
 * O desempenho por tópico é calculado no fim, numa única consulta, a partir das respostas já
 * contabilizadas: as atividades vencidas estão fechadas e a última está dentro do prazo. Roda uma vez
 * só (se a escola já existe, não faz nada) e depois do cenário pequeno ({@link SeedDemonstracao}).
 */
@Slf4j
@Component
@Profile("demo-volume")
@Order(2)
public class SeedVolume implements CommandLineRunner {

    public static final String CNPJ = "33.333.333/0001-33";
    private static final int TOTAL_ALUNOS = 1000;
    private static final int LOTE = 2000;

    /** Dias atrás em que cada atividade regular foi criada (todas com prazo de 5 dias, já vencido). */
    private static final int[] DIAS_DAS_ATIVIDADES = {55, 48, 34, 27, 13, 6};
    private static final int DIAS_DE_PRAZO = 5;
    /** A atividade atual foi criada há 2 dias e vence em 3. */
    private static final int DIAS_ATIVIDADE_ATUAL = 2;

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
     * Dificuldade própria de cada tópico na escola (soma à probabilidade de acerto): deixa o mapa ano ×
     * tópico do painel com contraste, por exemplo Frações e Equações como pontos fracos da escola.
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
    private final Random random = new Random(2027L);

    public SeedVolume(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, PlatformTransactionManager transactionManager,
                      @Value("${maisedu.termos.versao-atual:1}") int versaoTermos) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.transacao = new TransactionTemplate(transactionManager);
        this.versaoTermos = versaoTermos;
    }

    // ------------------------------------------------------------------ modelo em memória

    private record QuestaoDoBanco(long id, String topico, NivelDificuldade nivel, long correta, List<Long> erradas,
                                  long erroComum) {
    }

    private static final class Turma {
        long id;
        String nome;
        AnoEscolar ano;
        long professorId;
        double ajuste;
        String topicoFracoDaTurma;
        final List<Aluno> alunos = new ArrayList<>();
    }

    private static final class Aluno {
        long id;
        String nome;
        String login;
        Turma turma;
        double base;
        double engajamento;
        final Map<String, Double> ajustePorTopico = new HashMap<>();
        final Map<String, Double> efeitoDaPratica = new HashMap<>();
    }

    /** Uma folha (atividade regular ou prática) a gravar, com as questões e o que o aluno escolheu. */
    private static final class Folha {
        long id;
        long alunoId;
        Long atividadeId;
        Long topicoId;
        LocalDateTime iniciada;
        LocalDateTime enviada;
        LocalDateTime contabilizada;
        final List<QuestaoDoBanco> questoes = new ArrayList<>();
        final List<Long> escolhidas = new ArrayList<>();
        boolean soMarcacao;
    }

    @Override
    public void run(String... args) {
        Integer existe = jdbc.queryForObject("select count(*) from instituicao where cnpj = ?", Integer.class, CNPJ);
        if (existe != null && existe > 0) {
            log.info("SeedVolume: a escola de volume já existe; nada foi criado.");
            return;
        }
        long inicio = System.currentTimeMillis();
        log.info("SeedVolume: criando a escola de volume (30 turmas, {} alunos, 8 semanas)...", TOTAL_ALUNOS);
        String[] contas = transacao.execute(status -> criar());
        log.warn("""

                ================= MaisEdu — escola de volume (perfil demo-volume) =================
                Escola Estadual Portal do Saber (fictícia): 30 turmas, 13 professores, {} alunos
                Senha de todas as contas: {}
                  {}   (administrador + professor)
                  {}   (professor de 3 turmas)
                  alunos, por exemplo: {}
                Criada em {} s.
                ===================================================================================
                """, TOTAL_ALUNOS, SeedDemonstracao.SENHA_DEMO, contas[0], contas[1], contas[2],
                (System.currentTimeMillis() - inicio) / 1000);
    }

    private String[] criar() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicioDoPeriodo = agora.minusDays(60);
        String hash = passwordEncoder.encode(SeedDemonstracao.SENHA_DEMO);
        Map<String, Long> roles = new HashMap<>();
        for (Map<String, Object> linha : jdbc.queryForList("select id, nome from role")) {
            roles.put((String) linha.get("nome"), ((Number) linha.get("id")).longValue());
        }
        Map<String, Long> topicos = new HashMap<>();
        for (Map<String, Object> linha : jdbc.queryForList("select id, nome from topico")) {
            topicos.put((String) linha.get("nome"), ((Number) linha.get("id")).longValue());
        }

        long escola = reservar("instituicao_id_seq", 1).get(0);
        jdbc.update("insert into instituicao (id, nome, cnpj, endereco, ativo, criado_em) values (?, ?, ?, ?, true, ?)",
                escola, "Escola Estadual Portal do Saber (fictícia)", CNPJ, "Rua do Conhecimento, 300 (fictício)",
                inicioDoPeriodo.minusDays(30));

        // ---------------- professores (o admin também é professor)
        List<Long> professores = reservar("usuario_id_seq", 13);
        List<Object[]> usuarios = new ArrayList<>();
        List<Object[]> papeis = new ArrayList<>();
        String emailAdmin = "admin.c@maisedu.dev";
        for (int i = 0; i < professores.size(); i++) {
            long id = professores.get(i);
            String email = i == 0 ? emailAdmin : String.format("prof%02d.c@maisedu.dev", i);
            String nome = i == 0 ? "Sônia Prado" : PRENOMES[(i * 7) % PRENOMES.length] + " "
                    + SOBRENOMES[(i * 5) % SOBRENOMES.length];
            usuarios.add(new Object[]{id, nome, email, null, null, hash, escola, inicioDoPeriodo.minusDays(20),
                    versaoTermos, inicioDoPeriodo.minusDays(20)});
            papeis.add(new Object[]{id, roles.get("PROFESSOR")});
            if (i == 0) {
                papeis.add(new Object[]{id, roles.get("ADMIN")});
            }
        }

        // ---------------- turmas: 6º A-H, 7º A-H, 8º A-G, 9º A-G
        List<Turma> turmas = new ArrayList<>();
        Object[][] series = {{AnoEscolar.SEXTO_ANO, "6º", 8}, {AnoEscolar.SETIMO_ANO, "7º", 8},
                {AnoEscolar.OITAVO_ANO, "8º", 7}, {AnoEscolar.NONO_ANO, "9º", 7}};
        for (Object[] serie : series) {
            for (int i = 0; i < (int) serie[2]; i++) {
                Turma turma = new Turma();
                turma.ano = (AnoEscolar) serie[0];
                turma.nome = serie[1] + " ano " + (char) ('A' + i);
                turma.ajuste = gaussiana(0.05);
                List<String> doAno = TOPICOS_DO_ANO.get(turma.ano);
                turma.topicoFracoDaTurma = random.nextDouble() < 0.4 ? doAno.get(random.nextInt(doAno.size())) : null;
                turmas.add(turma);
            }
        }
        List<Long> idsTurmas = reservar("turma_id_seq", turmas.size());
        List<Object[]> linhasTurma = new ArrayList<>();
        List<Object[]> vinculosProfessor = new ArrayList<>();
        for (int i = 0; i < turmas.size(); i++) {
            Turma turma = turmas.get(i);
            turma.id = idsTurmas.get(i);
            // admin leciona em 2 turmas; os outros 12 professores, em 2 ou 3 turmas cada
            turma.professorId = i < 2 ? professores.get(0) : professores.get(1 + ((i - 2) % 12));
            linhasTurma.add(new Object[]{turma.id, turma.nome, escola, turma.ano.name(), inicioDoPeriodo.minusDays(10)});
            vinculosProfessor.add(new Object[]{turma.professorId, turma.id});
        }

        // ---------------- alunos
        List<Long> idsAlunos = reservar("usuario_id_seq", TOTAL_ALUNOS);
        Set<String> logins = new HashSet<>(jdbc.queryForList("select login from usuario where login is not null",
                String.class));
        List<Aluno> alunos = new ArrayList<>();
        List<Object[]> vinculosAluno = new ArrayList<>();
        for (int i = 0; i < TOTAL_ALUNOS; i++) {
            Aluno aluno = new Aluno();
            aluno.id = idsAlunos.get(i);
            aluno.turma = turmas.get(i % turmas.size());
            aluno.turma.alunos.add(aluno);
            aluno.nome = PRENOMES[random.nextInt(PRENOMES.length)] + " " + SOBRENOMES[random.nextInt(SOBRENOMES.length)]
                    + " " + SOBRENOMES[random.nextInt(SOBRENOMES.length)];
            do {
                aluno.login = "a" + (100000 + random.nextInt(900000));
            } while (!logins.add(aluno.login));
            double perfil = random.nextDouble();
            aluno.base = perfil < 0.30 ? 0.80 : perfil < 0.75 ? 0.62 : 0.43;
            aluno.engajamento = 0.55 + random.nextDouble() * 0.43;
            List<String> doAno = TOPICOS_DO_ANO.get(aluno.turma.ano);
            String maisDificil = doAno.get(random.nextInt(doAno.size()));
            for (String topico : topicos.keySet()) {
                double ajuste = gaussiana(0.07);
                if (topico.equals(maisDificil)) {
                    ajuste -= 0.18;
                }
                if (topico.equals(aluno.turma.topicoFracoDaTurma)) {
                    ajuste -= 0.10;
                }
                aluno.ajustePorTopico.put(topico, ajuste);
            }
            alunos.add(aluno);
            usuarios.add(new Object[]{aluno.id, aluno.nome, null, aluno.login,
                    "responsavel." + aluno.login + "@maisedu.dev", hash, escola, inicioDoPeriodo.minusDays(5),
                    versaoTermos, inicioDoPeriodo.minusDays(5)});
            papeis.add(new Object[]{aluno.id, roles.get("ALUNO")});
            vinculosAluno.add(new Object[]{aluno.id, aluno.turma.id});
        }
        lote("insert into usuario (id, nome, email, login, email_responsavel, senha, instituicao_id, ativo, criado_em, "
                + "tentativas_falhas, senha_temporaria, versao_sessao, termos_versao, termos_aceitos_em) "
                + "values (?, ?, ?, ?, ?, ?, ?, true, ?, 0, false, 0, ?, ?)", usuarios);
        lote("insert into usuario_role (usuario_id, role_id) values (?, ?)", papeis);
        lote("insert into turma (id, nome, instituicao_id, ano_escolar, ativo, criado_em) values (?, ?, ?, ?, true, ?)",
                linhasTurma);
        lote("insert into professor_turma (professor_id, turma_id) values (?, ?)", vinculosProfessor);
        lote("insert into aluno_turma (aluno_id, turma_id) values (?, ?)", vinculosAluno);

        // ---------------- questões de autoria da escola (aprovadas por 3 colegas, algumas na fila e rejeitadas)
        criarQuestoes(professores, topicos, inicioDoPeriodo);
        Map<String, Map<NivelDificuldade, List<QuestaoDoBanco>>> banco = carregarBanco();

        // ---------------- atividades (6 vencidas e 1 dentro do prazo por turma)
        List<AtividadePlanejada> planejadas = new ArrayList<>();
        List<Object[]> atividades = new ArrayList<>();
        List<Object[]> questoesDasAtividades = new ArrayList<>();
        List<Long> idsAtividades = reservar("atividade_id_seq", turmas.size() * (DIAS_DAS_ATIVIDADES.length + 1));
        int proximaAtividade = 0;
        for (Turma turma : turmas) {
            List<String> doAno = TOPICOS_DO_ANO.get(turma.ano);
            for (int a = 0; a <= DIAS_DAS_ATIVIDADES.length; a++) {
                boolean atual = a == DIAS_DAS_ATIVIDADES.length;
                LocalDateTime criada = agora.minusDays(atual ? DIAS_ATIVIDADE_ATUAL : DIAS_DAS_ATIVIDADES[a])
                        .withHour(8 + random.nextInt(4)).withMinute(random.nextInt(60));
                LocalDateTime prazo = criada.plusDays(DIAS_DE_PRAZO).withHour(23).withMinute(59);
                String t1 = doAno.get(a % doAno.size());
                String t2 = doAno.get((a + 1) % doAno.size());
                List<QuestaoDoBanco> questoes = new ArrayList<>();
                questoes.addAll(sortear(banco, t1, 2, 1, 1));
                questoes.addAll(sortear(banco, t2, 2, 1, 1));
                AtividadePlanejada atividade = new AtividadePlanejada(idsAtividades.get(proximaAtividade++), turma,
                        criada, prazo, atual, questoes);
                planejadas.add(atividade);
                atividades.add(new Object[]{atividade.id(), "Lista " + (a + 1) + ": " + t1 + " · " + t2, turma.id,
                        turma.professorId, criada, prazo, atual ? null : prazo.plusMinutes(1)});
                for (int q = 0; q < questoes.size(); q++) {
                    questoesDasAtividades.add(new Object[]{atividade.id(), questoes.get(q).id(), q + 1});
                }
            }
        }

        // ---------------- folhas de cada aluno, em ordem de data: a prática adaptativa melhora o aluno
        // no tópico praticado, e isso aparece nas atividades seguintes (efeito da prática no painel)
        List<Folha> folhas = new ArrayList<>();
        for (Aluno aluno : alunos) {
            List<Evento> eventos = new ArrayList<>();
            for (AtividadePlanejada atividade : planejadas) {
                if (atividade.turma() != aluno.turma) {
                    continue;
                }
                double participa = atividade.atual() ? aluno.engajamento - 0.30 : aluno.engajamento;
                if (random.nextDouble() <= participa) {
                    eventos.add(new Evento(atividade.criada().plusMinutes(random.nextInt(
                            atividade.atual() ? 36 * 60 : 96 * 60)), atividade));
                }
            }
            double sorteio = random.nextDouble() * aluno.engajamento;
            int praticas = sorteio < 0.25 ? 0 : sorteio < 0.50 ? 1 : sorteio < 0.70 ? 2 : 3;
            for (int p = 0; p < praticas; p++) {
                eventos.add(new Evento(agora.minusDays(3 + random.nextInt(48)).withHour(14 + random.nextInt(7))
                        .withMinute(random.nextInt(60)), null));
            }
            eventos.sort((a, b) -> a.quando().compareTo(b.quando()));

            for (Evento evento : eventos) {
                Folha folha = evento.atividade() == null
                        ? praticar(aluno, evento.quando(), banco, topicos, inicioDoPeriodo)
                        : responderAtividade(aluno, evento, inicioDoPeriodo);
                if (folha != null) {
                    folhas.add(folha);
                }
            }
        }

        // ---------------- gravação em lote
        lote("insert into atividade (id, titulo, turma_id, professor_id, criado_em, prazo, fechada_em) "
                + "values (?, ?, ?, ?, ?, ?, ?)", atividades);
        lote("insert into atividade_questao (atividade_id, questao_id, ordem) values (?, ?, ?)", questoesDasAtividades);
        gravarFolhas(folhas);

        // ---------------- desempenho por tópico e nível (RN05), a partir das respostas contabilizadas
        jdbc.update("""
                insert into desempenho_topico (aluno_id, topico_id, nivel_dificuldade, total_respostas, total_acertos,
                                               percentual_acerto, atualizado_em)
                select aa.aluno_id, q.topico_id, q.nivel_dificuldade, count(*),
                       sum(case when alt.correta then 1 else 0 end),
                       round(100.0 * sum(case when alt.correta then 1 else 0 end) / count(*), 2),
                       max(r.contabilizada_em)
                  from resposta r
                  join alternativa alt on alt.id = r.alternativa_id
                  join atividade_aluno_questao aaq on aaq.id = r.atividade_aluno_questao_id
                  join atividade_aluno aa on aa.id = aaq.atividade_aluno_id
                  join questao q on q.id = aaq.questao_id
                  join usuario u on u.id = aa.aluno_id
                 where u.instituicao_id = ? and r.contabilizada_em is not null
                 group by aa.aluno_id, q.topico_id, q.nivel_dificuldade
                """, escola);

        // ---------------- logins da última semana (painel do admin)
        List<Object[]> logs = new ArrayList<>();
        for (Aluno aluno : alunos) {
            if (random.nextDouble() < aluno.engajamento - 0.15) {
                logs.add(new Object[]{aluno.id, aluno.nome, aluno.login, escola,
                        agora.minusMinutes(random.nextInt(7 * 24 * 60))});
            }
        }
        lote("insert into log_auditoria (usuario_id, usuario_nome, identificador_acesso, instituicao_id, acao, "
                + "sucesso, criado_em) values (?, ?, ?, ?, 'LOGIN', true, ?)", logs);

        Aluno exemplo = turmas.get(2).alunos.get(0);
        return new String[]{emailAdmin, "prof01.c@maisedu.dev",
                exemplo.login + ", " + turmas.get(10).alunos.get(1).login + ", " + turmas.get(25).alunos.get(2).login};
    }

    // ------------------------------------------------------------------ respostas simuladas

    private record AtividadePlanejada(long id, Turma turma, LocalDateTime criada, LocalDateTime prazo, boolean atual,
                                      List<QuestaoDoBanco> questoes) {
    }

    /** Uma folha a simular na data: atividade regular ou, com {@code atividade} nula, prática adaptativa. */
    private record Evento(LocalDateTime quando, AtividadePlanejada atividade) {
    }

    /** Folha de uma atividade regular: a maioria envia; alguns só marcam e não enviam até o prazo. */
    private Folha responderAtividade(Aluno aluno, Evento evento, LocalDateTime inicioDoPeriodo) {
        AtividadePlanejada atividade = evento.atividade();
        Folha folha = new Folha();
        folha.alunoId = aluno.id;
        folha.atividadeId = atividade.id();
        folha.iniciada = evento.quando();
        folha.questoes.addAll(atividade.questoes());
        if (random.nextDouble() < (atividade.atual() ? 0.20 : 0.05)) {
            folha.soMarcacao = true;
        } else {
            folha.enviada = folha.iniciada.plusMinutes(10 + random.nextInt(35));
            folha.contabilizada = atividade.atual() ? null : atividade.prazo().plusMinutes(1);
        }
        escolher(aluno, folha, folha.iniciada, inicioDoPeriodo);
        return folha;
    }

    /**
     * Prática adaptativa no tópico mais fraco do aluno naquele momento: a recomendada (10 questões, com
     * um segundo tópico) ou a do tópico escolhido (5 questões). Depois dela, o aluno melhora no tópico.
     */
    private Folha praticar(Aluno aluno, LocalDateTime quando, Map<String, Map<NivelDificuldade, List<QuestaoDoBanco>>> banco,
                           Map<String, Long> topicos, LocalDateTime inicioDoPeriodo) {
        List<String> doAno = TOPICOS_DO_ANO.get(aluno.turma.ano);
        String topico = topicoMaisFraco(aluno, doAno);
        boolean doTopico = random.nextDouble() < 0.4;
        Folha folha = new Folha();
        folha.alunoId = aluno.id;
        folha.topicoId = doTopico ? topicos.get(topico) : null;
        folha.iniciada = quando;
        folha.enviada = quando.plusMinutes(8 + random.nextInt(20));
        folha.contabilizada = folha.enviada;
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
        aluno.efeitoDaPratica.merge(topico, 0.05, (atual, mais) -> Math.min(0.20, atual + mais));
        return folha;
    }

    /** Probabilidade de acerto do aluno no tópico e nível, na data (aprende um pouco por semana). */
    private double habilidade(Aluno aluno, String topico, NivelDificuldade nivel, LocalDateTime quando,
                              LocalDateTime inicioDoPeriodo) {
        double semanas = java.time.Duration.between(inicioDoPeriodo, quando).toDays() / 7.0;
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

    private void gravarFolhas(List<Folha> folhas) {
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
            linhasFolha.add(new Object[]{folha.id, folha.alunoId, folha.atividadeId,
                    folha.atividadeId == null ? "ADAPTATIVA" : "REGULAR", folha.topicoId, folha.iniciada, folha.enviada});
            for (int q = 0; q < folha.questoes.size(); q++) {
                long idQuestaoEntregue = idsQuestoes.get(proxima++);
                Long marcada = folha.soMarcacao && random.nextDouble() < 0.6 ? folha.escolhidas.get(q) : null;
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
        log.info("SeedVolume: {} folhas, {} questões entregues e {} respostas gravadas.", linhasFolha.size(),
                linhasQuestao.size(), linhasResposta.size());
    }

    // ------------------------------------------------------------------ banco de questões

    private void criarQuestoes(List<Long> professores, Map<String, Long> topicos, LocalDateTime inicioDoPeriodo) {
        List<GeradorQuestoesDemo.QuestaoDemo> aprovadas = new GeradorQuestoesDemo(99L).gerar(10);
        List<GeradorQuestoesDemo.QuestaoDemo> naFila = new GeradorQuestoesDemo(123L).gerar(1).subList(0, 6);
        int total = aprovadas.size() + naFila.size();
        List<Long> ids = reservar("questao_id_seq", total);
        List<Object[]> questoes = new ArrayList<>();
        List<Object[]> alternativas = new ArrayList<>();
        List<Object[]> votos = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            boolean aprovada = i < aprovadas.size();
            GeradorQuestoesDemo.QuestaoDemo demo = aprovada ? aprovadas.get(i) : naFila.get(i - aprovadas.size());
            long id = ids.get(i);
            int autor = 1 + (i % 12);
            boolean rejeitada = !aprovada && i - aprovadas.size() >= 4;
            String status = aprovada ? "APROVADA" : rejeitada ? "REJEITADA" : "PENDENTE";
            LocalDateTime criada = inicioDoPeriodo.minusDays(10).plusHours(i);
            questoes.add(new Object[]{id, demo.enunciado(), professores.get(autor), status, criada,
                    topicos.get(demo.topico()), demo.nivel().name(), demo.anoEscolar().name()});
            for (int a = 0; a < demo.alternativas().size(); a++) {
                alternativas.add(new Object[]{id, demo.alternativas().get(a), a == demo.indiceCorreta(), a + 1});
            }
            int votosDaQuestao = aprovada || rejeitada ? 3 : 1;
            for (int v = 1; v <= votosDaQuestao; v++) {
                long avaliador = professores.get(((autor - 1 + v) % 12) + 1);
                votos.add(new Object[]{id, avaliador, rejeitada ? "REJEITAR" : "APROVAR", criada.plusDays(v),
                        rejeitada ? "Gabarito precisa ser revisto: confira a conta da alternativa correta." : null});
            }
        }
        lote("insert into questao (id, enunciado, autor_id, status, criado_em, topico_id, nivel_dificuldade, ano_escolar) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?)", questoes);
        lote("insert into alternativa (questao_id, texto, correta, ordem) values (?, ?, ?, ?)", alternativas);
        lote("insert into avaliacao_questao (questao_id, professor_id, voto, avaliado_em, comentario) "
                + "values (?, ?, ?, ?, ?)", votos);
    }

    /** Todas as questões aprovadas (das três escolas), por tópico e nível, com as alternativas. */
    private Map<String, Map<NivelDificuldade, List<QuestaoDoBanco>>> carregarBanco() {
        Map<Long, Object[]> dados = new LinkedHashMap<>();
        Map<Long, List<Long>> erradas = new HashMap<>();
        Map<Long, Long> corretas = new HashMap<>();
        for (Map<String, Object> linha : jdbc.queryForList("""
                select q.id, t.nome as topico, q.nivel_dificuldade as nivel, a.id as alternativa, a.correta
                  from questao q join topico t on t.id = q.topico_id join alternativa a on a.questao_id = q.id
                 where q.status = 'APROVADA' order by q.id, a.ordem""")) {
            long id = ((Number) linha.get("id")).longValue();
            dados.putIfAbsent(id, new Object[]{linha.get("topico"), NivelDificuldade.valueOf((String) linha.get("nivel"))});
            long alternativa = ((Number) linha.get("alternativa")).longValue();
            if (Boolean.TRUE.equals(linha.get("correta"))) {
                corretas.put(id, alternativa);
            } else {
                erradas.computeIfAbsent(id, k -> new ArrayList<>()).add(alternativa);
            }
        }
        Map<String, Map<NivelDificuldade, List<QuestaoDoBanco>>> banco = new HashMap<>();
        for (Map.Entry<Long, Object[]> entrada : dados.entrySet()) {
            long id = entrada.getKey();
            List<Long> distratores = erradas.getOrDefault(id, List.of());
            if (!corretas.containsKey(id) || distratores.isEmpty()) {
                continue;
            }
            String topico = (String) entrada.getValue()[0];
            NivelDificuldade nivel = (NivelDificuldade) entrada.getValue()[1];
            long erroComum = distratores.get((int) (id % distratores.size()));
            banco.computeIfAbsent(topico, t -> new EnumMap<>(NivelDificuldade.class))
                    .computeIfAbsent(nivel, n -> new ArrayList<>())
                    .add(new QuestaoDoBanco(id, topico, nivel, corretas.get(id), distratores, erroComum));
        }
        return banco;
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
        java.util.Collections.shuffle(disponiveis, random);
        return new ArrayList<>(disponiveis.subList(0, Math.min(quantidade, disponiveis.size())));
    }

    private static void removerRepetidas(List<QuestaoDoBanco> questoes) {
        Set<Long> vistas = new HashSet<>();
        questoes.removeIf(questao -> !vistas.add(questao.id()));
    }

    // ------------------------------------------------------------------ apoio JDBC

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
