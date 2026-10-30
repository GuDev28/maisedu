package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.AlternativaResponse;
import br.com.maisedu.app.dto.CriarQuestaoRequest;
import br.com.maisedu.app.dto.QuestaoResponse;
import br.com.maisedu.app.exception.ErroResponse;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.StatusQuestao;
import br.com.maisedu.app.model.Topico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integracao do QuestaoController (RN2 - banco de questoes e envio para validacao).
 *
 * Fluxo real exercitado em cada teste:
 * RestClient -> HTTP real -> JwtAuthenticationFilter/Spring Security -> QuestaoController
 * -> QuestaoService -> QuestaoRepository -> H2 em memoria
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class QuestaoControllerIntegrationTest extends IntegracaoTestBase {

    private static final String BASE_PATH = "/questoes";

    private Conta admin;
    private Conta autor;

    @BeforeEach
    void criarContas() {
        admin = criarAdmin();
        autor = cadastrarProfessor(admin);
    }

    // ---------- POST /questoes ----------

    @Test
    void criar_comDadosValidos_deveRetornar201ComQuestaoEmRascunho() {
        Topico topico = topicoQualquer();
        CriarQuestaoRequest request = buildQuestaoRequest("Quanto e 3/4 + 1/4?", topico.getId());

        ResponseEntity<QuestaoResponse> response = client.post().uri(BASE_PATH)
                .header(HttpHeaders.COOKIE, cookie(autor))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(QuestaoResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        QuestaoResponse body = response.getBody();
        assertThat(body.id()).isNotNull();
        assertThat(body.enunciado()).isEqualTo("Quanto e 3/4 + 1/4?");
        assertThat(body.topico()).isEqualTo(topico.getNome());
        assertThat(body.nivelDificuldade()).isEqualTo(NivelDificuldade.MEDIO);
        assertThat(body.status()).isEqualTo(StatusQuestao.RASCUNHO);
        assertThat(body.autorNome()).isEqualTo(autor.nome());
        assertThat(body.criadoEm()).isNotNull();

        // As 5 alternativas voltam na ordem A..E e so a de indice 2 (C) e a correta
        List<AlternativaResponse> alternativas = body.alternativas();
        assertThat(alternativas).hasSize(5);
        for (int i = 0; i < alternativas.size(); i++) {
            AlternativaResponse alternativa = alternativas.get(i);
            assertThat(alternativa.id()).isNotNull();
            assertThat(alternativa.letra()).isEqualTo((char) ('A' + i));
            assertThat(alternativa.texto()).isEqualTo(request.alternativas().get(i));
            assertThat(alternativa.correta()).isEqualTo(i == 2);
        }

        // Checa persistencia: a questao foi gravada como rascunho
        assertThat(questaoRepository.findById(body.id()))
                .get()
                .extracting(Questao::getStatus)
                .isEqualTo(StatusQuestao.RASCUNHO);
    }

    @Test
    void criar_comQuatroAlternativas_deveRetornar400ComMensagemENaoGravar() {
        CriarQuestaoRequest request = new CriarQuestaoRequest(
                "Questao incompleta", topicoQualquer().getId(), NivelDificuldade.FACIL,
                List.of("1", "2", "3", "4"), 0);

        ResponseEntity<ErroResponse> response = client.post().uri(BASE_PATH)
                .header(HttpHeaders.COOKIE, cookie(autor))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().mensagem()).isEqualTo("A questão deve ter exatamente 5 alternativas");
        assertThat(questaoRepository.findByAutorIdOrderByCriadoEmDesc(autor.id())).isEmpty();
    }

    @Test
    void criar_comTopicoInexistente_deveRetornar400ComMensagem() {
        ResponseEntity<ErroResponse> response = client.post().uri(BASE_PATH)
                .header(HttpHeaders.COOKIE, cookie(autor))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildQuestaoRequest("Questao sem topico valido", Long.MAX_VALUE))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().mensagem()).isEqualTo("Tópico não encontrado");
    }

    @Test
    void criar_semAutenticacao_deveRetornar401ComMensagem() {
        ResponseEntity<ErroResponse> response = client.post().uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildQuestaoRequest("Sem login", topicoQualquer().getId()))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().mensagem()).isEqualTo("É preciso estar autenticado.");
    }

    @Test
    void criar_comoAluno_deveRetornar403ComMensagemENaoGravar() {
        Conta aluno = cadastrarAluno(autor);

        ResponseEntity<ErroResponse> response = client.post().uri(BASE_PATH)
                .header(HttpHeaders.COOKIE, cookie(aluno))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildQuestaoRequest("Aluno tentando criar", topicoQualquer().getId()))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().mensagem()).isEqualTo("Sem permissão para esta operação.");
        assertThat(questaoRepository.findByAutorIdOrderByCriadoEmDesc(aluno.id())).isEmpty();
    }

    @Test
    void criar_comoProfessor_deveRetornar201ComAutorDoToken() {
        Conta outroProfessor = cadastrarProfessor(admin);

        ResponseEntity<QuestaoResponse> response = client.post().uri(BASE_PATH)
                .header(HttpHeaders.COOKIE, cookie(outroProfessor))
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildQuestaoRequest("Autor vem do token", topicoQualquer().getId()))
                .retrieve()
                .toEntity(QuestaoResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().autorNome()).isEqualTo(outroProfessor.nome());
        // O autor gravado e o usuario do cookie, nao o professor do @BeforeEach
        assertThat(questaoRepository.findByAutorIdOrderByCriadoEmDesc(outroProfessor.id()))
                .extracting(Questao::getId)
                .containsExactly(response.getBody().id());
        assertThat(questaoRepository.findByAutorIdOrderByCriadoEmDesc(autor.id())).isEmpty();
    }

    // ---------- POST /questoes/{questaoId}/enviar-para-validacao ----------

    @Test
    void enviarParaValidacao_comoAutor_deveRetornar200ComStatusPendente() {
        Long questaoId = criarQuestao(autor, "Questao pronta para validar");

        ResponseEntity<QuestaoResponse> response = client.post()
                .uri(BASE_PATH + "/" + questaoId + "/enviar-para-validacao")
                .header(HttpHeaders.COOKIE, cookie(autor))
                .retrieve()
                .toEntity(QuestaoResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo(questaoId);
        assertThat(response.getBody().status()).isEqualTo(StatusQuestao.PENDENTE);
        assertThat(response.getBody().alternativas()).hasSize(5);

        // Checa persistencia: o novo status foi gravado no banco
        assertThat(questaoRepository.findById(questaoId))
                .get()
                .extracting(Questao::getStatus)
                .isEqualTo(StatusQuestao.PENDENTE);
    }

    @Test
    void enviarParaValidacao_comoOutroProfessor_deveRetornar403EManterRascunho() {
        Long questaoId = criarQuestao(autor, "Questao de outro autor");
        Conta outroProfessor = cadastrarProfessor(admin);

        ResponseEntity<ErroResponse> response = client.post()
                .uri(BASE_PATH + "/" + questaoId + "/enviar-para-validacao")
                .header(HttpHeaders.COOKIE, cookie(outroProfessor))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().mensagem()).isEqualTo("Sem permissão para esta operação.");
        assertThat(questaoRepository.findById(questaoId))
                .get()
                .extracting(Questao::getStatus)
                .isEqualTo(StatusQuestao.RASCUNHO);
    }

    @Test
    void enviarParaValidacao_comQuestaoJaEnviada_deveRetornar400ComMensagem() {
        Long questaoId = criarQuestaoPendente(autor, "Questao enviada duas vezes");

        ResponseEntity<ErroResponse> response = client.post()
                .uri(BASE_PATH + "/" + questaoId + "/enviar-para-validacao")
                .header(HttpHeaders.COOKIE, cookie(autor))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().mensagem()).isEqualTo("Só um rascunho pode ser enviado para validação");
    }

    @Test
    void enviarParaValidacao_comQuestaoInexistente_deveRetornar400ComMensagem() {
        ResponseEntity<ErroResponse> response = client.post()
                .uri(BASE_PATH + "/" + Long.MAX_VALUE + "/enviar-para-validacao")
                .header(HttpHeaders.COOKIE, cookie(autor))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().mensagem()).isEqualTo("Questão não encontrada");
    }

    // ---------- GET /questoes/minhas ----------

    @Test
    void minhas_comQuestoesDeDoisProfessores_deveRetornar200SoComAsDoAutorMaisRecentePrimeiro() {
        Long primeira = criarQuestao(autor, "Primeira do autor");
        Long segunda = criarQuestao(autor, "Segunda do autor");
        Conta outroProfessor = cadastrarProfessor(admin);
        Long deOutro = criarQuestao(outroProfessor, "Questao de outro professor");

        ResponseEntity<List<QuestaoResponse>> response = client.get().uri(BASE_PATH + "/minhas")
                .header(HttpHeaders.COOKIE, cookie(autor))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<QuestaoResponse> questoes = response.getBody();
        assertThat(idsDe(questoes))
                .containsExactly(segunda, primeira)
                .doesNotContain(deOutro);
        for (QuestaoResponse questao : questoes) {
            assertThat(questao.autorNome()).isEqualTo(autor.nome());
            assertThat(questao.status()).isEqualTo(StatusQuestao.RASCUNHO);
        }
    }

    @Test
    void minhas_comSenhaProvisoria_deveRetornar403PedindoTrocaDeSenha() {
        NovoCadastro professorNovo = cadastrarProfessorSemAtivar(admin);
        String tokenProvisorio = login(professorNovo.identificador(), professorNovo.senhaProvisoria());

        ResponseEntity<ErroResponse> response = client.get().uri(BASE_PATH + "/minhas")
                .header(HttpHeaders.COOKIE, cookie(tokenProvisorio))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().mensagem()).isEqualTo("Troque a senha provisória antes de continuar.");
    }

    // ---------- GET /questoes/pendentes ----------

    @Test
    void pendentes_comQuestaoEnviadaPorOutroProfessor_deveRetornar200ComQuestaoNaFila() {
        Long pendente = criarQuestaoPendente(autor, "Questao aguardando validacao");
        Long rascunho = criarQuestao(autor, "Questao ainda em rascunho");
        Conta avaliador = cadastrarProfessor(admin);

        ResponseEntity<List<QuestaoResponse>> response = client.get().uri(BASE_PATH + "/pendentes")
                .header(HttpHeaders.COOKIE, cookie(avaliador))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(idsDe(response.getBody()))
                .contains(pendente)
                .doesNotContain(rascunho);
        for (QuestaoResponse questao : response.getBody()) {
            assertThat(questao.status()).isEqualTo(StatusQuestao.PENDENTE);
        }

        // O autor nao ve a propria questao na fila de avaliacao
        assertThat(idsDe(listarQuestoes(autor, BASE_PATH + "/pendentes"))).doesNotContain(pendente);
    }

    @Test
    void pendentes_comoAluno_deveRetornar403ComMensagem() {
        Conta aluno = cadastrarAluno(autor);

        ResponseEntity<ErroResponse> response = client.get().uri(BASE_PATH + "/pendentes")
                .header(HttpHeaders.COOKIE, cookie(aluno))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().mensagem()).isEqualTo("Sem permissão para esta operação.");
    }
}
