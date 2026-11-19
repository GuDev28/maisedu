package br.com.maisedu.app.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Login do aluno gerado pelo sistema: formato curto, sem dados pessoais, e nunca repetido. */
class GeradorLoginTest {

    private final GeradorLogin gerador = new GeradorLogin();

    @Test
    void loginTemALetraAESeisDigitosSemComecarComZero() {
        for (int i = 0; i < 200; i++) {
            String login = gerador.gerar(candidato -> false);
            assertTrue(login.matches("a[1-9]\\d{5}"), login);
        }
    }

    @Test
    void sorteiaDeNovoQuandoOLoginJaExiste() {
        Set<String> ocupados = new HashSet<>();
        String primeiro = gerador.gerar(candidato -> false);
        ocupados.add(primeiro);
        // os três primeiros sorteios "colidem"; o quarto é aceito
        int[] tentativas = {0};
        String login = gerador.gerar(candidato -> ++tentativas[0] <= 3 || ocupados.contains(candidato));

        assertFalse(ocupados.contains(login));
        assertTrue(tentativas[0] >= 4);
    }

    @Test
    void depoisDeMuitasColisoesPassaAUsarOitoDigitos() {
        int[] tentativas = {0};
        String login = gerador.gerar(candidato -> ++tentativas[0] <= 10);

        assertTrue(login.matches("a[1-9]\\d{7}"), login);
    }

    @Test
    void desisteSeNenhumLoginEstiverLivre() {
        assertThrows(IllegalStateException.class, () -> gerador.gerar(candidato -> true));
    }
}
