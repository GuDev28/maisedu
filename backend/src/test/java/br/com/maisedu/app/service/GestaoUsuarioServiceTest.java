package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.SenhaRedefinidaResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlunoTurmaRepository;
import br.com.maisedu.app.repository.DesempenhoTopicoRepository;
import br.com.maisedu.app.repository.LogAuditoriaRepository;
import br.com.maisedu.app.repository.ProfessorTurmaRepository;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.SenhaGeradora;
import br.com.maisedu.app.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Redefinição de senha e controles da LGPD (desativação e anonimização). */
@ExtendWith(MockitoExtension.class)
class GestaoUsuarioServiceTest {

    private static final Long INSTITUICAO_ID = 1L;
    private static final Long ADMIN_ID = 1L;
    private static final Long PROFESSOR_ID = 7L;
    private static final Long ALUNO_ID = 20L;

    @Mock UsuarioRepository usuarioRepository;
    @Mock AlunoTurmaRepository alunoTurmaRepository;
    @Mock ProfessorTurmaRepository professorTurmaRepository;
    @Mock QuestaoRepository questaoRepository;
    @Mock RespostaRepository respostaRepository;
    @Mock DesempenhoTopicoRepository desempenhoTopicoRepository;
    @Mock LogAuditoriaRepository logAuditoriaRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock SenhaGeradora senhaGeradora;
    @Mock ResendEmailService emailService;
    @Mock AuditoriaService auditoriaService;
    @InjectMocks GestaoUsuarioService service;

    private Instituicao escola;
    private Usuario aluno;
    private Usuario professor;
    private UsuarioAutenticado atorAdmin;
    private UsuarioAutenticado atorProfessor;

    @BeforeEach
    void setUp() {
        escola = new Instituicao("Escola", "00.000.000/0001-00", "Rua 1");
        ReflectionTestUtils.setField(escola, "id", INSTITUICAO_ID);

        aluno = Usuario.novoAluno("Ana Aluna", "2026001", "hash-antigo", escola);
        ReflectionTestUtils.setField(aluno, "id", ALUNO_ID);
        aluno.adicionarRole(new Role(RoleNome.ALUNO));
        aluno.trocarSenha("hash-antigo");

        professor = new Usuario("Paulo Professor", "paulo@escola.test", "hash-antigo", escola);
        ReflectionTestUtils.setField(professor, "id", PROFESSOR_ID);
        professor.adicionarRole(new Role(RoleNome.PROFESSOR));

        atorAdmin = new UsuarioAutenticado(ADMIN_ID, "Admin", INSTITUICAO_ID,
                EnumSet.of(RoleNome.ADMIN, RoleNome.PROFESSOR), false);
        atorProfessor = new UsuarioAutenticado(PROFESSOR_ID, "Paulo Professor", INSTITUICAO_ID,
                EnumSet.of(RoleNome.PROFESSOR), false);
    }

    @Test
    void professorRedefineASenhaDeAlunoDaSuaTurmaEARecebeParaRepassar() {
        when(usuarioRepository.findById(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(alunoTurmaRepository.alunoEstaEmTurmaDoProfessor(ALUNO_ID, PROFESSOR_ID)).thenReturn(true);
        when(senhaGeradora.gerar()).thenReturn("NovaSenha12");
        when(passwordEncoder.encode("NovaSenha12")).thenReturn("hash-novo");
        int versaoAntes = aluno.getVersaoSessao();

        SenhaRedefinidaResponse resposta = service.redefinirSenha(atorProfessor, ALUNO_ID);

        assertEquals("NovaSenha12", resposta.senhaProvisoria());
        assertFalse(resposta.enviadaPorEmail());
        assertEquals("hash-novo", aluno.getSenha());
        assertTrue(aluno.isSenhaTemporaria());
        // As sessões abertas do aluno deixam de valer
        assertEquals(versaoAntes + 1, aluno.getVersaoSessao());
        verifyNoInteractions(emailService);
        verify(auditoriaService).registrar(
                atorProfessor, AcaoAuditoria.REDEFINICAO_SENHA, "Usuario", ALUNO_ID, true, null);
    }

    @Test
    void professorNaoRedefineSenhaDeAlunoDeOutraTurma() {
        when(usuarioRepository.findById(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(alunoTurmaRepository.alunoEstaEmTurmaDoProfessor(ALUNO_ID, PROFESSOR_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.redefinirSenha(atorProfessor, ALUNO_ID));

        verifyNoInteractions(senhaGeradora, passwordEncoder, emailService);
    }

    @Test
    void adminRedefineSenhaDeProfessorQueRecebeSoPorEmail() {
        when(usuarioRepository.findById(PROFESSOR_ID)).thenReturn(Optional.of(professor));
        when(senhaGeradora.gerar()).thenReturn("Prov1234567");
        when(passwordEncoder.encode("Prov1234567")).thenReturn("hash-novo");

        SenhaRedefinidaResponse resposta = service.redefinirSenha(atorAdmin, PROFESSOR_ID);

        assertNull(resposta.senhaProvisoria());
        assertTrue(resposta.enviadaPorEmail());
        verify(emailService).enviarNovaSenhaProvisoria(PROFESSOR_ID, "Paulo Professor", "paulo@escola.test",
                "Prov1234567");
    }

    @Test
    void usuarioDeOutraInstituicaoNaoEEncontrado() {
        Instituicao outra = new Instituicao("Outra", "11.111.111/0001-11", "Rua 2");
        ReflectionTestUtils.setField(outra, "id", 2L);
        Usuario deOutraEscola = Usuario.novoAluno("Beto", "b001", "hash", outra);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(deOutraEscola));

        assertThrows(RecursoNaoEncontradoException.class, () -> service.redefinirSenha(atorAdmin, 99L));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.exportarDados(atorAdmin, 99L));
    }

    @Test
    void desativarDerrubaAsSessoesENaoValeParaOProprioAdmin() {
        when(usuarioRepository.findById(PROFESSOR_ID)).thenReturn(Optional.of(professor));

        service.desativar(atorAdmin, PROFESSOR_ID);

        assertFalse(professor.isAtivo());
        assertEquals(1, professor.getVersaoSessao());

        Usuario proprioAdmin = new Usuario("Admin", "admin@escola.test", "hash", escola);
        ReflectionTestUtils.setField(proprioAdmin, "id", ADMIN_ID);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(proprioAdmin));
        assertThrows(NegocioException.class, () -> service.desativar(atorAdmin, ADMIN_ID));
    }

    @Test
    void professorNaoUsaControlesExclusivosDoAdmin() {
        assertThrows(AccessDeniedException.class, () -> service.desativar(atorProfessor, ALUNO_ID));
        assertThrows(AccessDeniedException.class, () -> service.anonimizar(atorProfessor, ALUNO_ID));
        assertThrows(AccessDeniedException.class, () -> service.exportarDados(atorProfessor, ALUNO_ID));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void anonimizarRemoveDadosPessoaisVinculosENomeNaAuditoria() {
        when(usuarioRepository.findById(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-aleatorio");

        service.anonimizar(atorAdmin, ALUNO_ID);

        assertEquals("Usuário anonimizado #20", aluno.getNome());
        assertEquals("anonimizado-20", aluno.getLogin());
        assertNull(aluno.getEmail());
        assertFalse(aluno.isAtivo());
        assertTrue(aluno.isAnonimizado());
        assertEquals("hash-aleatorio", aluno.getSenha());
        verify(alunoTurmaRepository).excluirVinculosDoAluno(ALUNO_ID);
        verify(professorTurmaRepository).excluirVinculosDoProfessor(ALUNO_ID);
        verify(logAuditoriaRepository).anonimizarRegistrosDoUsuario(ALUNO_ID, "Usuário anonimizado #20");
        verify(logAuditoriaRepository).substituirDetalhes(ALUNO_ID, AcaoAuditoria.ENVIO_EMAIL, "Dados anonimizados");
        verify(auditoriaService).registrar(
                atorAdmin, AcaoAuditoria.ANONIMIZACAO_USUARIO, "Usuario", ALUNO_ID, true, null);
    }

    @Test
    void contaDeAdministradorNaoEAnonimizada() {
        Usuario outroAdmin = new Usuario("Outro Admin", "outro@escola.test", "hash", escola);
        ReflectionTestUtils.setField(outroAdmin, "id", 2L);
        outroAdmin.adicionarRole(new Role(RoleNome.ADMIN));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(outroAdmin));

        assertThrows(NegocioException.class, () -> service.anonimizar(atorAdmin, 2L));
        verifyNoInteractions(logAuditoriaRepository);
    }
}
