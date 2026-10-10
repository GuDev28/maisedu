package br.com.maisedu.app.service;

import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.AlunoTurma;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.ProfessorTurma;
import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlunoTurmaRepository;
import br.com.maisedu.app.repository.ProfessorTurmaRepository;
import br.com.maisedu.app.repository.TurmaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * RN1 — Vínculo e elegibilidade aluno-turma-professor.
 * Teste unitário: repositórios e auditoria são mocks; nada de banco nem contexto do Spring.
 */
@ExtendWith(MockitoExtension.class)
class VinculoTurmaServiceTest {

    private static final long INSTITUICAO_ID = 1L;
    private static final long OUTRA_INSTITUICAO_ID = 2L;
    private static final long TURMA_ID = 10L;
    private static final long ALUNO_ID = 100L;
    private static final long PROFESSOR_ID = 200L;
    private static final long ADMIN_ID = 300L;

    @Mock UsuarioRepository usuarioRepository;
    @Mock TurmaRepository turmaRepository;
    @Mock AlunoTurmaRepository alunoTurmaRepository;
    @Mock ProfessorTurmaRepository professorTurmaRepository;
    @Mock AuditoriaService auditoriaService;
    @InjectMocks VinculoTurmaService service;

    private Instituicao escola;
    private Turma turma;
    private Usuario aluno;
    private Usuario professor;
    private UsuarioAutenticado atorProfessor;
    private UsuarioAutenticado atorAdmin;

    @BeforeEach
    void setUp() {
        escola = instituicao(INSTITUICAO_ID);

        turma = new Turma("7º ano A", escola, AnoEscolar.SETIMO_ANO);
        ReflectionTestUtils.setField(turma, "id", TURMA_ID);

        aluno = Usuario.novoAluno("Ana Aluna", "2026001", "hash", escola);
        ReflectionTestUtils.setField(aluno, "id", ALUNO_ID);
        aluno.adicionarRole(new Role(RoleNome.ALUNO));

        professor = new Usuario("Paulo Professor", "paulo@escola.test", "hash", escola);
        ReflectionTestUtils.setField(professor, "id", PROFESSOR_ID);
        professor.adicionarRole(new Role(RoleNome.PROFESSOR));

        atorProfessor = new UsuarioAutenticado(PROFESSOR_ID, "Paulo Professor", INSTITUICAO_ID,
                EnumSet.of(RoleNome.PROFESSOR), false);
        atorAdmin = new UsuarioAutenticado(ADMIN_ID, "Admin", INSTITUICAO_ID,
                EnumSet.of(RoleNome.ADMIN, RoleNome.PROFESSOR), false);
    }

    private static Instituicao instituicao(long id) {
        Instituicao instituicao = new Instituicao("Escola " + id, "00.000.000/000" + id + "-00", "Rua " + id);
        ReflectionTestUtils.setField(instituicao, "id", id);
        return instituicao;
    }

    @Test
    void deveVincularAlunoQuandoProfessorLecionaNaTurma() {
        // Arrange
        when(turmaRepository.findById(TURMA_ID)).thenReturn(Optional.of(turma));
        when(professorTurmaRepository.existsByProfessorIdAndTurmaId(PROFESSOR_ID, TURMA_ID)).thenReturn(true);
        when(usuarioRepository.findById(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(alunoTurmaRepository.existsByAlunoIdAndTurmaId(ALUNO_ID, TURMA_ID)).thenReturn(false);
        when(alunoTurmaRepository.save(any(AlunoTurma.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        // Act
        AlunoTurma vinculo = service.vincularAluno(ALUNO_ID, TURMA_ID, atorProfessor);

        // Assert
        assertSame(aluno, vinculo.getAluno());
        assertSame(turma, vinculo.getTurma());
        verify(alunoTurmaRepository).save(any(AlunoTurma.class));
        verify(auditoriaService).registrar(atorProfessor, AcaoAuditoria.VINCULO_ALUNO_TURMA,
                "Turma", TURMA_ID, true, "aluno " + ALUNO_ID);
    }

    @Test
    void deveVincularProfessorQuandoAdminEDaMesmaInstituicaoDaTurma() {
        // Arrange
        when(usuarioRepository.findById(PROFESSOR_ID)).thenReturn(Optional.of(professor));
        when(turmaRepository.findById(TURMA_ID)).thenReturn(Optional.of(turma));
        when(professorTurmaRepository.existsByProfessorIdAndTurmaId(PROFESSOR_ID, TURMA_ID)).thenReturn(false);
        when(professorTurmaRepository.save(any(ProfessorTurma.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        // Act
        ProfessorTurma vinculo = service.vincularProfessor(PROFESSOR_ID, TURMA_ID, atorAdmin);

        // Assert
        assertSame(professor, vinculo.getProfessor());
        assertSame(turma, vinculo.getTurma());
        verify(auditoriaService).registrar(atorAdmin, AcaoAuditoria.VINCULO_PROFESSOR_TURMA,
                "Turma", TURMA_ID, true, "professor " + PROFESSOR_ID);
    }

    @Test
    void deveLancarExcecaoQuandoAlunoJaEstaVinculadoATurma() {
        // Arrange
        when(turmaRepository.findById(TURMA_ID)).thenReturn(Optional.of(turma));
        when(professorTurmaRepository.existsByProfessorIdAndTurmaId(PROFESSOR_ID, TURMA_ID)).thenReturn(true);
        when(usuarioRepository.findById(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(alunoTurmaRepository.existsByAlunoIdAndTurmaId(ALUNO_ID, TURMA_ID)).thenReturn(true);

        // Act
        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.vincularAluno(ALUNO_ID, TURMA_ID, atorProfessor));

        // Assert
        assertEquals("Aluno já vinculado a esta turma.", ex.getMessage());
        verify(alunoTurmaRepository, never()).save(any());
        verifyNoInteractions(auditoriaService);
    }

    @Test
    void deveLancarExcecaoQuandoAlunoEDeOutraInstituicao() {
        // Arrange
        Usuario alunoDeOutraEscola = Usuario.novoAluno("Bruno Aluno", "2026999", "hash",
                instituicao(OUTRA_INSTITUICAO_ID));
        ReflectionTestUtils.setField(alunoDeOutraEscola, "id", 101L);
        alunoDeOutraEscola.adicionarRole(new Role(RoleNome.ALUNO));
        when(turmaRepository.findById(TURMA_ID)).thenReturn(Optional.of(turma));
        when(professorTurmaRepository.existsByProfessorIdAndTurmaId(PROFESSOR_ID, TURMA_ID)).thenReturn(true);
        when(usuarioRepository.findById(101L)).thenReturn(Optional.of(alunoDeOutraEscola));

        // Act
        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.vincularAluno(101L, TURMA_ID, atorProfessor));

        // Assert
        assertEquals("Usuário e turma pertencem a instituições diferentes.", ex.getMessage());
        verify(alunoTurmaRepository, never()).save(any());
    }

    @Test
    void deveNegarVinculoQuandoProfessorNaoLecionaNaTurma() {
        // Arrange
        when(turmaRepository.findById(TURMA_ID)).thenReturn(Optional.of(turma));
        when(professorTurmaRepository.existsByProfessorIdAndTurmaId(PROFESSOR_ID, TURMA_ID)).thenReturn(false);

        // Act
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.vincularAluno(ALUNO_ID, TURMA_ID, atorProfessor));

        // Assert
        assertEquals("Você não leciona nesta turma.", ex.getMessage());
        verify(usuarioRepository, never()).findById(any());
        verify(alunoTurmaRepository, never()).save(any());
    }

    @ParameterizedTest(name = "usuário com papel {0} não pode ser vinculado como aluno")
    @EnumSource(value = RoleNome.class, names = {"PROFESSOR", "ADMIN"})
    void deveRecusarVinculoComoAlunoDeUsuarioSemPapelDeAluno(RoleNome papelDoUsuario) {
        // Arrange
        Usuario usuarioSemPapelDeAluno = new Usuario("Carla", "carla@escola.test", "hash", escola);
        ReflectionTestUtils.setField(usuarioSemPapelDeAluno, "id", 102L);
        usuarioSemPapelDeAluno.adicionarRole(new Role(papelDoUsuario));
        when(turmaRepository.findById(TURMA_ID)).thenReturn(Optional.of(turma));
        when(professorTurmaRepository.existsByProfessorIdAndTurmaId(PROFESSOR_ID, TURMA_ID)).thenReturn(true);
        when(usuarioRepository.findById(102L)).thenReturn(Optional.of(usuarioSemPapelDeAluno));

        // Act
        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.vincularAluno(102L, TURMA_ID, atorProfessor));

        // Assert
        assertEquals("Usuário não possui o papel de ALUNO necessário para este vínculo.", ex.getMessage());
        verify(alunoTurmaRepository, never()).save(any());
    }

    @Test
    void deveRetornarListaVaziaQuandoTodasAsTurmasDoProfessorEstaoInativas() {
        // Arrange
        turma.setAtivo(false);
        when(professorTurmaRepository.findByProfessorId(PROFESSOR_ID))
                .thenReturn(List.of(new ProfessorTurma(professor, turma)));

        // Act
        List<Turma> turmas = service.listarTurmasParaVinculo(atorProfessor);

        // Assert
        assertTrue(turmas.isEmpty());
        verify(turmaRepository, never()).findByInstituicaoIdAndAtivoTrueOrderByNomeAsc(any());
    }
}
