package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.AvaliacaoQuestaoRequest;
import br.com.maisedu.app.dto.AvaliacaoQuestaoResponse;
import br.com.maisedu.app.dto.CadastroAlunoRequest;
import br.com.maisedu.app.dto.CadastroProfessorRequest;
import br.com.maisedu.app.dto.CriarQuestaoRequest;
import br.com.maisedu.app.dto.LoginRequest;
import br.com.maisedu.app.dto.QuestaoResponse;
import br.com.maisedu.app.dto.TrocarSenhaRequest;
import br.com.maisedu.app.dto.UsuarioCadastradoResponse;
import br.com.maisedu.app.dto.UsuarioResumoResponse;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Topico;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.model.VotoAvaliacao;
import br.com.maisedu.app.repository.AvaliacaoQuestaoRepository;
import br.com.maisedu.app.repository.InstituicaoRepository;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.RoleRepository;
import br.com.maisedu.app.repository.TopicoRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.service.ResendEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Infraestrutura comum dos testes de integracao: cliente HTTP, contas de usuario e helpers.
 *
 * As classes filhas sobem a aplicacao inteira com @SpringBootTest(RANDOM_PORT), e cada requisicao
 * percorre o caminho real: HTTP -> filtro JWT/Spring Security -> Controller -> Service -> Repository -> H2.
 *
 * Unico componente mockado: ResendEmailService, porque ele chama a API externa do Resend.
 * O mock tambem serve para ler a senha provisoria do professor, que no sistema so sai por e-mail.
 */
abstract class IntegracaoTestBase {

    /** Senha definitiva usada por todas as contas depois da troca da senha provisoria. */
    protected static final String SENHA_DEFINITIVA = "SenhaDeTeste#2026";

    @LocalServerPort
    private int port;

    @Value("${maisedu.jwt.cookie-nome}")
    protected String nomeCookie;

    @Autowired
    protected InstituicaoRepository instituicaoRepository;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected TopicoRepository topicoRepository;

    @Autowired
    protected QuestaoRepository questaoRepository;

    @Autowired
    protected AvaliacaoQuestaoRepository avaliacaoQuestaoRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @MockitoBean
    protected ResendEmailService emailService;

    protected RestClient client;

    /** Usuario que ja trocou a senha provisoria, com o token JWT do ultimo login. */
    protected record Conta(Long id, String nome, String identificador, String token) {
    }

    /** Usuario recem-cadastrado, ainda com a senha provisoria. */
    protected record NovoCadastro(Long id, String nome, String identificador, String senhaProvisoria) {
    }

    @BeforeEach
    void configurarCliente() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                // Nao lanca excecao em 4xx/5xx: os cenarios de erro sao verificados com assercoes
                .defaultStatusHandler(status -> true, (req, res) -> {})
                .build();
    }

    // ---------- Contas ----------

    /**
     * Cria uma instituicao e o administrador dela direto no banco. No MaisEdu esse provisionamento
     * nao tem endpoint (e o registro inicial do cliente, como faz o SeedInicial). Todo o resto
     * (cadastro de professor e aluno, login, troca de senha) passa pelos endpoints reais.
     */
    protected Conta criarAdmin() {
        long ts = System.nanoTime();
        Instituicao instituicao = instituicaoRepository.save(
                new Instituicao("Escola Teste " + ts, "TESTE-" + ts, "Rua dos Testes, 100"));

        String nome = "Admin " + ts;
        String email = "admin." + ts + "@maisedu.test";
        String hash = passwordEncoder.encode(SENHA_DEFINITIVA);

        Usuario admin = new Usuario(nome, email, hash, instituicao);
        // Igual ao SeedInicial: o admin acumula o papel de professor
        admin.adicionarRole(buscarRole(RoleNome.ADMIN));
        admin.adicionarRole(buscarRole(RoleNome.PROFESSOR));
        // Marca o primeiro acesso como ja feito (senhaTemporaria = false e termos vigentes aceitos)
        admin.trocarSenha(hash);
        admin.aceitarTermos(1);
        Usuario salvo = usuarioRepository.save(admin);

        return new Conta(salvo.getId(), nome, email, login(email, SENHA_DEFINITIVA));
    }

    /** POST /professores como admin. A senha provisoria e lida do e-mail entregue ao mock do Resend. */
    protected NovoCadastro cadastrarProfessorSemAtivar(Conta admin) {
        long ts = System.nanoTime();
        String nome = "Professor " + ts;
        String email = "prof." + ts + "@maisedu.test";

        ResponseEntity<UsuarioCadastradoResponse> response = client.post().uri("/professores")
                .header(HttpHeaders.COOKIE, cookie(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CadastroProfessorRequest(nome, email))
                .retrieve()
                .toEntity(UsuarioCadastradoResponse.class);
        assertThat(response.getStatusCode()).as("cadastro do professor (arrange)").isEqualTo(HttpStatus.CREATED);
        Long id = response.getBody().id();

        ArgumentCaptor<String> senhaProvisoria = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarSenhaProvisoria(eq(id), eq(nome), eq(email), senhaProvisoria.capture());

        return new NovoCadastro(id, nome, email, senhaProvisoria.getValue());
    }

    /** POST /alunos como professor. A senha provisoria do aluno volta na propria resposta. */
    protected NovoCadastro cadastrarAlunoSemAtivar(Conta professor) {
        long ts = System.nanoTime();
        String nome = "Aluno " + ts;
        String loginAluno = "aluno." + ts;

        ResponseEntity<UsuarioCadastradoResponse> response = client.post().uri("/alunos")
                .header(HttpHeaders.COOKIE, cookie(professor))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CadastroAlunoRequest(nome, loginAluno))
                .retrieve()
                .toEntity(UsuarioCadastradoResponse.class);
        assertThat(response.getStatusCode()).as("cadastro do aluno (arrange)").isEqualTo(HttpStatus.CREATED);

        UsuarioCadastradoResponse body = response.getBody();
        return new NovoCadastro(body.id(), nome, loginAluno, body.senhaProvisoria());
    }

    /** Faz o primeiro acesso: login com a senha provisoria, troca de senha e novo login. */
    protected Conta ativarConta(NovoCadastro cadastro) {
        String tokenProvisorio = login(cadastro.identificador(), cadastro.senhaProvisoria());

        ResponseEntity<Void> troca = client.post().uri("/auth/trocar-senha")
                .header(HttpHeaders.COOKIE, cookie(tokenProvisorio))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TrocarSenhaRequest(cadastro.senhaProvisoria(), SENHA_DEFINITIVA, true))
                .retrieve()
                .toBodilessEntity();
        assertThat(troca.getStatusCode()).as("troca da senha provisoria (arrange)").isEqualTo(HttpStatus.NO_CONTENT);

        String token = login(cadastro.identificador(), SENHA_DEFINITIVA);
        return new Conta(cadastro.id(), cadastro.nome(), cadastro.identificador(), token);
    }

    protected Conta cadastrarProfessor(Conta admin) {
        return ativarConta(cadastrarProfessorSemAtivar(admin));
    }

    protected Conta cadastrarAluno(Conta professor) {
        return ativarConta(cadastrarAlunoSemAtivar(professor));
    }

    /** POST /auth/login esperando sucesso; devolve o token lido do cookie de sessao. */
    protected String login(String identificador, String senha) {
        ResponseEntity<UsuarioResumoResponse> response = client.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(identificador, senha))
                .retrieve()
                .toEntity(UsuarioResumoResponse.class);
        assertThat(response.getStatusCode()).as("login de %s (arrange)", identificador).isEqualTo(HttpStatus.OK);
        return extrairToken(response);
    }

    /** Le o valor do cookie de sessao no header Set-Cookie. */
    protected String extrairToken(ResponseEntity<?> response) {
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).as("cookie de sessao no Set-Cookie").startsWith(nomeCookie + "=");
        int fimDoValor = setCookie.indexOf(';');
        return setCookie.substring(nomeCookie.length() + 1, fimDoValor);
    }

    /** Valor do header Cookie: o JWT do MaisEdu viaja no cookie HttpOnly, nao no header Authorization. */
    protected String cookie(Conta conta) {
        return cookie(conta.token());
    }

    protected String cookie(String token) {
        return nomeCookie + "=" + token;
    }

    private Role buscarRole(RoleNome nome) {
        return roleRepository.findByNome(nome)
                .orElseThrow(() -> new IllegalStateException("Role " + nome + " ausente (ver seed-teste.sql)"));
    }

    // ---------- Questoes e avaliacoes ----------

    protected Topico topicoQualquer() {
        List<Topico> topicos = topicoRepository.findAll();
        assertThat(topicos).as("topicos do seed-teste.sql").isNotEmpty();
        return topicos.get(0);
    }

    /** Questao valida: 5 alternativas, a correta e a de indice 2 (letra C). */
    protected CriarQuestaoRequest buildQuestaoRequest(String enunciado, Long topicoId) {
        return new CriarQuestaoRequest(
                enunciado,
                topicoId,
                NivelDificuldade.MEDIO,
                AnoEscolar.SEXTO_ANO,
                List.of("1/2", "3/4", "1", "5/4", "2"),
                2);
    }

    /** POST /questoes esperando 201; devolve o id da questao (status RASCUNHO). */
    protected Long criarQuestao(Conta professor, String enunciado) {
        ResponseEntity<QuestaoResponse> response = client.post().uri("/questoes")
                .header(HttpHeaders.COOKIE, cookie(professor))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildQuestaoRequest(enunciado, topicoQualquer().getId()))
                .retrieve()
                .toEntity(QuestaoResponse.class);
        assertThat(response.getStatusCode()).as("criacao da questao (arrange)").isEqualTo(HttpStatus.CREATED);
        return response.getBody().id();
    }

    /** Cria a questao e a envia para validacao (status PENDENTE). */
    protected Long criarQuestaoPendente(Conta autor, String enunciado) {
        Long questaoId = criarQuestao(autor, enunciado);
        ResponseEntity<QuestaoResponse> envio = client.post()
                .uri("/questoes/" + questaoId + "/enviar-para-validacao")
                .header(HttpHeaders.COOKIE, cookie(autor))
                .retrieve()
                .toEntity(QuestaoResponse.class);
        assertThat(envio.getStatusCode()).as("envio para validacao (arrange)").isEqualTo(HttpStatus.OK);
        return questaoId;
    }

    /** POST /questoes/{id}/avaliacoes esperando 200. */
    protected AvaliacaoQuestaoResponse votar(Conta professor, Long questaoId, VotoAvaliacao voto) {
        ResponseEntity<AvaliacaoQuestaoResponse> response = client.post()
                .uri("/questoes/" + questaoId + "/avaliacoes")
                .header(HttpHeaders.COOKIE, cookie(professor))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AvaliacaoQuestaoRequest(voto))
                .retrieve()
                .toEntity(AvaliacaoQuestaoResponse.class);
        assertThat(response.getStatusCode()).as("voto %s (arrange)", voto).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    /** GET em uma lista de questoes (/questoes/minhas ou /questoes/pendentes) esperando 200. */
    protected List<QuestaoResponse> listarQuestoes(Conta professor, String uri) {
        ResponseEntity<List<QuestaoResponse>> response = client.get().uri(uri)
                .header(HttpHeaders.COOKIE, cookie(professor))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});
        assertThat(response.getStatusCode()).as("GET %s", uri).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    protected List<Long> idsDe(List<QuestaoResponse> questoes) {
        List<Long> ids = new ArrayList<>();
        for (QuestaoResponse questao : questoes) {
            ids.add(questao.id());
        }
        return ids;
    }
}
