package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.RespostaResultadoResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
import br.com.maisedu.app.model.Alternativa;
import br.com.maisedu.app.model.AnoEscolar;
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
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlternativaRepository;
import br.com.maisedu.app.repository.AtividadeAlunoQuestaoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RF16, RN04 e RF17: resposta única, só a questões entregues ao próprio aluno, com desempenho atualizado. */
@ExtendWith(MockitoExtension.class)
class RespostaServiceTest {

    private static final Long ALUNO_ID = 10L;
    private static final Long ATIVIDADE_ALUNO_ID = 50L;

    @Mock UsuarioRepository usuarioRepository;
    @Mock AtividadeAlunoQuestaoRepository atividadeAlunoQuestaoRepository;
    @Mock AlternativaRepository alternativaRepository;
    @Mock RespostaRepository respostaRepository;
    @Mock DesempenhoService desempenhoService;
    @InjectMocks RespostaService service;

    private Usuario aluno;
    private Questao questao1;
    private Questao questao2;
    private AtividadeAluno atividadeAluno;
    private AtividadeAlunoQuestao entregue1;

    @BeforeEach
    void setUp() {
        Instituicao escola = new Instituicao("Escola", "00.000.000/0001-00", "Rua 1");
        ReflectionTestUtils.setField(escola, "id", 1L);
        aluno = Usuario.novoAluno("Ana Aluna", "2026001", "hash", escola);
        ReflectionTestUtils.setField(aluno, "id", ALUNO_ID);
        aluno.adicionarRole(new Role(RoleNome.ALUNO));

        questao1 = questao(1L, 2, 100L);
        questao2 = questao(2L, 0, 200L);
        atividadeAluno = AtividadeAluno.adaptativa(aluno, List.of(questao1, questao2));
        ReflectionTestUtils.setField(atividadeAluno, "id", ATIVIDADE_ALUNO_ID);
        entregue1 = atividadeAluno.getQuestoes().get(0);
        ReflectionTestUtils.setField(entregue1, "id", 70L);
        ReflectionTestUtils.setField(atividadeAluno.getQuestoes().get(1), "id", 71L);
    }

    @Test
    void rf16Ca01_respostaCertaERegistradaCorrigidaEAtualizaODesempenho() {
        Alternativa correta = questao1.getAlternativas().get(2);
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoQuestaoRepository.findById(70L)).thenReturn(Optional.of(entregue1));
        when(respostaRepository.existsByAtividadeAlunoQuestaoId(70L)).thenReturn(false);
        when(alternativaRepository.findById(102L)).thenReturn(Optional.of(correta));
        when(respostaRepository.save(any(Resposta.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(respostaRepository.findByAtividadeAluno(ATIVIDADE_ALUNO_ID))
                .thenReturn(List.of(new Resposta(entregue1, correta)));

        RespostaResultadoResponse resultado = service.responder(ALUNO_ID, ATIVIDADE_ALUNO_ID, 70L, 102L);

        assertTrue(resultado.acertou());
        assertEquals(102L, resultado.alternativaCorretaId());
        assertEquals('C', resultado.letraCorreta());
        assertEquals(1, resultado.respondidas());
        assertEquals(1, resultado.acertos());
        assertEquals(2, resultado.totalQuestoes());
        // Ainda falta uma questão: a prática não é concluída
        assertFalse(resultado.atividadeConcluida());
        // RF17/CA03: desempenho atualizado na mesma operação
        verify(desempenhoService).registrarResposta(aluno, questao1, true);
    }

    @Test
    void ultimaRespostaConcluiAAtividade() {
        AtividadeAlunoQuestao entregue2 = atividadeAluno.getQuestoes().get(1);
        Alternativa errada = questao2.getAlternativas().get(3);
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoQuestaoRepository.findById(71L)).thenReturn(Optional.of(entregue2));
        when(respostaRepository.existsByAtividadeAlunoQuestaoId(71L)).thenReturn(false);
        when(alternativaRepository.findById(203L)).thenReturn(Optional.of(errada));
        when(respostaRepository.save(any(Resposta.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(respostaRepository.findByAtividadeAluno(ATIVIDADE_ALUNO_ID)).thenReturn(List.of(
                new Resposta(entregue1, questao1.getAlternativas().get(2)),
                new Resposta(entregue2, errada)));

        RespostaResultadoResponse resultado = service.responder(ALUNO_ID, ATIVIDADE_ALUNO_ID, 71L, 203L);

        assertFalse(resultado.acertou());
        assertEquals('A', resultado.letraCorreta());
        assertEquals(2, resultado.respondidas());
        assertEquals(1, resultado.acertos());
        assertTrue(resultado.atividadeConcluida());
        assertTrue(atividadeAluno.isConcluida());
        verify(desempenhoService).registrarResposta(aluno, questao2, false);
    }

    @Test
    void rf16Ca02_questaoJaRespondidaNaoAceitaNovaResposta() {
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoQuestaoRepository.findById(70L)).thenReturn(Optional.of(entregue1));
        when(respostaRepository.existsByAtividadeAlunoQuestaoId(70L)).thenReturn(true);

        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.responder(ALUNO_ID, ATIVIDADE_ALUNO_ID, 70L, 101L));

        assertTrue(ex.getMessage().contains("já foi respondida"));
        verify(respostaRepository, never()).save(any());
        verifyNoInteractions(alternativaRepository, desempenhoService);
    }

    @Test
    void rn04_alunoNaoRespondeQuestaoEntregueAOutroAluno() {
        Usuario outroAluno = Usuario.novoAluno("Beto", "2026002", "hash", aluno.getInstituicao());
        ReflectionTestUtils.setField(outroAluno, "id", 11L);
        when(usuarioRepository.findByIdParaAtualizacao(11L)).thenReturn(Optional.of(outroAluno));
        when(atividadeAlunoQuestaoRepository.findById(70L)).thenReturn(Optional.of(entregue1));

        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.responder(11L, ATIVIDADE_ALUNO_ID, 70L, 102L));

        verify(respostaRepository, never()).save(any());
        verifyNoInteractions(desempenhoService);
    }

    @Test
    void rn04_questaoPrecisaSerDaAtividadeInformada() {
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoQuestaoRepository.findById(70L)).thenReturn(Optional.of(entregue1));

        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.responder(ALUNO_ID, 999L, 70L, 102L));

        verify(respostaRepository, never()).save(any());
    }

    @Test
    void rn04_alternativaDeOutraQuestaoERecusada() {
        Alternativa daOutraQuestao = questao2.getAlternativas().get(0);
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoQuestaoRepository.findById(70L)).thenReturn(Optional.of(entregue1));
        when(respostaRepository.existsByAtividadeAlunoQuestaoId(70L)).thenReturn(false);
        when(alternativaRepository.findById(201L)).thenReturn(Optional.of(daOutraQuestao));

        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.responder(ALUNO_ID, ATIVIDADE_ALUNO_ID, 70L, 201L));

        assertEquals("A alternativa escolhida não pertence a esta questão", ex.getMessage());
        verify(respostaRepository, never()).save(any());
        verifyNoInteractions(desempenhoService);
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
        questao.setStatus(StatusQuestao.APROVADA);
        return questao;
    }
}
