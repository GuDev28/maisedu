package br.com.maisedu.app.service;

import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.repository.InstituicaoRepository;
import br.com.maisedu.app.repository.TurmaRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RF06: cadastro e gestão de turmas pelo administrador. */
@ExtendWith(MockitoExtension.class)
class TurmaServiceTest {

    private static final Long INSTITUICAO_ID = 1L;

    @Mock TurmaRepository turmaRepository;
    @Mock InstituicaoRepository instituicaoRepository;
    @Mock AuditoriaService auditoriaService;
    @InjectMocks TurmaService service;

    private Instituicao escola;
    private UsuarioAutenticado admin;
    private UsuarioAutenticado professor;

    @BeforeEach
    void setUp() {
        escola = new Instituicao("Escola", "00.000.000/0001-00", "Rua 1");
        ReflectionTestUtils.setField(escola, "id", INSTITUICAO_ID);
        admin = new UsuarioAutenticado(
                1L, "Admin", INSTITUICAO_ID, EnumSet.of(RoleNome.ADMIN, RoleNome.PROFESSOR), false);
        professor = new UsuarioAutenticado(7L, "Paulo", INSTITUICAO_ID, EnumSet.of(RoleNome.PROFESSOR), false);
    }

    @Test
    void rf06Ca01_adminCadastraTurmaAtivaNaPropriaInstituicao() {
        when(turmaRepository.existsByInstituicaoIdAndNomeIgnoreCase(INSTITUICAO_ID, "7º ano A")).thenReturn(false);
        when(instituicaoRepository.findById(INSTITUICAO_ID)).thenReturn(Optional.of(escola));
        when(turmaRepository.save(any(Turma.class))).thenAnswer(invocacao -> {
            Turma salva = invocacao.getArgument(0);
            ReflectionTestUtils.setField(salva, "id", 5L);
            return salva;
        });

        Turma turma = service.cadastrar(admin, "  7º ano A ", AnoEscolar.SETIMO_ANO);

        assertEquals("7º ano A", turma.getNome());
        assertEquals(AnoEscolar.SETIMO_ANO, turma.getAnoEscolar());
        assertTrue(turma.isAtivo());
        assertSame(escola, turma.getInstituicao());
        verify(auditoriaService).registrar(admin, AcaoAuditoria.CADASTRO_TURMA, "Turma", 5L, true,
                "7º ano A (SETIMO_ANO)");
    }

    @Test
    void rf06Ca03_quemNaoEAdminNaoCadastraTurma() {
        assertThrows(AccessDeniedException.class,
                () -> service.cadastrar(professor, "7º ano A", AnoEscolar.SETIMO_ANO));

        verifyNoInteractions(turmaRepository, instituicaoRepository, auditoriaService);
    }

    @Test
    void rf06Ca02_anoEscolarEObrigatorio() {
        assertThrows(NegocioException.class, () -> service.cadastrar(admin, "7º ano A", null));

        verify(turmaRepository, never()).save(any());
    }

    @Test
    void naoPermiteDuasTurmasComOMesmoNomeNaInstituicao() {
        when(turmaRepository.existsByInstituicaoIdAndNomeIgnoreCase(INSTITUICAO_ID, "7º ano A")).thenReturn(true);

        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.cadastrar(admin, "7º ano A", AnoEscolar.SETIMO_ANO));

        assertEquals("Já existe uma turma com este nome na sua instituição.", ex.getMessage());
        verify(turmaRepository, never()).save(any());
    }

    @Test
    void adminDesativaTurmaDaPropriaInstituicao() {
        Turma turma = new Turma("7º ano A", escola, AnoEscolar.SETIMO_ANO);
        ReflectionTestUtils.setField(turma, "id", 5L);
        when(turmaRepository.findById(5L)).thenReturn(Optional.of(turma));

        service.alterarSituacao(admin, 5L, false);

        assertFalse(turma.isAtivo());
        verify(auditoriaService).registrar(admin, AcaoAuditoria.ALTERACAO_TURMA, "Turma", 5L, true, "desativada");
    }

    @Test
    void adminNaoAlteraTurmaDeOutraInstituicao() {
        Instituicao outra = new Instituicao("Outra", "11.111.111/0001-11", "Rua 2");
        ReflectionTestUtils.setField(outra, "id", 2L);
        Turma turma = new Turma("8º ano B", outra, AnoEscolar.OITAVO_ANO);
        when(turmaRepository.findById(9L)).thenReturn(Optional.of(turma));

        assertThrows(AccessDeniedException.class, () -> service.alterarSituacao(admin, 9L, false));

        assertTrue(turma.isAtivo());
        verifyNoInteractions(auditoriaService);
    }
}
