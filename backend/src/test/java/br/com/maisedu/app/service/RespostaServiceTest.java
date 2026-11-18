package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.EnvioFolhaRequest;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
import br.com.maisedu.app.model.Alternativa;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.Atividade;
import br.com.maisedu.app.model.AtividadeAluno;
import br.com.maisedu.app.model.AtividadeAlunoQuestao;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.Resposta;
import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.StatusQuestao;
import br.com.maisedu.app.model.Topico;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlternativaRepository;
import br.com.maisedu.app.repository.AtividadeAlunoQuestaoRepository;
import br.com.maisedu.app.repository.AtividadeAlunoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * RF16 e RN04 com entrega única: o aluno marca e troca as alternativas e envia tudo de uma vez, só com
 * todas as questões respondidas e dentro do prazo. RN05: a prática adaptativa entra no desempenho no
 * envio; a atividade regular, só no fechamento do prazo.
 */
@ExtendWith(MockitoExtension.class)
class RespostaServiceTest {

    private static final Long ALUNO_ID = 10L;
    private static final Long ATIVIDADE_ALUNO_ID = 50L;

    @Mock UsuarioRepository usuarioRepository;
    @Mock AtividadeAlunoRepository atividadeAlunoRepository;
    @Mock AtividadeAlunoQuestaoRepository atividadeAlunoQuestaoRepository;
    @Mock AlternativaRepository alternativaRepository;
    @Mock RespostaRepository respostaRepository;
    @Mock DesempenhoService desempenhoService;
    @Mock AuditoriaService auditoriaService;
    @InjectMocks RespostaService service;

    private Usuario aluno;
    private Questao questao1;
    private Questao questao2;
    private AtividadeAluno pratica;
    private AtividadeAlunoQuestao entregue1;
    private AtividadeAlunoQuestao entregue2;

    @BeforeEach
    void setUp() {
        Instituicao escola = new Instituicao("Escola", "00.000.000/0001-00", "Rua 1");
        ReflectionTestUtils.setField(escola, "id", 1L);
        aluno = Usuario.novoAluno("Ana Aluna", "a123456", "hash", escola);
        ReflectionTestUtils.setField(aluno, "id", ALUNO_ID);
        aluno.adicionarRole(new Role(RoleNome.ALUNO));

        questao1 = questao(1L, 2, 100L);
        questao2 = questao(2L, 0, 200L);
        pratica = AtividadeAluno.adaptativa(aluno, List.of(questao1, questao2));
        ReflectionTestUtils.setField(pratica, "id", ATIVIDADE_ALUNO_ID);
        entregue1 = pratica.getQuestoes().get(0);
        entregue2 = pratica.getQuestoes().get(1);
        ReflectionTestUtils.setField(entregue1, "id", 70L);
        ReflectionTestUtils.setField(entregue2, "id", 71L);
    }

    // ---------- envio (entrega única) ----------

    @Test
    @SuppressWarnings("unchecked")
    void envioDaPraticaGravaTodasAsRespostasContabilizaNoDesempenhoEConclui() {
        Alternativa certaDa1 = questao1.getAlternativas().get(2);
        Alternativa erradaDa2 = questao2.getAlternativas().get(3);
        prepararEnvio(pratica, List.of(certaDa1, erradaDa2));
        entregue1.marcar(questao1.getAlternativas().get(0));

        service.enviar(ALUNO_ID, ATIVIDADE_ALUNO_ID, List.of(escolha(70L, 102L), escolha(71L, 203L)));

        ArgumentCaptor<List<Resposta>> salvas = ArgumentCaptor.forClass(List.class);
        verify(respostaRepository).saveAll(salvas.capture());
        assertEquals(2, salvas.getValue().size());
        assertTrue(pratica.isConcluida());
        // o que vale é o enviado; a marcação provisória deixa de existir
        assertNull(entregue1.getAlternativaMarcada());
        // prática adaptativa: desempenho atualizado no envio e respostas marcadas como contabilizadas
        verify(desempenhoService).registrarResposta(aluno, questao1, true);
        verify(desempenhoService).registrarResposta(aluno, questao2, false);
        for (Resposta resposta : salvas.getValue()) {
            assertTrue(resposta.isContabilizada());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void envioDaAtividadeRegularNaoMexeNoDesempenhoAntesDoPrazo() {
        AtividadeAluno regular = folhaRegular(LocalDateTime.now().plusDays(2));
        prepararEnvio(regular, List.of(questao1.getAlternativas().get(2), questao2.getAlternativas().get(0)));

        service.enviar(ALUNO_ID, ATIVIDADE_ALUNO_ID, List.of(escolha(80L, 102L), escolha(81L, 200L)));

        ArgumentCaptor<List<Resposta>> salvas = ArgumentCaptor.forClass(List.class);
        verify(respostaRepository).saveAll(salvas.capture());
        assertTrue(regular.isConcluida());
        // o desempenho só muda no fechamento do prazo (o aluno não descobre o resultado antes da hora)
        verifyNoInteractions(desempenhoService);
        for (Resposta resposta : salvas.getValue()) {
            assertFalse(resposta.isContabilizada());
        }
    }

    @Test
    void envioComQuestaoSemRespostaERecusado() {
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findById(ATIVIDADE_ALUNO_ID)).thenReturn(Optional.of(pratica));
        when(respostaRepository.findByAtividadeAluno(ATIVIDADE_ALUNO_ID)).thenReturn(List.of());

        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.enviar(ALUNO_ID, ATIVIDADE_ALUNO_ID, List.of(escolha(70L, 102L))));

        assertTrue(ex.getMessage().contains("Falta responder 1 questão"));
        assertFalse(pratica.isConcluida());
        verify(respostaRepository, never()).saveAll(any());
        verifyNoInteractions(desempenhoService);
    }

    @Test
    void envioDepoisDoPrazoERecusado() {
        AtividadeAluno regular = folhaRegular(LocalDateTime.now().plusDays(2));
        ReflectionTestUtils.setField(regular.getAtividade(), "prazo", LocalDateTime.now().minusMinutes(1));
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findById(ATIVIDADE_ALUNO_ID)).thenReturn(Optional.of(regular));

        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.enviar(ALUNO_ID, ATIVIDADE_ALUNO_ID, List.of(escolha(80L, 102L), escolha(81L, 200L))));

        assertTrue(ex.getMessage().contains("prazo"));
        assertFalse(regular.isConcluida());
        verify(respostaRepository, never()).saveAll(any());
    }

    @Test
    void folhaJaEnviadaNaoAceitaNovoEnvio() {
        pratica.concluir();
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findById(ATIVIDADE_ALUNO_ID)).thenReturn(Optional.of(pratica));

        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.enviar(ALUNO_ID, ATIVIDADE_ALUNO_ID, List.of(escolha(70L, 102L), escolha(71L, 200L))));

        assertTrue(ex.getMessage().contains("já foi enviada"));
        verify(respostaRepository, never()).saveAll(any());
    }

    @Test
    void rn04_alunoNaoEnviaFolhaDeOutroAluno() {
        Usuario outroAluno = Usuario.novoAluno("Beto", "a654321", "hash", aluno.getInstituicao());
        ReflectionTestUtils.setField(outroAluno, "id", 11L);
        when(usuarioRepository.findByIdParaAtualizacao(11L)).thenReturn(Optional.of(outroAluno));
        when(atividadeAlunoRepository.findById(ATIVIDADE_ALUNO_ID)).thenReturn(Optional.of(pratica));

        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.enviar(11L, ATIVIDADE_ALUNO_ID, List.of(escolha(70L, 102L), escolha(71L, 200L))));

        verify(respostaRepository, never()).saveAll(any());
        verifyNoInteractions(desempenhoService);
    }

    @Test
    void rn04_questaoDeOutraFolhaERecusada() {
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findById(ATIVIDADE_ALUNO_ID)).thenReturn(Optional.of(pratica));

        assertThrows(RecursoNaoEncontradoException.class, () -> service.enviar(ALUNO_ID, ATIVIDADE_ALUNO_ID,
                List.of(escolha(70L, 102L), escolha(71L, 200L), escolha(999L, 300L))));

        verify(respostaRepository, never()).saveAll(any());
    }

    @Test
    void rn04_alternativaDeOutraQuestaoERecusadaNoEnvio() {
        Alternativa daQuestao2 = questao2.getAlternativas().get(1);
        Alternativa certaDa2 = questao2.getAlternativas().get(0);
        prepararEnvio(pratica, List.of(daQuestao2, certaDa2));

        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.enviar(ALUNO_ID, ATIVIDADE_ALUNO_ID, List.of(escolha(70L, 201L), escolha(71L, 200L))));

        assertEquals("A alternativa escolhida não pertence a esta questão", ex.getMessage());
        assertFalse(pratica.isConcluida());
        verifyNoInteractions(desempenhoService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void respostaDadaAntesDaEntregaUnicaEMantida() {
        // folha começada no fluxo antigo: a questão 1 já tinha resposta; o envio completa só a 2
        Resposta antiga = new Resposta(entregue1, questao1.getAlternativas().get(2));
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findById(ATIVIDADE_ALUNO_ID)).thenReturn(Optional.of(pratica));
        when(respostaRepository.findByAtividadeAluno(ATIVIDADE_ALUNO_ID)).thenReturn(List.of(antiga));
        when(alternativaRepository.findAllById(any())).thenReturn(List.of(questao2.getAlternativas().get(0)));

        service.enviar(ALUNO_ID, ATIVIDADE_ALUNO_ID, List.of(escolha(71L, 200L)));

        ArgumentCaptor<List<Resposta>> salvas = ArgumentCaptor.forClass(List.class);
        verify(respostaRepository).saveAll(salvas.capture());
        assertEquals(1, salvas.getValue().size());
        assertSame(entregue2, salvas.getValue().get(0).getAtividadeAlunoQuestao());
        assertTrue(pratica.isConcluida());
        // a resposta antiga já tinha entrado no desempenho: só a nova é contabilizada agora
        verify(desempenhoService, never()).registrarResposta(aluno, questao1, true);
        verify(desempenhoService).registrarResposta(aluno, questao2, true);
    }

    // ---------- marcação provisória ----------

    @Test
    void marcarGuardaAEscolhaSemCriarRespostaNemMexerNoDesempenho() {
        Alternativa primeira = questao1.getAlternativas().get(0);
        Alternativa segunda = questao1.getAlternativas().get(3);
        when(atividadeAlunoQuestaoRepository.findById(70L)).thenReturn(Optional.of(entregue1));
        when(respostaRepository.existsByAtividadeAlunoQuestaoId(70L)).thenReturn(false);
        when(alternativaRepository.findById(100L)).thenReturn(Optional.of(primeira));
        when(alternativaRepository.findById(103L)).thenReturn(Optional.of(segunda));

        service.marcar(ALUNO_ID, ATIVIDADE_ALUNO_ID, 70L, 100L);
        // o aluno muda de ideia: a marcação é trocada, sem gastar a resposta (RN04)
        service.marcar(ALUNO_ID, ATIVIDADE_ALUNO_ID, 70L, 103L);

        assertSame(segunda, entregue1.getAlternativaMarcada());
        assertNotNull(entregue1.getMarcadaEm());
        verify(respostaRepository, never()).save(any());
        verifyNoInteractions(desempenhoService, usuarioRepository);
    }

    @Test
    void naoMarcaQuestaoJaRespondida() {
        when(atividadeAlunoQuestaoRepository.findById(70L)).thenReturn(Optional.of(entregue1));
        when(respostaRepository.existsByAtividadeAlunoQuestaoId(70L)).thenReturn(true);

        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.marcar(ALUNO_ID, ATIVIDADE_ALUNO_ID, 70L, 101L));

        assertTrue(ex.getMessage().contains("já foi respondida"));
        assertNull(entregue1.getAlternativaMarcada());
    }

    @Test
    void naoMarcaDepoisDoPrazo() {
        AtividadeAluno regular = folhaRegular(LocalDateTime.now().plusDays(2));
        ReflectionTestUtils.setField(regular.getAtividade(), "prazo", LocalDateTime.now().minusMinutes(1));
        AtividadeAlunoQuestao entregue = regular.getQuestoes().get(0);
        when(atividadeAlunoQuestaoRepository.findById(80L)).thenReturn(Optional.of(entregue));

        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.marcar(ALUNO_ID, ATIVIDADE_ALUNO_ID, 80L, 101L));

        assertTrue(ex.getMessage().contains("prazo"));
        assertNull(entregue.getAlternativaMarcada());
        verifyNoInteractions(alternativaRepository);
    }

    @Test
    void naoMarcaFolhaJaEnviada() {
        pratica.concluir();
        when(atividadeAlunoQuestaoRepository.findById(70L)).thenReturn(Optional.of(entregue1));

        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.marcar(ALUNO_ID, ATIVIDADE_ALUNO_ID, 70L, 101L));

        assertTrue(ex.getMessage().contains("já foi enviada"));
    }

    @Test
    void naoMarcaAlternativaDeOutraQuestao() {
        Alternativa deOutraQuestao = questao2.getAlternativas().get(1);
        when(atividadeAlunoQuestaoRepository.findById(70L)).thenReturn(Optional.of(entregue1));
        when(respostaRepository.existsByAtividadeAlunoQuestaoId(70L)).thenReturn(false);
        when(alternativaRepository.findById(201L)).thenReturn(Optional.of(deOutraQuestao));

        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.marcar(ALUNO_ID, ATIVIDADE_ALUNO_ID, 70L, 201L));

        assertEquals("A alternativa escolhida não pertence a esta questão", ex.getMessage());
        assertNull(entregue1.getAlternativaMarcada());
    }

    @Test
    void outroAlunoNaoMarcaNaFolhaAlheia() {
        when(atividadeAlunoQuestaoRepository.findById(70L)).thenReturn(Optional.of(entregue1));

        assertThrows(RecursoNaoEncontradoException.class, () -> service.marcar(99L, ATIVIDADE_ALUNO_ID, 70L, 101L));

        verifyNoInteractions(alternativaRepository);
    }

    // ---------- apoio ----------

    private void prepararEnvio(AtividadeAluno folha, List<Alternativa> alternativas) {
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findById(ATIVIDADE_ALUNO_ID)).thenReturn(Optional.of(folha));
        when(respostaRepository.findByAtividadeAluno(ATIVIDADE_ALUNO_ID)).thenReturn(List.of());
        when(alternativaRepository.findAllById(any())).thenReturn(alternativas);
    }

    /** Folha de uma atividade regular com as duas questões (ids 80 e 81) e o prazo informado. */
    private AtividadeAluno folhaRegular(LocalDateTime prazo) {
        Turma turma = new Turma("7º A", aluno.getInstituicao(), AnoEscolar.SETIMO_ANO);
        Atividade atividade = new Atividade("Lista", turma, null);
        ReflectionTestUtils.setField(atividade, "prazo", prazo);
        atividade.definirQuestoes(List.of(questao1, questao2));
        AtividadeAluno folha = AtividadeAluno.regular(aluno, atividade);
        ReflectionTestUtils.setField(folha, "id", ATIVIDADE_ALUNO_ID);
        ReflectionTestUtils.setField(folha.getQuestoes().get(0), "id", 80L);
        ReflectionTestUtils.setField(folha.getQuestoes().get(1), "id", 81L);
        return folha;
    }

    private static EnvioFolhaRequest.Escolha escolha(Long questaoEntregueId, Long alternativaId) {
        return new EnvioFolhaRequest.Escolha(questaoEntregueId, alternativaId);
    }

    /** Questão aprovada com 5 alternativas de ids {@code primeiroIdAlternativa}..+4. */
    private static Questao questao(Long id, int indiceCorreta, long primeiroIdAlternativa) {
        Topico topico = new Topico("Frações");
        ReflectionTestUtils.setField(topico, "id", 3L);
        Questao questao = new Questao("Questão " + id, null, topico, NivelDificuldade.FACIL, AnoEscolar.SEXTO_ANO);
        ReflectionTestUtils.setField(questao, "id", id);
        questao.definirAlternativas(List.of("a", "b", "c", "d", "e"), indiceCorreta);
        for (int i = 0; i < 5; i++) {
            ReflectionTestUtils.setField(questao.getAlternativas().get(i), "id", primeiroIdAlternativa + i);
        }
        ReflectionTestUtils.setField(questao, "status", StatusQuestao.APROVADA);
        return questao;
    }
}
