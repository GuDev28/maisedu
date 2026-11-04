package br.com.maisedu.app.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Versão da sessão (revogação de tokens), aceite versionado dos termos e anonimização. */
class UsuarioSessaoELgpdTest {

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Paula Professora", "paula@escola.test", "hash", null);
        ReflectionTestUtils.setField(usuario, "id", 42L);
    }

    @Test
    void trocaRedefinicaoEDesativacaoEncerramAsSessoes() {
        usuario.trocarSenha("h1");
        assertEquals(1, usuario.getVersaoSessao());

        usuario.redefinirSenhaProvisoria("h2");
        assertEquals(2, usuario.getVersaoSessao());
        assertTrue(usuario.isSenhaTemporaria());

        usuario.desativar();
        assertEquals(3, usuario.getVersaoSessao());
        assertFalse(usuario.isAtivo());
    }

    @Test
    void redefinicaoDesbloqueiaAConta() {
        for (int i = 0; i < 5; i++) {
            usuario.registrarTentativaFalha(5, 15);
        }
        assertTrue(usuario.estaBloqueado());

        usuario.redefinirSenhaProvisoria("novo");

        assertFalse(usuario.estaBloqueado());
    }

    @Test
    void termosPendentesQuandoAVersaoAceitaEAntiga() {
        assertTrue(usuario.termosPendentes(1), "nunca aceitou");

        usuario.aceitarTermos(1);
        assertFalse(usuario.termosPendentes(1));
        assertTrue(usuario.termosPendentes(2), "a versão 2 exige novo aceite");
    }

    @Test
    void anonimizarRemoveIdentificacaoEDesativa() {
        usuario.aceitarTermos(1);

        usuario.anonimizar("hash-aleatorio");

        assertEquals("Usuário anonimizado #42", usuario.getNome());
        assertNull(usuario.getEmail());
        assertEquals("anonimizado-42", usuario.getLogin());
        assertEquals("anonimizado-42", usuario.identificadorDeAcesso());
        assertFalse(usuario.isAtivo());
        assertNull(usuario.getTermosAceitosEm());
        assertTrue(usuario.isAnonimizado());
        assertEquals(1, usuario.getVersaoSessao());
    }
}
