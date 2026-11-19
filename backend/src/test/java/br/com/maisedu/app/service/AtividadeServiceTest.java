package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.AtividadeAlunoResumoResponse;
import br.com.maisedu.app.dto.FolhaAtividadeResponse;
import br.com.maisedu.app.dto.ResultadoAtividadeResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.AlunoTurma;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.Atividade;
import br.com.maisedu.app.model.AtividadeAluno;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.Resposta;
import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.StatusQuestao;
import br.com.maisedu.app.model.TipoAtividade;
import br.com.maisedu.app.model.Topico;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlunoTurmaRepository;
import br.com.maisedu.app.repository.AtividadeAlunoRepository;
import br.com.maisedu.app.repository.AtividadeRepository;
import br.com.maisedu.app.repository.Projecoes;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.PlacarAtividade;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * RF15 (criar atividade com prazo), RF16/RN03 (iniciar com snapshot, reabrir com a mesma folha), a
 * correção escondida do aluno até o prazo e a análise por questão do professor.
 */
@ExtendWith(MockitoExtension.class)
class AtividadeServiceTest {

    private static final Long INSTITUICAO_ID = 1L;
    private static final Long TURMA_ID = 5L;
    private static final Long PROFESSOR_ID = 7L;
    private static final Long ALUNO_ID = 10L;
    private static final Long ATIVIDADE_ID = 40L;
    private static final LocalDateTime PRAZO = LocalDateTime.now().plusDays(3);

    @Mock AtividadeRepository atividadeRepository;
    @Mock AtividadeAlunoRepository atividadeAlunoRepository;
    @Mock QuestaoRepository questaoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock AlunoTurmaRepository alunoTurmaRepository;
    @Mock RespostaRepository respostaRepository;
    @Mock AcessoTurma acessoTurma;
    @Mock AuditoriaService auditoriaService;
    @InjectMocks AtividadeService service;

    private Turma turma;
    private Usuario professor;
    private Usuario aluno;
    private UsuarioAutenticado atorProfessor;
    private Questao q1;
    private Questao q2;

    @BeforeEach
    void setUp() {
        Instituicao escola = new Instituicao("Escola", "00.000.000/0001-00", "Rua 1");
        ReflectionTestUtils.setField(escola, "id", INSTITUICAO_ID);
        turma = new Turma("7º ano A", escola, AnoEscolar.SETIMO_ANO);
        ReflectionTestUtils.setField(turma, "id", TURMA_ID);

        professor = new Usuario("Paulo Professor", "paulo@escola.test", "hash", escola);
        ReflectionTestUtils.setField(professor, "id", PROFESSOR_ID);
        professor.adicionarRole(new Role(RoleNome.PROFESSOR));
        atorProfessor = new UsuarioAutenticado(PROFESSOR_ID, "Paulo Professor", INSTITUICAO_ID,
                EnumSet.of(RoleNome.PROFESSOR), false);

        aluno = Usuario.novoAluno("Ana Aluna", "2026001", "hash", escola);
        ReflectionTestUtils.setField(aluno, "id", ALUNO_ID);
        aluno.adicionarRole(new Role(RoleNome.ALUNO));

        q1 = questao(1L);
        q2 = questao(2L);
    }

    @Test
    void rf15Ca01_professorDaTurmaCriaAtividadeNaOrdemEscolhida() {
        dadoQueOProfessorLecionaNaTurma();
        when(questaoRepository.findAllByIdIn(List.of(2L, 1L))).thenReturn(List.of(q1, q2));
        when(usuarioRepository.findById(PROFESSOR_ID)).thenReturn(Optional.of(professor));
        when(atividadeRepository.save(any(Atividade.class))).thenAnswer(invocacao -> {
            Atividade salva = invocacao.getArgument(0);
            ReflectionTestUtils.setField(salva, "id", ATIVIDADE_ID);
            return salva;
        });

        Atividade atividade = service.criar(atorProfessor, TURMA_ID, "Revisão", PRAZO, List.of(2L, 1L));

        assertSame(turma, atividade.getTurma());
        assertEquals(PRAZO, atividade.getPrazo());
        assertSame(q2, atividade.getQuestoes().get(0).getQuestao());
        assertSame(q1, atividade.getQuestoes().get(1).getQuestao());
        verify(auditoriaService).registrar(eq(atorProfessor), eq(AcaoAuditoria.CRIACAO_ATIVIDADE), eq("Atividade"),
                eq(ATIVIDADE_ID), eq(true), anyString());
    }

    @Test
    void rf15Ca03_professorQueNaoLecionaNaTurmaNaoCriaAtividade() {
        when(acessoTurma.buscarComoProfessorDaTurma(eq(atorProfessor), eq(TURMA_ID), anyString()))
                .thenThrow(new AccessDeniedException("Só o professor que leciona na turma pode criar atividades para ela."));

        assertThrows(AccessDeniedException.class,
                () -> service.criar(atorProfessor, TURMA_ID, "Revisão", PRAZO, List.of(1L)));

        verifyNoInteractions(questaoRepository, atividadeRepository);
    }

    @Test
    void turmaInativaNaoRecebeAtividade() {
        turma.setAtivo(false);
        dadoQueOProfessorLecionaNaTurma();

        assertThrows(NegocioException.class, () -> service.criar(atorProfessor, TURMA_ID, "Revisão", PRAZO, List.of(1L)));

        verify(atividadeRepository, never()).save(any());
    }

    @Test
    void questaoInexistenteERecusada() {
        dadoQueOProfessorLecionaNaTurma();
        when(questaoRepository.findAllByIdIn(List.of(1L, 99L))).thenReturn(List.of(q1));

        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.criar(atorProfessor, TURMA_ID, "Revisão", PRAZO, List.of(1L, 99L)));

        assertEquals("Questão 99 não encontrada", ex.getMessage());
    }

    @Test
    void rf16Ca03_reabrirAtividadeDevolveAMesmaFolha() {
        Atividade atividade = atividadeDaTurma();
        AtividadeAluno existente = AtividadeAluno.regular(aluno, atividade);
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
        when(alunoTurmaRepository.existsByAlunoIdAndTurmaId(ALUNO_ID, TURMA_ID)).thenReturn(true);
        when(atividadeAlunoRepository.findByAlunoIdAndAtividadeId(ALUNO_ID, ATIVIDADE_ID))
                .thenReturn(Optional.of(existente));

        AtividadeAluno reaberta = service.iniciar(ALUNO_ID, ATIVIDADE_ID);

        assertSame(existente, reaberta);
        verify(atividadeAlunoRepository, never()).save(any());
    }

    @Test
    void rn03_iniciarFixaAsQuestoesEntregues() {
        Atividade atividade = atividadeDaTurma();
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
        when(alunoTurmaRepository.existsByAlunoIdAndTurmaId(ALUNO_ID, TURMA_ID)).thenReturn(true);
        when(atividadeAlunoRepository.findByAlunoIdAndAtividadeId(ALUNO_ID, ATIVIDADE_ID)).thenReturn(Optional.empty());
        when(atividadeAlunoRepository.save(any(AtividadeAluno.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        AtividadeAluno iniciada = service.iniciar(ALUNO_ID, ATIVIDADE_ID);

        assertEquals(TipoAtividade.REGULAR, iniciada.getTipo());
        assertEquals(2, iniciada.getQuestoes().size());
        assertSame(q1, iniciada.getQuestoes().get(0).getQuestao());
        verify(auditoriaService).registrar(eq(ALUNO_ID), eq("Ana Aluna"), isNull(), eq(AcaoAuditoria.INICIO_ATIVIDADE),
                eq("AtividadeAluno"), any(), eq(true), anyString());
    }

    @Test
    void alunoDeOutraTurmaNaoIniciaAAtividade() {
        Atividade atividade = atividadeDaTurma();
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
        when(alunoTurmaRepository.existsByAlunoIdAndTurmaId(ALUNO_ID, TURMA_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.iniciar(ALUNO_ID, ATIVIDADE_ID));

        verify(atividadeAlunoRepository, never()).save(any());
    }

    @Test
    void prazoNoPassadoOuEmCimaDaHoraERecusado() {
        dadoQueOProfessorLecionaNaTurma();
        when(questaoRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(q1));
        when(usuarioRepository.findById(PROFESSOR_ID)).thenReturn(Optional.of(professor));

        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.criar(atorProfessor, TURMA_ID, "Revisão", LocalDateTime.now().plusMinutes(1), List.of(1L)));

        assertTrue(ex.getMessage().contains("prazo de entrega"));
        verify(atividadeRepository, never()).save(any());
    }

    @Test
    void depoisDoPrazoAAtividadeNaoPodeSerIniciada() {
        Atividade atividade = atividadeDaTurma();
        ReflectionTestUtils.setField(atividade, "prazo", LocalDateTime.now().minusHours(1));
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
        when(alunoTurmaRepository.existsByAlunoIdAndTurmaId(ALUNO_ID, TURMA_ID)).thenReturn(true);
        when(atividadeAlunoRepository.findByAlunoIdAndAtividadeId(ALUNO_ID, ATIVIDADE_ID)).thenReturn(Optional.empty());

        NegocioException ex = assertThrows(NegocioException.class, () -> service.iniciar(ALUNO_ID, ATIVIDADE_ID));

        assertTrue(ex.getMessage().contains("prazo"));
        verify(atividadeAlunoRepository, never()).save(any());
    }

    @Test
    void depoisDoPrazoQuemJaComecouReabreAMesmaFolhaParaVerACorrecao() {
        Atividade atividade = atividadeDaTurma();
        AtividadeAluno existente = AtividadeAluno.regular(aluno, atividade);
        ReflectionTestUtils.setField(atividade, "prazo", LocalDateTime.now().minusHours(1));
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
        when(alunoTurmaRepository.existsByAlunoIdAndTurmaId(ALUNO_ID, TURMA_ID)).thenReturn(true);
        when(atividadeAlunoRepository.findByAlunoIdAndAtividadeId(ALUNO_ID, ATIVIDADE_ID))
                .thenReturn(Optional.of(existente));

        assertSame(existente, service.iniciar(ALUNO_ID, ATIVIDADE_ID));
    }

    @Test
    void professorDaTurmaEstendeOPrazoAntesDeEleTerminar() {
        Atividade atividade = atividadeDaTurma();
        when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
        when(acessoTurma.buscarComoProfessorDaTurma(eq(atorProfessor), eq(TURMA_ID), anyString())).thenReturn(turma);
        LocalDateTime novoPrazo = LocalDateTime.now().plusDays(10);

        service.alterarPrazo(atorProfessor, ATIVIDADE_ID, novoPrazo);

        assertEquals(novoPrazo, atividade.getPrazo());
        verify(auditoriaService).registrar(eq(atorProfessor), eq(AcaoAuditoria.ALTERACAO_PRAZO_ATIVIDADE),
                eq("Atividade"), eq(ATIVIDADE_ID), eq(true), anyString());
    }

    @Test
    void prazoQueJaTerminouNaoPodeSerAlterado() {
        Atividade atividade = atividadeDaTurma();
        LocalDateTime vencido = LocalDateTime.now().minusMinutes(5);
        ReflectionTestUtils.setField(atividade, "prazo", vencido);
        when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
        when(acessoTurma.buscarComoProfessorDaTurma(eq(atorProfessor), eq(TURMA_ID), anyString())).thenReturn(turma);

        assertThrows(NegocioException.class,
                () -> service.alterarPrazo(atorProfessor, ATIVIDADE_ID, LocalDateTime.now().plusDays(2)));

        assertEquals(vencido, atividade.getPrazo());
        verifyNoInteractions(auditoriaService);
    }

    @Test
    void antesDoPrazoAFolhaEnviadaMostraAsEscolhasMasNaoACorrecao() {
        Questao comAlternativas = questaoCompleta(3L, 1, 300L);
        Atividade atividade = new Atividade("Revisão", turma, professor);
        atividade.definirQuestoes(List.of(comAlternativas));
        AtividadeAluno folha = AtividadeAluno.regular(aluno, atividade);
        ReflectionTestUtils.setField(folha, "id", 900L);
        ReflectionTestUtils.setField(folha.getQuestoes().get(0), "id", 70L);
        folha.concluir();
        when(respostaRepository.findByAtividadeAluno(900L))
                .thenReturn(List.of(new Resposta(folha.getQuestoes().get(0), comAlternativas.getAlternativas().get(1))));

        FolhaAtividadeResponse antes = service.montarFolha(folha);

        assertFalse(antes.correcaoLiberada());
        assertFalse(antes.podeResponder());
        assertNull(antes.acertos());
        assertEquals(301L, antes.questoes().get(0).alternativaEscolhidaId());
        assertNull(antes.questoes().get(0).correcao());

        // depois do prazo: gabarito e acerto aparecem
        ReflectionTestUtils.setField(atividade, "prazo", LocalDateTime.now().minusMinutes(1));
        FolhaAtividadeResponse depois = service.montarFolha(folha);

        assertTrue(depois.correcaoLiberada());
        assertEquals(1, depois.acertos());
        assertNotNull(depois.questoes().get(0).correcao());
        assertTrue(depois.questoes().get(0).correcao().acertou());
        assertEquals('B', depois.questoes().get(0).correcao().letraCorreta());
    }

    @Test
    void resultadoTrazAAlternativaErradaMaisEscolhidaEmCadaQuestao() {
        Questao comAlternativas = questaoCompleta(3L, 0, 300L);
        Atividade atividade = new Atividade("Revisão", turma, professor);
        atividade.definirQuestoes(List.of(comAlternativas));
        ReflectionTestUtils.setField(atividade, "id", ATIVIDADE_ID);
        when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
        when(atividadeAlunoRepository.findByAtividadeId(ATIVIDADE_ID)).thenReturn(List.of());
        when(alunoTurmaRepository.findByTurmaIdComAluno(TURMA_ID)).thenReturn(List.of(new AlunoTurma(aluno, turma)));
        // 4 acertaram (A), 3 marcaram C e 1 marcou D
        when(respostaRepository.contarAlternativasDaAtividade(ATIVIDADE_ID)).thenReturn(List.of(
                new Projecoes.ContagemAlternativa(3L, 300L, 4L),
                new Projecoes.ContagemAlternativa(3L, 302L, 3L),
                new Projecoes.ContagemAlternativa(3L, 303L, 1L)));

        ResultadoAtividadeResponse resultado = service.resultados(atorProfessor, ATIVIDADE_ID);

        ResultadoAtividadeResponse.AnaliseQuestao analise = resultado.analise().get(0);
        assertEquals(8, analise.respostas());
        assertEquals(4, analise.acertos());
        assertEquals(0, analise.percentualAcerto().compareTo(new java.math.BigDecimal("50.00")));
        assertEquals('C', analise.erroMaisComum().letra());
        assertEquals(3, analise.erroMaisComum().vezes());
        assertEquals(0, analise.erroMaisComum().percentualDosErros().compareTo(new java.math.BigDecimal("75.00")));
        // quem não começou aparece como "não iniciada" enquanto o prazo está aberto
        assertEquals(AtividadeAlunoResumoResponse.NAO_INICIADA, resultado.alunos().get(0).situacao());
    }

    @Test
    void listaDoAlunoJuntaNaoIniciadasEmAndamentoEPraticasComPlacarContadoNoBanco() {
        Atividade iniciada = atividadeDaTurma();
        Atividade nova = new Atividade("Frações", turma, professor);
        nova.definirQuestoes(List.of(q1));
        ReflectionTestUtils.setField(nova, "id", 41L);
        AtividadeAluno folha = AtividadeAluno.regular(aluno, iniciada);
        ReflectionTestUtils.setField(folha, "id", 900L);
        AtividadeAluno pratica = AtividadeAluno.adaptativa(aluno, List.of(q1, q2));
        ReflectionTestUtils.setField(pratica, "id", 901L);
        pratica.concluir();
        when(atividadeAlunoRepository.findByAlunoIdComAtividade(ALUNO_ID)).thenReturn(List.of(pratica, folha));
        when(respostaRepository.contarPorAtividadeAluno(Set.of(900L, 901L))).thenReturn(List.of(
                new PlacarAtividade(900L, 2L, 1L), new PlacarAtividade(901L, 1L, 1L)));
        when(atividadeRepository.findDisponiveisParaAluno(ALUNO_ID)).thenReturn(List.of(iniciada, nova));

        List<AtividadeAlunoResumoResponse> lista = service.listarDoAluno(ALUNO_ID);

        assertEquals(3, lista.size());
        assertEquals(900L, lista.get(0).atividadeAlunoId());
        assertEquals(AtividadeAlunoResumoResponse.EM_ANDAMENTO, lista.get(0).situacao());
        assertEquals(2, lista.get(0).escolhidas());
        // atividade regular dentro do prazo: o aluno não vê os acertos
        assertNull(lista.get(0).acertos());
        assertFalse(lista.get(0).correcaoLiberada());
        assertEquals(AtividadeAlunoResumoResponse.NAO_INICIADA, lista.get(1).situacao());
        assertEquals(41L, lista.get(1).atividadeId());
        assertEquals(TipoAtividade.ADAPTATIVA.name(), lista.get(2).tipo());
        // prática enviada: correção liberada na hora
        assertEquals(AtividadeAlunoResumoResponse.ENTREGUE, lista.get(2).situacao());
        assertEquals(1, lista.get(2).acertos());
        verify(atividadeAlunoRepository, never()).findByAlunoIdAndAtividadeId(any(), any());
        verify(respostaRepository, never()).findByAtividadeAluno(any());
    }

    private void dadoQueOProfessorLecionaNaTurma() {
        when(acessoTurma.buscarComoProfessorDaTurma(eq(atorProfessor), eq(TURMA_ID), anyString())).thenReturn(turma);
    }

    private Atividade atividadeDaTurma() {
        Atividade atividade = new Atividade("Revisão", turma, professor);
        atividade.definirQuestoes(List.of(q1, q2));
        ReflectionTestUtils.setField(atividade, "id", ATIVIDADE_ID);
        return atividade;
    }

    /** Questão aprovada com tópico e 5 alternativas de ids {@code primeiroIdAlternativa}..+4. */
    private Questao questaoCompleta(Long id, int indiceCorreta, long primeiroIdAlternativa) {
        Topico topico = new Topico("Frações");
        ReflectionTestUtils.setField(topico, "id", 3L);
        Questao questao = new Questao("Questão " + id, professor, topico, NivelDificuldade.FACIL, AnoEscolar.SETIMO_ANO);
        ReflectionTestUtils.setField(questao, "id", id);
        questao.definirAlternativas(List.of("a", "b", "c", "d", "e"), indiceCorreta);
        for (int i = 0; i < 5; i++) {
            ReflectionTestUtils.setField(questao.getAlternativas().get(i), "id", primeiroIdAlternativa + i);
        }
        ReflectionTestUtils.setField(questao, "status", StatusQuestao.APROVADA);
        return questao;
    }

    private static Questao questao(Long id) {
        Questao questao = new Questao("Questão " + id, null, null, NivelDificuldade.FACIL, AnoEscolar.SETIMO_ANO);
        ReflectionTestUtils.setField(questao, "id", id);
        ReflectionTestUtils.setField(questao, "status", StatusQuestao.APROVADA);
        return questao;
    }
}
