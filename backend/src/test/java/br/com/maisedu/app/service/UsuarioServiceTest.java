package br.com.maisedu.app.service;

import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.InstituicaoRepository;
import br.com.maisedu.app.repository.RoleRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.SenhaGeradora;
import br.com.maisedu.app.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Cadastro de usuários: o admin cadastra professores (e-mail único, senha provisória enviada pelo Resend)
 * e o professor cadastra alunos (login único, sem e-mail — decisão de minimização de dados da LGPD).
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final long INSTITUICAO_ID = 1L;
    private static final long ADMIN_ID = 300L;
    private static final long PROFESSOR_ID = 200L;

    @Mock UsuarioRepository usuarioRepository;
    @Mock InstituicaoRepository instituicaoRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock SenhaGeradora senhaGeradora;
    @Mock AuditoriaService auditoriaService;
    @Mock ResendEmailService emailService;
    @Mock VinculoTurmaService vinculoTurmaService;
    @InjectMocks UsuarioService service;

    private static final long TURMA_ID = 70L;

    private Instituicao escola;
    private UsuarioAutenticado admin;
    private UsuarioAutenticado atorProfessor;

    @BeforeEach
    void setUp() {
        escola = new Instituicao("Escola Teste", "00.000.000/0001-00", "Rua Teste, 1");
        ReflectionTestUtils.setField(escola, "id", INSTITUICAO_ID);
        admin = new UsuarioAutenticado(ADMIN_ID, "Admin", INSTITUICAO_ID,
                EnumSet.of(RoleNome.ADMIN, RoleNome.PROFESSOR), false);
        atorProfessor = new UsuarioAutenticado(PROFESSOR_ID, "Paulo Professor", INSTITUICAO_ID,
                EnumSet.of(RoleNome.PROFESSOR), false);
    }

    /** O banco gera o id ao salvar; aqui o mock devolve o próprio objeto com o id preenchido. */
    private void dadoQueOBancoGeraOId(long idGerado) {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> {
            Usuario salvo = invocacao.getArgument(0);
            ReflectionTestUtils.setField(salvo, "id", idGerado);
            return salvo;
        });
    }

    @Test
    void deveCadastrarProfessorComSenhaProvisoriaCriptografadaEEnviadaPorEmail() {
        // Arrange
        when(usuarioRepository.existsByEmail("nova@escola.test")).thenReturn(false);
        when(instituicaoRepository.findById(INSTITUICAO_ID)).thenReturn(Optional.of(escola));
        when(senhaGeradora.gerar()).thenReturn("Provisoria9");
        when(passwordEncoder.encode("Provisoria9")).thenReturn("hash-da-provisoria");
        when(roleRepository.findByNome(RoleNome.PROFESSOR)).thenReturn(Optional.of(new Role(RoleNome.PROFESSOR)));
        dadoQueOBancoGeraOId(400L);

        // Act
        UsuarioService.NovoUsuario novo = service.cadastrarProfessor(admin, "Nova Professora", "nova@escola.test", null);

        // Assert
        Usuario professor = novo.usuario();
        assertEquals("Provisoria9", novo.senhaProvisoria());
        assertEquals("hash-da-provisoria", professor.getSenha());
        assertTrue(professor.isSenhaTemporaria());
        assertTrue(professor.possuiRole(RoleNome.PROFESSOR));
        assertSame(escola, professor.getInstituicao());
        verify(auditoriaService).registrar(admin, AcaoAuditoria.CADASTRO_PROFESSOR, "Usuario", 400L, true, null);
        verify(emailService).enviarSenhaProvisoria(400L, "Nova Professora", "nova@escola.test", "Provisoria9");
        verifyNoInteractions(vinculoTurmaService);
    }

    @Test
    void deveCadastrarProfessorEJaVincularATurmaInformada() {
        // Arrange
        when(usuarioRepository.existsByEmail("nova@escola.test")).thenReturn(false);
        when(instituicaoRepository.findById(INSTITUICAO_ID)).thenReturn(Optional.of(escola));
        when(senhaGeradora.gerar()).thenReturn("Provisoria9");
        when(passwordEncoder.encode("Provisoria9")).thenReturn("hash-da-provisoria");
        when(roleRepository.findByNome(RoleNome.PROFESSOR)).thenReturn(Optional.of(new Role(RoleNome.PROFESSOR)));
        dadoQueOBancoGeraOId(400L);

        // Act
        service.cadastrarProfessor(admin, "Nova Professora", "nova@escola.test", TURMA_ID);

        // Assert
        verify(vinculoTurmaService).vincularProfessor(400L, TURMA_ID, admin);
        verify(emailService).enviarSenhaProvisoria(400L, "Nova Professora", "nova@escola.test", "Provisoria9");
    }

    @Test
    void deveLancarExcecaoQuandoEmailDoProfessorJaEstaCadastrado() {
        // Arrange
        when(usuarioRepository.existsByEmail("paulo@escola.test")).thenReturn(true);

        // Act
        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.cadastrarProfessor(admin, "Paulo Repetido", "paulo@escola.test", null));

        // Assert
        assertEquals("Já existe um usuário com este e-mail.", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
        verify(emailService, never()).enviarSenhaProvisoria(any(), any(), any(), any());
    }

    @Test
    void deveCadastrarAlunoSemEmailProprioEMandarOAcessoParaOResponsavel() {
        // Arrange
        when(usuarioRepository.existsByLogin("2026001")).thenReturn(false);
        when(instituicaoRepository.findById(INSTITUICAO_ID)).thenReturn(Optional.of(escola));
        when(senhaGeradora.gerar()).thenReturn("Aluno12345");
        when(passwordEncoder.encode("Aluno12345")).thenReturn("hash-do-aluno");
        when(roleRepository.findByNome(RoleNome.ALUNO)).thenReturn(Optional.of(new Role(RoleNome.ALUNO)));
        dadoQueOBancoGeraOId(500L);

        // Act
        UsuarioService.NovoUsuario novo = service.cadastrarAluno(atorProfessor, "Ana Aluna", "2026001",
                "  Mae.Da.Ana@Familia.test ", null);

        // Assert
        Usuario aluno = novo.usuario();
        assertEquals("2026001", aluno.getLogin());
        assertNull(aluno.getEmail());
        assertEquals("mae.da.ana@familia.test", aluno.getEmailResponsavel());
        assertEquals("hash-do-aluno", aluno.getSenha());
        assertTrue(aluno.possuiRole(RoleNome.ALUNO));
        assertSame(escola, aluno.getInstituicao());
        verify(auditoriaService).registrar(atorProfessor, AcaoAuditoria.CADASTRO_ALUNO, "Usuario", 500L, true, null);
        verify(emailService).enviarAcessoAoResponsavel(500L, "Ana Aluna", "2026001", escola.getNome(),
                "mae.da.ana@familia.test", "Aluno12345");
        verify(emailService, never()).enviarSenhaProvisoria(any(), any(), any(), any());
        verifyNoInteractions(vinculoTurmaService);
    }

    @Test
    void deveCadastrarAlunoEJaVincularNaTurmaInformada() {
        // Arrange
        when(usuarioRepository.existsByLogin("2026002")).thenReturn(false);
        when(instituicaoRepository.findById(INSTITUICAO_ID)).thenReturn(Optional.of(escola));
        when(senhaGeradora.gerar()).thenReturn("Aluno12345");
        when(passwordEncoder.encode("Aluno12345")).thenReturn("hash-do-aluno");
        when(roleRepository.findByNome(RoleNome.ALUNO)).thenReturn(Optional.of(new Role(RoleNome.ALUNO)));
        dadoQueOBancoGeraOId(501L);

        // Act
        service.cadastrarAluno(atorProfessor, "Beto Aluno", "2026002", "pai@familia.test", TURMA_ID);

        // Assert
        verify(vinculoTurmaService).vincularAluno(501L, TURMA_ID, atorProfessor);
    }

    @Test
    void deveLancarExcecaoQuandoLoginDoAlunoJaExiste() {
        // Arrange
        when(usuarioRepository.existsByLogin("2026001")).thenReturn(true);

        // Act
        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.cadastrarAluno(atorProfessor, "Outra Ana", "2026001", "mae@familia.test", null));

        // Assert
        assertEquals("Já existe um usuário com este login.", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(senhaGeradora, passwordEncoder, emailService);
    }

    @Test
    void deveFalharSemSalvarQuandoPapelProfessorNaoEstaCadastradoNoBanco() {
        // Arrange
        when(usuarioRepository.existsByEmail("nova@escola.test")).thenReturn(false);
        when(instituicaoRepository.findById(INSTITUICAO_ID)).thenReturn(Optional.of(escola));
        when(senhaGeradora.gerar()).thenReturn("Provisoria9");
        when(passwordEncoder.encode("Provisoria9")).thenReturn("hash-da-provisoria");
        when(roleRepository.findByNome(RoleNome.PROFESSOR)).thenReturn(Optional.empty());

        // Act
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.cadastrarProfessor(admin, "Nova Professora", "nova@escola.test", null));

        // Assert
        assertEquals("Papel PROFESSOR não está cadastrado (ver migrations).", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
        verify(emailService, never()).enviarSenhaProvisoria(any(), any(), any(), any());
    }
}
