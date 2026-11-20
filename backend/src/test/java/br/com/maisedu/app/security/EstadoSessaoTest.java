package br.com.maisedu.app.security;

import br.com.maisedu.app.model.RoleNome;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Estado da conta lido do banco a cada requisição (campos podem chegar nulos da consulta JPQL). */
class EstadoSessaoTest {

    @Test
    void contaAtiva_soQuandoOCampoForVerdadeiro() {
        assertTrue(new EstadoSessao(true, 0, false, 1).contaAtiva());
        assertFalse(new EstadoSessao(false, 0, false, 1).contaAtiva());
        assertFalse(new EstadoSessao(null, 0, false, 1).contaAtiva());
    }

    @Test
    void senhaProvisoria_nulaContaComoFalsa() {
        assertTrue(new EstadoSessao(true, 0, true, 1).senhaProvisoria());
        assertFalse(new EstadoSessao(true, 0, false, 1).senhaProvisoria());
        assertFalse(new EstadoSessao(true, 0, null, 1).senhaProvisoria());
    }

    @Test
    void versao_nulaViraZero() {
        assertEquals(4, new EstadoSessao(true, 4, false, 1).versao());
        assertEquals(0, new EstadoSessao(true, null, false, 1).versao());
    }

    @Test
    void termosPendentes_quandoNuncaAceitouOuAceitouVersaoAntiga() {
        assertTrue(new EstadoSessao(true, 0, false, null).termosPendentes(2));
        assertTrue(new EstadoSessao(true, 0, false, 1).termosPendentes(2));
        assertFalse(new EstadoSessao(true, 0, false, 2).termosPendentes(2));
        assertFalse(new EstadoSessao(true, 0, false, 3).termosPendentes(2));
    }

    @Test
    void usuarioAutenticadoComEstado_atualizaSenhaETermosSemMudarOResto() {
        UsuarioAutenticado doToken = new UsuarioAutenticado(7L, "Paulo", 1L, EnumSet.of(RoleNome.PROFESSOR),
                false, false, 5);

        UsuarioAutenticado comTermosAntigos = doToken.comEstado(new EstadoSessao(true, 5, false, 1), 2);
        assertTrue(comTermosAntigos.termosPendentes());
        assertFalse(comTermosAntigos.senhaTemporaria());
        assertEquals(5, comTermosAntigos.versaoSessao());
        assertTrue(comTermosAntigos.possuiRole(RoleNome.PROFESSOR));
        assertFalse(comTermosAntigos.possuiRole(RoleNome.ADMIN));

        // senha provisória tem prioridade: os termos só são cobrados depois da troca
        UsuarioAutenticado comSenhaProvisoria = doToken.comEstado(new EstadoSessao(true, 5, true, null), 2);
        assertTrue(comSenhaProvisoria.senhaTemporaria());
        assertFalse(comSenhaProvisoria.termosPendentes());
    }
}
