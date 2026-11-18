package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.AlunoTurmaResponse;
import br.com.maisedu.app.dto.AtividadeAlunoResumoResponse;
import br.com.maisedu.app.dto.AtividadeResumoResponse;
import br.com.maisedu.app.dto.CadastroTurmaRequest;
import br.com.maisedu.app.dto.CriarAtividadeRequest;
import br.com.maisedu.app.dto.DesempenhoAlunoResponse;
import br.com.maisedu.app.dto.DesempenhoTurmaResponse;
import br.com.maisedu.app.dto.EnvioFolhaRequest;
import br.com.maisedu.app.dto.FolhaAtividadeResponse;
import br.com.maisedu.app.dto.PainelPraticaResponse;
import br.com.maisedu.app.dto.PraticaAdaptativaResponse;
import br.com.maisedu.app.dto.ProfessorTurmaResponse;
import br.com.maisedu.app.dto.ResponderQuestaoRequest;
import br.com.maisedu.app.dto.ResultadoAtividadeResponse;
import br.com.maisedu.app.dto.TurmaGestaoResponse;
import br.com.maisedu.app.exception.ErroResponse;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.Atividade;
import br.com.maisedu.app.model.VotoAvaliacao;
import br.com.maisedu.app.repository.AtividadeRepository;
import br.com.maisedu.app.service.FechamentoAtividadesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fluxo completo das RNs 03 a 06 pela API real (HTTP -> segurança -> controllers -> services -> H2):
 * turma (RF06) -> vínculos (RN01) -> questões aprovadas por quórum (RN02) -> atividade com prazo (RF15)
 * -> marcação e entrega única com snapshot e unicidade (RF16, RN03, RN04) -> correção e desempenho só
 * depois do prazo (RF17, RF19) -> prática adaptativa recomendada e por tópico (RF18).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FluxoAtividadeIntegrationTest extends IntegracaoTestBase {

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Autowired
    private FechamentoAtividadesService fechamentoAtividadesService;

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

        // RF15: atividade com prazo e as questões na ordem escolhida (Q2 antes de Q1)
        ResponseEntity<AtividadeResumoResponse> atividade = postar(professor, "/atividades",
                new CriarAtividadeRequest(turmaId, "Revisão " + ts, LocalDateTime.now().plusDays(1), List.of(q2, q1)),
                AtividadeResumoResponse.class);
        assertEquals(HttpStatus.CREATED, atividade.getStatusCode());
        Long atividadeId = atividade.getBody().id();
        assertEquals(2, atividade.getBody().totalQuestoes());
        assertFalse(atividade.getBody().prazoEncerrado());

        // prazo no passado é recusado
        assertEquals(HttpStatus.BAD_REQUEST, postar(professor, "/atividades",
                new CriarAtividadeRequest(turmaId, "Atrasada " + ts, LocalDateTime.now().minusHours(1), List.of(q1)),
                ErroResponse.class).getStatusCode());

        // O aluno vê a atividade como não iniciada
        AtividadeAlunoResumoResponse item = daLista(aluno, atividadeId);
        assertEquals(AtividadeAlunoResumoResponse.NAO_INICIADA, item.situacao());
        assertNotNull(item.prazo());

        // RF16/RN03: ao iniciar, as questões são fixadas na ordem da atividade
        FolhaAtividadeResponse folha = postar(aluno, "/aluno/atividades/" + atividadeId + "/iniciar", null,
                FolhaAtividadeResponse.class).getBody();
        assertEquals(2, folha.questoes().size());
        assertEquals("Fluxo Q2 " + ts, folha.questoes().get(0).enunciado());
        assertTrue(folha.podeResponder());

        // Questões criadas pelo helper têm a alternativa C como correta
        FolhaAtividadeResponse.QuestaoEntregue primeira = folha.questoes().get(0);
        FolhaAtividadeResponse.QuestaoEntregue segunda = folha.questoes().get(1);
        String uriFolha = "/aluno/folhas/" + folha.atividadeAlunoId();

        // Marcação: o aluno escolhe A, muda para B e sai; nada disso é resposta ainda
        String uriMarcacao = uriFolha + "/questoes/" + primeira.atividadeAlunoQuestaoId() + "/marcacao";
        for (char letra : new char[]{'A', 'B'}) {
            assertEquals(HttpStatus.NO_CONTENT, marcar(aluno, uriMarcacao, alternativa(primeira, letra)).getStatusCode());
        }
        FolhaAtividadeResponse comMarcacao = buscar(aluno, uriFolha, new ParameterizedTypeReference<>() {});
        assertEquals(alternativa(primeira, 'B'), comMarcacao.questoes().get(0).alternativaEscolhidaId());
        assertNull(comMarcacao.questoes().get(0).correcao());
        assertEquals(1, comMarcacao.escolhidas());

        // Entrega única: com uma questão sem resposta, o envio é recusado
        ResponseEntity<ErroResponse> incompleto = postar(aluno, uriFolha + "/envio",
                new EnvioFolhaRequest(List.of(new EnvioFolhaRequest.Escolha(primeira.atividadeAlunoQuestaoId(),
                        alternativa(primeira, 'C')))), ErroResponse.class);
        assertEquals(HttpStatus.BAD_REQUEST, incompleto.getStatusCode());

        // Envio completo: vale o que foi enviado (C na primeira, A na segunda)
        ResponseEntity<FolhaAtividadeResponse> envio = postar(aluno, uriFolha + "/envio", new EnvioFolhaRequest(List.of(
                new EnvioFolhaRequest.Escolha(primeira.atividadeAlunoQuestaoId(), alternativa(primeira, 'C')),
                new EnvioFolhaRequest.Escolha(segunda.atividadeAlunoQuestaoId(), alternativa(segunda, 'A')))),
                FolhaAtividadeResponse.class);
        assertEquals(HttpStatus.OK, envio.getStatusCode());
        FolhaAtividadeResponse enviada = envio.getBody();
        assertNotNull(enviada.enviadaEm());
        assertFalse(enviada.podeResponder());
        // antes do prazo o aluno não vê a correção (nem quantas acertou)
        assertFalse(enviada.correcaoLiberada());
        assertNull(enviada.acertos());
        assertNull(enviada.questoes().get(0).correcao());
        assertEquals(alternativa(primeira, 'C'), enviada.questoes().get(0).alternativaEscolhidaId());

        // Depois de enviada, nada muda: nem marcação nem novo envio (RN04)
        assertEquals(HttpStatus.BAD_REQUEST, marcar(aluno, uriMarcacao, alternativa(primeira, 'A')).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, postar(aluno, uriFolha + "/envio", new EnvioFolhaRequest(List.of(
                new EnvioFolhaRequest.Escolha(primeira.atividadeAlunoQuestaoId(), alternativa(primeira, 'A')),
                new EnvioFolhaRequest.Escolha(segunda.atividadeAlunoQuestaoId(), alternativa(segunda, 'A')))),
                ErroResponse.class).getStatusCode());

        item = daLista(aluno, atividadeId);
        assertEquals(AtividadeAlunoResumoResponse.ENTREGUE, item.situacao());
        assertNull(item.acertos());

        // RN05: antes do prazo o desempenho do aluno não muda
        DesempenhoAlunoResponse antesDoPrazo = buscar(aluno, "/aluno/desempenho", new ParameterizedTypeReference<>() {});
        assertEquals(0, antesDoPrazo.totalRespostas());
        assertTrue(antesDoPrazo.evolucao().isEmpty());

        // O professor já vê o resultado de quem entregou e a análise por questão
        ResultadoAtividadeResponse resultado = buscar(professor, "/atividades/" + atividadeId + "/resultados",
                new ParameterizedTypeReference<>() {});
        ResultadoAtividadeResponse.ResultadoAluno doAluno = null;
        for (ResultadoAtividadeResponse.ResultadoAluno linha : resultado.alunos()) {
            if (linha.alunoId().equals(aluno.id())) {
                doAluno = linha;
            }
        }
        assertNotNull(doAluno);
        assertEquals(AtividadeAlunoResumoResponse.ENTREGUE, doAluno.situacao());
        assertEquals(1, doAluno.acertos());
        ResultadoAtividadeResponse.AnaliseQuestao analiseQ2 = resultado.analise().get(0);
        assertEquals(1, analiseQ2.respostas());
        assertEquals(0, analiseQ2.acertos());
        assertEquals('A', analiseQ2.erroMaisComum().letra());

        // O prazo termina (simulado) e a rotina de fechamento contabiliza o desempenho
        Atividade daAtividade = atividadeRepository.findById(atividadeId).orElseThrow();
        ReflectionTestUtils.setField(daAtividade, "prazo", LocalDateTime.now().minusMinutes(1));
        atividadeRepository.save(daAtividade);
        assertTrue(fechamentoAtividadesService.fecharVencidas() >= 1);

        FolhaAtividadeResponse corrigida = buscar(aluno, uriFolha, new ParameterizedTypeReference<>() {});
        assertTrue(corrigida.correcaoLiberada());
        assertEquals(1, corrigida.acertos());
        assertTrue(corrigida.questoes().get(0).correcao().acertou());
        assertFalse(corrigida.questoes().get(1).correcao().acertou());
        assertEquals('C', corrigida.questoes().get(1).correcao().letraCorreta());

        // RF17/RF19: desempenho acumulado (1 acerto em 2 respostas); fechar de novo não soma outra vez
        fechamentoAtividadesService.fecharVencidas();
        DesempenhoAlunoResponse painel = buscar(aluno, "/aluno/desempenho", new ParameterizedTypeReference<>() {});
        assertEquals(2, painel.totalRespostas());
        assertEquals(1, painel.totalAcertos());
        assertEquals(0, new BigDecimal("50.00").compareTo(painel.percentualGeral()));
        assertFalse(painel.evolucao().isEmpty());

        // RF19: o professor da turma vê o aluno no painel da turma e no mapa de calor
        DesempenhoTurmaResponse daTurma = buscar(professor, "/turmas/" + turmaId + "/desempenho",
                new ParameterizedTypeReference<>() {});
        boolean alunoNoPainel = false;
        for (DesempenhoTurmaResponse.AlunoResumo resumo : daTurma.alunos()) {
            if (resumo.alunoId().equals(aluno.id())) {
                alunoNoPainel = resumo.totalRespostas() == 2;
            }
        }
        assertTrue(alunoNoPainel);
        assertFalse(daTurma.celulas().isEmpty());
        assertFalse(daTurma.evolucao().isEmpty());

        // RF19/CA02: professor que não leciona na turma não vê o desempenho dela
        ResponseEntity<ErroResponse> negado = client.get().uri("/turmas/" + turmaId + "/desempenho")
                .header(HttpHeaders.COOKIE, cookie(autor))
                .retrieve()
                .toEntity(ErroResponse.class);
        assertEquals(HttpStatus.FORBIDDEN, negado.getStatusCode());

        // RF18: prática adaptativa recomendada; pedir de novo retoma a mesma
        PraticaAdaptativaResponse pratica = postar(aluno, "/aluno/praticas-adaptativas", null,
                PraticaAdaptativaResponse.class).getBody();
        assertTrue(pratica.nova());
        assertTrue(pratica.totalQuestoes() >= 1);
        PraticaAdaptativaResponse retomada = postar(aluno, "/aluno/praticas-adaptativas", null,
                PraticaAdaptativaResponse.class).getBody();
        assertFalse(retomada.nova());
        assertEquals(pratica.atividadeAlunoId(), retomada.atividadeAlunoId());

        // Envio da prática: sem prazo, a correção sai na hora e o desempenho já soma as respostas
        FolhaAtividadeResponse folhaDaPratica = buscar(aluno, "/aluno/folhas/" + pratica.atividadeAlunoId(),
                new ParameterizedTypeReference<>() {});
        List<EnvioFolhaRequest.Escolha> escolhas = new ArrayList<>();
        for (FolhaAtividadeResponse.QuestaoEntregue questao : folhaDaPratica.questoes()) {
            escolhas.add(new EnvioFolhaRequest.Escolha(questao.atividadeAlunoQuestaoId(), alternativa(questao, 'C')));
        }
        FolhaAtividadeResponse praticaEnviada = postar(aluno, "/aluno/folhas/" + pratica.atividadeAlunoId() + "/envio",
                new EnvioFolhaRequest(escolhas), FolhaAtividadeResponse.class).getBody();
        assertTrue(praticaEnviada.correcaoLiberada());
        assertNotNull(praticaEnviada.acertos());
        assertNotNull(praticaEnviada.questoes().get(0).correcao());
        DesempenhoAlunoResponse depoisDaPratica = buscar(aluno, "/aluno/desempenho",
                new ParameterizedTypeReference<>() {});
        assertEquals(2 + escolhas.size(), depoisDaPratica.totalRespostas());

        // Tela Praticar: card do tópico respondido; prática só desse tópico
        PainelPraticaResponse painelPratica = buscar(aluno, "/aluno/praticas", new ParameterizedTypeReference<>() {});
        assertFalse(painelPratica.topicos().isEmpty());
        PainelPraticaResponse.TopicoParaPraticar card = painelPratica.topicos().get(0);
        PraticaAdaptativaResponse doTopico = postar(aluno, "/aluno/praticas-adaptativas?topicoId=" + card.topicoId(), null,
                PraticaAdaptativaResponse.class).getBody();
        assertTrue(doTopico.nova());
        assertEquals(card.topico(), doTopico.topico());
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

    private AtividadeAlunoResumoResponse daLista(Conta aluno, Long atividadeId) {
        List<AtividadeAlunoResumoResponse> minhas = buscar(aluno, "/aluno/atividades", new ParameterizedTypeReference<>() {});
        for (AtividadeAlunoResumoResponse a : minhas) {
            if (atividadeId.equals(a.atividadeId())) {
                return a;
            }
        }
        throw new IllegalStateException("Atividade " + atividadeId + " não está na lista do aluno");
    }

    private ResponseEntity<ErroResponse> marcar(Conta aluno, String uri, Long alternativaId) {
        return client.put().uri(uri)
                .header(HttpHeaders.COOKIE, cookie(aluno))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ResponderQuestaoRequest(alternativaId))
                .retrieve()
                .toEntity(ErroResponse.class);
    }

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
