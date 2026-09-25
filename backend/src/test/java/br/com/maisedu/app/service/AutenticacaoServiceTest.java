package br.com.maisedu.app.service;

import br.com.maisedu.app.exception.AutenticacaoException;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    private AutenticacaoService service;
    private Usuario usuario;

    private static final int MAX_TENTATIVAS = 5;
    private static final int BLOQUEIO_MINUTOS = 15;

    @BeforeEach
    void setUp() {
        service = new AutenticacaoService(usuarioRepository, passwordEncoder, jwtService);
        ReflectionTestUtils.setField(service, "maxTentativas", MAX_TENTATIVAS);
        ReflectionTestUtils.setField(service, "bloqueioMinutos", BLOQUEIO_MINUTOS);

        usuario = new Usuario("Prof", "prof@escola.test", "hash", mock(Instituicao.class));
        ReflectionTestUtils.setField(usuario, "id", 1L);
    }

    @Test
    void loginComSucessoEmiteToken() {
        when(usuarioRepository.findByIdentificadorDeAcesso("prof@escola.test")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);
        when(jwtService.gerarToken(usuario)).thenReturn("token-fake");

        var sessao = service.login("prof@escola.test", "senha123");

        assertThat(sessao.token()).isEqualTo("token-fake");
        assertThat(sessao.usuario()).isSameAs(usuario);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void identificadorInexistenteDaMensagemGenerica() {
        when(usuarioRepository.findByIdentificadorDeAcesso("ninguem@escola.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("ninguem@escola.test", "x"))
                .isInstanceOf(AutenticacaoException.class)
                .hasMessage("Identificador ou senha inválidos.");
    }

    @Test
    void senhaErradaDaMesmaMensagemDoIdentificadorInexistente() {
        when(usuarioRepository.findByIdentificadorDeAcesso("prof@escola.test")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login("prof@escola.test", "errada"))
                .isInstanceOf(AutenticacaoException.class)
                .hasMessage("Identificador ou senha inválidos.");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void quintaTentativaErradaBloqueiaOLogin() {
        when(usuarioRepository.findByIdentificadorDeAcesso("prof@escola.test")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(false);

        for (int i = 0; i < MAX_TENTATIVAS; i++) {
            assertThatThrownBy(() -> service.login("prof@escola.test", "errada"))
                    .isInstanceOf(AutenticacaoException.class);
        }

        assertThat(usuario.estaBloqueado()).isTrue();
    }

    @Test
    void naoAceitaLoginDeContaBloqueada() {
        ReflectionTestUtils.setField(usuario, "bloqueadoAte", LocalDateTime.now().plusMinutes(10));
        when(usuarioRepository.findByIdentificadorDeAcesso("prof@escola.test")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.login("prof@escola.test", "qualquer"))
                .isInstanceOf(AutenticacaoException.class)
                .hasMessageContaining("bloqueado");
        verify(jwtService, never()).gerarToken(any());
    }

    @Test
    void naoAceitaLoginDeContaInativa() {
        usuario.setAtivo(false);
        when(usuarioRepository.findByIdentificadorDeAcesso("prof@escola.test")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.login("prof@escola.test", "qualquer"))
                .isInstanceOf(AutenticacaoException.class)
                .hasMessageContaining("desativada");
    }

    @Test
    void trocaDeSenhaExigeSenhaAtualCorreta() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("atualErrada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.trocarSenha(1L, "atualErrada", "novaSenha123"))
                .isInstanceOf(AutenticacaoException.class)
                .hasMessageContaining("Senha atual incorreta");
    }

    @Test
    void novaSenhaNaoPodeSerIgualAAtual() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaAtual", "hash")).thenReturn(true, true);

        assertThatThrownBy(() -> service.trocarSenha(1L, "senhaAtual", "senhaAtual"))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void trocaDeSenhaValidaAtualizaOHash() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaAtual", "hash")).thenReturn(true);
        when(passwordEncoder.matches("novaSenha123", "hash")).thenReturn(false);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hashNovo");

        service.trocarSenha(1L, "senhaAtual", "novaSenha123");

        assertThat(usuario.getSenha()).isEqualTo("hashNovo");
        assertThat(usuario.isSenhaTemporaria()).isFalse();
        verify(usuarioRepository).save(usuario);
    }
}
