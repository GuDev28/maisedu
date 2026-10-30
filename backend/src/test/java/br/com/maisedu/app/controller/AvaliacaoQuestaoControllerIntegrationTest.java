package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.AvaliacaoQuestaoRequest;
import br.com.maisedu.app.dto.AvaliacaoQuestaoResponse;
import br.com.maisedu.app.dto.QuestaoResponse;
import br.com.maisedu.app.exception.ErroResponse;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.StatusQuestao;
import br.com.maisedu.app.model.VotoAvaliacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integracao do AvaliacaoQuestaoController (RN2 - validacao colaborativa com quorum:
 * ate 5 votos por questao, um por professor; 3 votos no mesmo sentido decidem o status).
 *
 * Fluxo real exercitado em cada teste:
 * RestClient -> HTTP real -> JwtAuthenticationFilter/Spring Security -> AvaliacaoQuestaoController
 * -> AvaliacaoQuestaoService -> QuestaoRepository/AvaliacaoQuestaoRepository -> H2 em memoria
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AvaliacaoQuestaoControllerIntegrationTest extends IntegracaoTestBase {

    private Conta admin;
    private Conta autor;
    private Conta avaliador;

    @BeforeEach
    void criarContas() {
        admin = criarAdmin();
        autor = cadastrarProfessor(admin);
        avaliador = cadastrarProfessor(admin);
    }

    private String avaliacoesPath(Long questaoId) {
        return "/questoes/" + questaoId + "/avaliacoes";
    }

    // ---------- POST /questoes/{questaoId}/avaliacoes ----------

    @Test
    void avaliar_comVotoAprovar_deveRetornar200ComContagemAtualizada() {
        Long questaoId = criarQuestaoPendente(autor, "Quanto e 7 x 8?");

        ResponseEntity<AvaliacaoQuestaoResponse> response = client.post().uri(avaliacoesPath(questaoId))
                .header(HttpHeaders.COOKIE, cookie(avaliador))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AvaliacaoQuestaoRequest(VotoAvaliacao.APROVAR))
                .retrieve()
                .toEntity(AvaliacaoQuestaoResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AvaliacaoQuestaoResponse body = response.getBody();
        assertThat(body.questaoId()).isEqualTo(questaoId);
        assertThat(body.status()).isEqualTo(StatusQuestao.PENDENTE);
        assertThat(body.aprovacoes()).isEqualTo(1);
        assertThat(body.rejeicoes()).isZero();
        assertThat(body.votosParaDecidir()).isEqualTo(3);
        assertThat(body.maximoVotos()).isEqualTo(5);

        // Checa persistencia: o voto deste professor foi gravado
        assertThat(avaliacaoQuestaoRepository.existsByQuestaoIdAndProfessorId(questaoId, avaliador.id())).isTrue();
        assertThat(avaliacaoQuestaoRepository.countByQuestaoId(questaoId)).isEqualTo(1);
    }

    @Test
    void avaliar_comTerceiraAprovacao_deveMudarQuestaoDoAutorParaAprovada() {
        // Relacionamento entre recursos: professor -> questao (POST /questoes, envio) -> avaliacoes -> status
        Long questaoId = criarQuestaoPendente(autor, "Qual e a area de um quadrado de lado 4?");
        Conta segundoAvaliador = cadastrarProfessor(admin);
        Conta terceiroAvaliador = cadastrarProfessor(admin);
        votar(avaliador, questaoId, VotoAvaliacao.APROVAR);
        votar(segundoAvaliador, questaoId, VotoAvaliacao.APROVAR);

        ResponseEntity<AvaliacaoQuestaoResponse> response = client.post().uri(avaliacoesPath(questaoId))
                .header(HttpHeaders.COOKIE, cookie(terceiroAvaliador))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AvaliacaoQuestaoRequest(VotoAvaliacao.APROVAR))
                .retrieve()
                .toEntity(AvaliacaoQuestaoResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo(StatusQuestao.APROVADA);
        assertThat(response.getBody().aprovacoes()).isEqualTo(3);
        assertThat(response.getBody().rejeicoes()).isZero();

        // O autor ve a questao aprovada em GET /questoes/minhas
        QuestaoResponse questaoDoAutor = null;
        for (QuestaoResponse questao : listarQuestoes(autor, "/questoes/minhas")) {
            if (questao.id().equals(questaoId)) {
                questaoDoAutor = questao;
            }
        }
        assertThat(questaoDoAutor).isNotNull();
        assertThat(questaoDoAutor.status()).isEqualTo(StatusQuestao.APROVADA);

        // E o status ficou gravado no banco
        assertThat(questaoRepository.findById(questaoId))
                .get()
                .extracting(Questao::getStatus)
                .isEqualTo(StatusQuestao.APROVADA);
    }

    @Test
    void avaliar_comTerceiraRejeicao_deveRetornar200ComQuestaoRejeitada() {
        Long questaoId = criarQuestaoPendente(autor, "Questao com gabarito errado");
        Conta segundoAvaliador = cadastrarProfessor(admin);
        Conta terceiroAvaliador = cadastrarProfessor(admin);
        Conta quartoAvaliador = cadastrarProfessor(admin);
        votar(avaliador, questaoId, VotoAvaliacao.APROVAR);
        votar(segundoAvaliador, questaoId, VotoAvaliacao.REJEITAR);
        votar(terceiroAvaliador, questaoId, VotoAvaliacao.REJEITAR);

        ResponseEntity<AvaliacaoQuestaoResponse> response = client.post().uri(avaliacoesPath(questaoId))
                .header(HttpHeaders.COOKIE, cookie(quartoAvaliador))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AvaliacaoQuestaoRequest(VotoAvaliacao.REJEITAR))
                .retrieve()
                .toEntity(AvaliacaoQuestaoResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo(StatusQuestao.REJEITADA);
        assertThat(response.getBody().aprovacoes()).isEqualTo(1);
        assertThat(response.getBody().rejeicoes()).isEqualTo(3);

        assertThat(questaoRepository.findById(questaoId))
                .get()
                .extracting(Questao::getStatus)
                .isEqualTo(StatusQuestao.REJEITADA);
        assertThat(avaliacaoQuestaoRepository.countByQuestaoId(questaoId)).isEqualTo(4);
    }

    @Test
    void avaliar_aposVotar_deveTirarQuestaoDaFilaDePendentesDoAvaliador() {
        // Relacionamento entre recursos: o voto (avaliacoes) muda o que GET /questoes/pendentes devolve
        Long questaoId = criarQuestaoPendente(autor, "Questao na fila de dois professores");
        Conta outroAvaliador = cadastrarProfessor(admin);
        assertThat(idsDe(listarQuestoes(avaliador, "/questoes/pendentes"))).contains(questaoId);

        ResponseEntity<AvaliacaoQuestaoResponse> response = client.post().uri(avaliacoesPath(questaoId))
                .header(HttpHeaders.COOKIE, cookie(avaliador))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AvaliacaoQuestaoRequest(VotoAvaliacao.APROVAR))
                .retrieve()
                .toEntity(AvaliacaoQuestaoResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(idsDe(listarQuestoes(avaliador, "/questoes/pendentes"))).doesNotContain(questaoId);
        assertThat(idsDe(listarQuestoes(outroAvaliador, "/questoes/pendentes"))).contains(questaoId);
    }

    @Test
    void avaliar_professorQueJaVotou_deveRetornar400EManterUmVoto() {
        Long questaoId = criarQuestaoPendente(autor, "Questao votada duas vezes");
        votar(avaliador, questaoId, VotoAvaliacao.APROVAR);

        ResponseEntity<ErroResponse> response = client.post().uri(avaliacoesPath(questaoId))
                .header(HttpHeaders.COOKIE, cookie(avaliador))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AvaliacaoQuestaoRequest(VotoAvaliacao.APROVAR))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().mensagem()).isEqualTo("Professor já avaliou esta questão");
        assertThat(avaliacaoQuestaoRepository.countByQuestaoId(questaoId)).isEqualTo(1);
    }

    @Test
    void avaliar_propriaQuestao_deveRetornar400ComMensagem() {
        Long questaoId = criarQuestaoPendente(autor, "Autor tentando votar");

        ResponseEntity<ErroResponse> response = client.post().uri(avaliacoesPath(questaoId))
                .header(HttpHeaders.COOKIE, cookie(autor))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AvaliacaoQuestaoRequest(VotoAvaliacao.APROVAR))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().mensagem()).isEqualTo("O autor da questão não pode avaliar a própria questão");
        assertThat(avaliacaoQuestaoRepository.countByQuestaoId(questaoId)).isZero();
    }

    @Test
    void avaliar_questaoEmRascunho_deveRetornar400ComMensagem() {
        Long questaoId = criarQuestao(autor, "Rascunho nao enviado");

        ResponseEntity<ErroResponse> response = client.post().uri(avaliacoesPath(questaoId))
                .header(HttpHeaders.COOKIE, cookie(avaliador))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AvaliacaoQuestaoRequest(VotoAvaliacao.APROVAR))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().mensagem())
                .isEqualTo("Questão ainda é um rascunho: o autor precisa enviá-la para validação antes");
    }

    @Test
    void avaliar_questaoJaAprovada_deveRetornar400ValidacaoEncerrada() {
        Long questaoId = criarQuestaoPendente(autor, "Questao ja decidida");
        Conta segundoAvaliador = cadastrarProfessor(admin);
        Conta terceiroAvaliador = cadastrarProfessor(admin);
        Conta atrasado = cadastrarProfessor(admin);
        votar(avaliador, questaoId, VotoAvaliacao.APROVAR);
        votar(segundoAvaliador, questaoId, VotoAvaliacao.APROVAR);
        votar(terceiroAvaliador, questaoId, VotoAvaliacao.APROVAR);

        ResponseEntity<ErroResponse> response = client.post().uri(avaliacoesPath(questaoId))
                .header(HttpHeaders.COOKIE, cookie(atrasado))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AvaliacaoQuestaoRequest(VotoAvaliacao.REJEITAR))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().mensagem()).isEqualTo("A validação desta questão já foi encerrada");
        assertThat(avaliacaoQuestaoRepository.countByQuestaoId(questaoId)).isEqualTo(3);
    }

    @Test
    void avaliar_semVoto_deveRetornar400ComMensagem() {
        Long questaoId = criarQuestaoPendente(autor, "Voto em branco");

        ResponseEntity<ErroResponse> response = client.post().uri(avaliacoesPath(questaoId))
                .header(HttpHeaders.COOKIE, cookie(avaliador))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AvaliacaoQuestaoRequest(null))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().mensagem()).isEqualTo("Informe o voto (APROVAR ou REJEITAR)");
        assertThat(avaliacaoQuestaoRepository.countByQuestaoId(questaoId)).isZero();
    }

    @Test
    void avaliar_comoAluno_deveRetornar403ComMensagem() {
        Long questaoId = criarQuestaoPendente(autor, "Aluno tentando votar");
        Conta aluno = cadastrarAluno(autor);

        ResponseEntity<ErroResponse> response = client.post().uri(avaliacoesPath(questaoId))
                .header(HttpHeaders.COOKIE, cookie(aluno))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AvaliacaoQuestaoRequest(VotoAvaliacao.APROVAR))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().mensagem()).isEqualTo("Sem permissão para esta operação.");
        assertThat(avaliacaoQuestaoRepository.countByQuestaoId(questaoId)).isZero();
    }
}
