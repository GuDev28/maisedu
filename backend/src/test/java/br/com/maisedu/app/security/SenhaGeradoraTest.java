package br.com.maisedu.app.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Senha provisória: 10 caracteres, sem os que se confundem na leitura (I, l, O, 0 e 1). */
class SenhaGeradoraTest {

    private final SenhaGeradora geradora = new SenhaGeradora();

    @Test
    void gerar_temDezCaracteresDoAlfabetoSemAmbiguidade() {
        for (int i = 0; i < 200; i++) {
            String senha = geradora.gerar();
            assertEquals(10, senha.length());
            assertTrue(senha.matches("[A-HJKMNP-Za-hjkmnp-z2-9]{10}"), "caractere inesperado em " + senha);
        }
    }

    @Test
    void gerar_produzSenhasDiferentes() {
        Set<String> senhas = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            senhas.add(geradora.gerar());
        }
        assertEquals(50, senhas.size());
    }
}
