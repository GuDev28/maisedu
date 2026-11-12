package br.com.maisedu.app.service;

import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.Atividade;
import br.com.maisedu.app.model.AtividadeAluno;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.StatusQuestao;
import br.com.maisedu.app.model.TipoAtividade;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlunoTurmaRepository;
import br.com.maisedu.app.repository.AtividadeAlunoRepository;
import br.com.maisedu.app.repository.AtividadeRepository;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.TurmaRepository;
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

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RF15 (criar atividade) e RF16/RN03 (iniciar com snapshot, reabrir com a mesma folha). */
@ExtendWith(MockitoExtension.class)
class AtividadeServiceTest {

    private static final Long INSTITUICAO_ID = 1L;
    private static final Long TURMA_ID = 5L;
    private static final Long PROFESSOR_ID = 7L;
    private static final Long ALUNO_ID = 10L;
    private static final Long ATIVIDADE_ID = 40L;

    @Mock AtividadeRepository atividadeRepository;
    @Mock AtividadeAlunoRepository atividadeAlunoRepository;
    @Mock TurmaRepository turmaRepository;
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
        when(turmaRepository.findById(TURMA_ID)).thenReturn(Optional.of(turma));
        when(acessoTurma.lecionaNaTurma(atorProfessor, TURMA_ID)).thenReturn(true);
        when(questaoRepository.findAllByIdIn(List.of(2L, 1L))).thenReturn(List.of(q1, q2));
        when(usuarioRepository.findById(PROFESSOR_ID)).thenReturn(Optional.of(professor));
        when(atividadeRepository.save(any(Atividade.class))).thenAnswer(invocacao -> {
            Atividade salva = invocacao.getArgument(0);
            ReflectionTestUtils.setField(salva, "id", ATIVIDADE_ID);
            return salva;
        });

        Atividade atividade = service.criar(atorProfessor, TURMA_ID, "Revisão", List.of(2L, 1L));

        assertSame(turma, atividade.getTurma());
        assertSame(q2, atividade.getQuestoes().get(0).getQuestao());
        assertSame(q1, atividade.getQuestoes().get(1).getQuestao());
        verify(auditoriaService).registrar(eq(atorProfessor), eq(AcaoAuditoria.CRIACAO_ATIVIDADE), eq("Atividade"),
                eq(ATIVIDADE_ID), eq(true), anyString());
    }

    @Test
    void rf15Ca03_professorQueNaoLecionaNaTurmaNaoCriaAtividade() {
        when(turmaRepository.findById(TURMA_ID)).thenReturn(Optional.of(turma));
        when(acessoTurma.lecionaNaTurma(atorProfessor, TURMA_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.criar(atorProfessor, TURMA_ID, "Revisão", List.of(1L)));

        verifyNoInteractions(questaoRepository, atividadeRepository);
    }

    @Test
    void turmaInativaNaoRecebeAtividade() {
        turma.setAtivo(false);
        when(turmaRepository.findById(TURMA_ID)).thenReturn(Optional.of(turma));
        when(acessoTurma.lecionaNaTurma(atorProfessor, TURMA_ID)).thenReturn(true);

        assertThrows(NegocioException.class, () -> service.criar(atorProfessor, TURMA_ID, "Revisão", List.of(1L)));

        verify(atividadeRepository, never()).save(any());
    }

    @Test
    void questaoInexistenteERecusada() {
        when(turmaRepository.findById(TURMA_ID)).thenReturn(Optional.of(turma));
        when(acessoTurma.lecionaNaTurma(atorProfessor, TURMA_ID)).thenReturn(true);
        when(questaoRepository.findAllByIdIn(List.of(1L, 99L))).thenReturn(List.of(q1));

        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.criar(atorProfessor, TURMA_ID, "Revisão", List.of(1L, 99L)));

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
        when(atividadeAlunoRepository.save(any(AtividadeAluno.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        AtividadeAluno iniciada = service.iniciar(ALUNO_ID, ATIVIDADE_ID);

        assertEquals(TipoAtividade.REGULAR, iniciada.getTipo());
        assertEquals(2, iniciada.getQuestoes().size());
        assertSame(q1, iniciada.getQuestoes().get(0).getQuestao());
        verify(auditoriaService).registrar(eq(ALUNO_ID), eq("Ana Aluna"), eq(AcaoAuditoria.INICIO_ATIVIDADE),
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

    private Atividade atividadeDaTurma() {
        Atividade atividade = new Atividade("Revisão", turma, professor);
        atividade.definirQuestoes(List.of(q1, q2));
        ReflectionTestUtils.setField(atividade, "id", ATIVIDADE_ID);
        return atividade;
    }

    private static Questao questao(Long id) {
        Questao questao = new Questao("Questão " + id, null, null, NivelDificuldade.FACIL, AnoEscolar.SETIMO_ANO);
        ReflectionTestUtils.setField(questao, "id", id);
        questao.setStatus(StatusQuestao.APROVADA);
        return questao;
    }
}
