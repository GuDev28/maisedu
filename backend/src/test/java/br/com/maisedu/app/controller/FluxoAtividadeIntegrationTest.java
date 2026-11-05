package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.AlunoTurmaResponse;
import br.com.maisedu.app.dto.AtividadeAlunoResumoResponse;
import br.com.maisedu.app.dto.AtividadeResumoResponse;
import br.com.maisedu.app.dto.CadastroTurmaRequest;
import br.com.maisedu.app.dto.CriarAtividadeRequest;
import br.com.maisedu.app.dto.DesempenhoAlunoResponse;
import br.com.maisedu.app.dto.DesempenhoTurmaResponse;
import br.com.maisedu.app.dto.FolhaAtividadeResponse;
import br.com.maisedu.app.dto.PraticaAdaptativaResponse;
import br.com.maisedu.app.dto.ProfessorTurmaResponse;
import br.com.maisedu.app.dto.QuestaoResponse;
import br.com.maisedu.app.dto.ResponderQuestaoRequest;
import br.com.maisedu.app.dto.RespostaResultadoResponse;
import br.com.maisedu.app.dto.TurmaGestaoResponse;
import br.com.maisedu.app.exception.ErroResponse;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.VotoAvaliacao;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fluxo completo das RNs 03 a 06 pela API real (HTTP -> segurança -> controllers -> services -> H2):
 * turma (RF06) -> vínculos (RN01) -> questões aprovadas por quórum (RN02) -> atividade (RF15) ->
 * respostas com snapshot e unicidade (RF16, RN03, RN04) -> desempenho (RF17, RF19) -> prática adaptativa (RF18).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FluxoAtividadeIntegrationTest extends IntegracaoTestBase {

    @Test
    void fluxoCompleto_daTurmaAPraticaAdaptativa() {
        long ts = System.nanoTime();
        Conta admin = criarAdmin();

        // RF06: admin cadastra a turma
        ResponseEntity<TurmaGestaoResponse> turmaCriada = postar(admin, "/turmas",
                new CadastroTurmaRequest("7º ano A " + ts, AnoEscolar.SETIMO_ANO), TurmaGestaoResponse.class);
        assertEquals(HttpStatus.CREATED, turmaCriada.getStatusCode());
        Long turmaId = turmaCriada.getBody().id();
        assertTrue(turmaCriada.getBody().ativo());

        // RN01: professor vinculado pelo admin; aluno cadastrado e vinculado pelo professor
        Conta professor = cadastrarProfessor(admin);
        assertEquals(HttpStatus.CREATED, postar(admin, "/turmas/" + turmaId + "/professores/" + professor.id(), null,
                ProfessorTurmaResponse.class).getStatusCode());
        NovoCadastro novoAluno = cadastrarAlunoSemAtivar(professor);
        assertEquals(HttpStatus.CREATED, postar(professor, "/turmas/" + turmaId + "/alunos/" + novoAluno.id(), null,
                AlunoTurmaResponse.class).getStatusCode());
        Conta aluno = ativarConta(novoAluno);

        // RN02: duas questões aprovadas por quórum de 3 avaliadores
        Conta autor = cadastrarProfessor(admin);
        List<Conta> avaliadores = List.of(cadastrarProfessor(admin), cadastrarProfessor(admin), cadastrarProfessor(admin));
        Long q1 = criarQuestaoPendente(autor, "Fluxo Q1 " + ts);
        Long q2 = criarQuestaoPendente(autor, "Fluxo Q2 " + ts);
        for (Conta avaliador : avaliadores) {
            votar(avaliador, q1, VotoAvaliacao.APROVAR);
            votar(avaliador, q2, VotoAvaliacao.APROVAR);
        }
        List<Long> banco = idsDe(listarQuestoes(professor, "/questoes/aprovadas"));
        assertTrue(banco.contains(q1) && banco.contains(q2), "aprovadas aparecem no banco compartilhado");

        // RF15: atividade com as questões na ordem escolhida (Q2 antes de Q1)
        ResponseEntity<AtividadeResumoResponse> atividade = postar(professor, "/atividades",
                new CriarAtividadeRequest(turmaId, "Revisão " + ts, List.of(q2, q1)), AtividadeResumoResponse.class);
        assertEquals(HttpStatus.CREATED, atividade.getStatusCode());
        Long atividadeId = atividade.getBody().id();
        assertEquals(2, atividade.getBody().totalQuestoes());

        // O aluno vê a atividade como não iniciada
        List<AtividadeAlunoResumoResponse> minhas = buscar(aluno, "/aluno/atividades",
                new ParameterizedTypeReference<List<AtividadeAlunoResumoResponse>>() {});
        AtividadeAlunoResumoResponse item = null;
        for (AtividadeAlunoResumoResponse a : minhas) {
            if (atividadeId.equals(a.atividadeId())) {
                item = a;
            }
        }
        assertNotNull(item);
        assertEquals(AtividadeAlunoResumoResponse.NAO_INICIADA, item.situacao());

        // RF16/RN03: ao iniciar, as questões são fixadas na ordem da atividade
        FolhaAtividadeResponse folha = postar(aluno, "/aluno/atividades/" + atividadeId + "/iniciar", null,
                FolhaAtividadeResponse.class).getBody();
        assertEquals(2, folha.questoes().size());
        assertEquals("Fluxo Q2 " + ts, folha.questoes().get(0).enunciado());

        // Questões criadas pelo helper têm a alternativa C como correta
        FolhaAtividadeResponse.QuestaoEntregue primeira = folha.questoes().get(0);
        FolhaAtividadeResponse.QuestaoEntregue segunda = folha.questoes().get(1);
        String uriPrimeira = "/aluno/folhas/" + folha.atividadeAlunoId() + "/questoes/" + primeira.atividadeAlunoQuestaoId()
                + "/resposta";
        RespostaResultadoResponse certa = postar(aluno, uriPrimeira,
                new ResponderQuestaoRequest(alternativa(primeira, 'C')), RespostaResultadoResponse.class).getBody();
        assertTrue(certa.acertou());
        assertFalse(certa.atividadeConcluida());

        // RF16/CA02 e RN04: a mesma questão não aceita segunda resposta
        ResponseEntity<ErroResponse> repetida = postar(aluno, uriPrimeira,
                new ResponderQuestaoRequest(alternativa(primeira, 'A')), ErroResponse.class);
        assertEquals(HttpStatus.BAD_REQUEST, repetida.getStatusCode());

        RespostaResultadoResponse errada = postar(aluno,
                "/aluno/folhas/" + folha.atividadeAlunoId() + "/questoes/" + segunda.atividadeAlunoQuestaoId() + "/resposta",
                new ResponderQuestaoRequest(alternativa(segunda, 'A')), RespostaResultadoResponse.class).getBody();
        assertFalse(errada.acertou());
        assertEquals('C', errada.letraCorreta());
        assertTrue(errada.atividadeConcluida());

        // RF16/CA03: reabrir devolve a mesma folha, já com as respostas
        FolhaAtividadeResponse reaberta = postar(aluno, "/aluno/atividades/" + atividadeId + "/iniciar", null,
                FolhaAtividadeResponse.class).getBody();
        assertEquals(folha.atividadeAlunoId(), reaberta.atividadeAlunoId());
        assertEquals(2, reaberta.respondidas());
        assertEquals(1, reaberta.acertos());

        // RF17/RF19: desempenho acumulado (1 acerto em 2 respostas)
        DesempenhoAlunoResponse painel = buscar(aluno, "/aluno/desempenho",
                new ParameterizedTypeReference<DesempenhoAlunoResponse>() {});
        assertEquals(2, painel.totalRespostas());
        assertEquals(1, painel.totalAcertos());
        assertEquals(0, new BigDecimal("50.00").compareTo(painel.percentualGeral()));
        assertFalse(painel.evolucao().isEmpty());

        // RF19: o professor da turma vê o aluno no painel da turma
        DesempenhoTurmaResponse daTurma = buscar(professor, "/turmas/" + turmaId + "/desempenho",
                new ParameterizedTypeReference<DesempenhoTurmaResponse>() {});
        boolean alunoNoPainel = false;
        for (DesempenhoTurmaResponse.AlunoResumo resumo : daTurma.alunos()) {
            if (resumo.alunoId().equals(aluno.id())) {
                alunoNoPainel = resumo.totalRespostas() == 2;
            }
        }
        assertTrue(alunoNoPainel);

        // RF19: indicadores da turma, detalhe por nível e evolução semanal
        assertEquals(1, daTurma.resumo().totalAlunos());
        assertEquals(1, daTurma.resumo().alunosComRespostas());
        assertEquals(1L, daTurma.resumo().totalAtividades());
        assertEquals(2, daTurma.resumo().totalRespostas());
        assertEquals(1, daTurma.resumo().totalAcertos());
        assertEquals(0, new BigDecimal("50.00").compareTo(daTurma.resumo().percentual()));
        assertFalse(daTurma.topicos().isEmpty());
        assertFalse(daTurma.topicos().get(0).niveis().isEmpty());
        assertFalse(daTurma.evolucao().isEmpty());

        // RF19/CA02: professor que não leciona na turma não vê o desempenho dela
        ResponseEntity<ErroResponse> negado = client.get().uri("/turmas/" + turmaId + "/desempenho")
                .header(HttpHeaders.COOKIE, cookie(autor))
                .retrieve()
                .toEntity(ErroResponse.class);
        assertEquals(HttpStatus.FORBIDDEN, negado.getStatusCode());

        // RF18: prática adaptativa gerada a partir do desempenho; pedir de novo retoma a mesma
        PraticaAdaptativaResponse pratica = postar(aluno, "/aluno/praticas-adaptativas", null,
                PraticaAdaptativaResponse.class).getBody();
        assertTrue(pratica.nova());
        assertTrue(pratica.totalQuestoes() >= 1);
        PraticaAdaptativaResponse retomada = postar(aluno, "/aluno/praticas-adaptativas", null,
                PraticaAdaptativaResponse.class).getBody();
        assertFalse(retomada.nova());
        assertEquals(pratica.atividadeAlunoId(), retomada.atividadeAlunoId());
    }

    @Test
    void alunoNaoAbreFolhaDeOutroAluno() {
        Conta admin = criarAdmin();
        Conta professor = cadastrarProfessor(admin);
        Conta outroAluno = cadastrarAluno(professor);

        ResponseEntity<ErroResponse> resposta = client.get().uri("/aluno/folhas/999999")
                .header(HttpHeaders.COOKIE, cookie(outroAluno))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, resposta.getStatusCode());
    }

    @Test
    void professorNaoAcessaAreaDoAluno() {
        Conta admin = criarAdmin();
        Conta professor = cadastrarProfessor(admin);

        ResponseEntity<ErroResponse> resposta = client.get().uri("/aluno/atividades")
                .header(HttpHeaders.COOKIE, cookie(professor))
                .retrieve()
                .toEntity(ErroResponse.class);

        assertEquals(HttpStatus.FORBIDDEN, resposta.getStatusCode());
    }

    // ---------- helpers ----------

    private <T> ResponseEntity<T> postar(Conta conta, String uri, Object corpo, Class<T> tipo) {
        if (corpo == null) {
            return client.post().uri(uri).header(HttpHeaders.COOKIE, cookie(conta)).retrieve().toEntity(tipo);
        }
        return client.post().uri(uri)
                .header(HttpHeaders.COOKIE, cookie(conta))
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .toEntity(tipo);
    }

    private <T> T buscar(Conta conta, String uri, ParameterizedTypeReference<T> tipo) {
        ResponseEntity<T> resposta = client.get().uri(uri).header(HttpHeaders.COOKIE, cookie(conta)).retrieve()
                .toEntity(tipo);
        assertEquals(HttpStatus.OK, resposta.getStatusCode(), "GET " + uri);
        return resposta.getBody();
    }

    private static Long alternativa(FolhaAtividadeResponse.QuestaoEntregue questao, char letra) {
        for (FolhaAtividadeResponse.AlternativaAluno alternativa : questao.alternativas()) {
            if (alternativa.letra() == letra) {
                return alternativa.id();
            }
        }
        throw new IllegalStateException("Alternativa " + letra + " não encontrada");
    }
}
