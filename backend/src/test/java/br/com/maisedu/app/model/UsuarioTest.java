package br.com.maisedu.app.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UsuarioTest {

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Prof", "prof@escola.test", "hashAntigo", mock(Instituicao.class));
    }

    @Test
    void naoBloqueiaAntesDoLimiteDeTentativas() {
        usuario.registrarTentativaFalha(5, 15);
        usuario.registrarTentativaFalha(5, 15);
        usuario.registrarTentativaFalha(5, 15);
        usuario.registrarTentativaFalha(5, 15);

        assertThat(usuario.estaBloqueado()).isFalse();
    }

    @Test
    void bloqueiaAoAtingirOLimiteDeTentativas() {
        for (int i = 0; i < 5; i++) {
            usuario.registrarTentativaFalha(5, 15);
        }

        assertThat(usuario.estaBloqueado()).isTrue();
    }

    @Test
    void loginComSucessoZeraTentativasEBloqueio() {
        for (int i = 0; i < 5; i++) {
            usuario.registrarTentativaFalha(5, 15);
        }
        assertThat(usuario.estaBloqueado()).isTrue();

        usuario.registrarLoginComSucesso();

        assertThat(usuario.estaBloqueado()).isFalse();
        assertThat((int) ReflectionTestUtils.getField(usuario, "tentativasFalhas")).isZero();
    }

    @Test
    void bloqueioExpiraSozinho() {
        for (int i = 0; i < 5; i++) {
            usuario.registrarTentativaFalha(5, 15);
        }
        // simula o tempo já ter passado
        ReflectionTestUtils.setField(usuario, "bloqueadoAte", LocalDateTime.now().minusMinutes(1));

        assertThat(usuario.estaBloqueado()).isFalse();
    }

    @Test
    void nasceComSenhaTemporaria() {
        assertThat(usuario.isSenhaTemporaria()).isTrue();
    }

    @Test
    void trocarSenhaLimpaSenhaTemporariaETentativas() {
        usuario.registrarTentativaFalha(5, 15);

        usuario.trocarSenha("novoHash");

        assertThat(usuario.getSenha()).isEqualTo("novoHash");
        assertThat(usuario.isSenhaTemporaria()).isFalse();
        assertThat(usuario.estaBloqueado()).isFalse();
        assertThat((int) ReflectionTestUtils.getField(usuario, "tentativasFalhas")).isZero();
    }

    @Test
    void professorUsaEmailComoIdentificador() {
        assertThat(usuario.identificadorDeAcesso()).isEqualTo("prof@escola.test");
    }

    @Test
    void alunoUsaLoginComoIdentificador() {
        Usuario aluno = Usuario.novoAluno("Aluno", "2026001", "hash", mock(Instituicao.class));
        assertThat(aluno.identificadorDeAcesso()).isEqualTo("2026001");
        assertThat(aluno.getEmail()).isNull();
    }
}
