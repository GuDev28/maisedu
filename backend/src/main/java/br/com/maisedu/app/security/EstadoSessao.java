package br.com.maisedu.app.security;

/**
 * Estado atual do usuário no banco, conferido a cada requisição autenticada. Tipos wrapper porque o
 * Hibernate instancia este record direto da consulta JPQL ("select new ...").
 */
public record EstadoSessao(Boolean ativo, Integer versaoSessao, Boolean senhaTemporaria, Integer termosVersao) {

    public boolean contaAtiva() {
        return Boolean.TRUE.equals(ativo);
    }

    public boolean senhaProvisoria() {
        return Boolean.TRUE.equals(senhaTemporaria);
    }

    public int versao() {
        return versaoSessao == null ? 0 : versaoSessao;
    }

    public boolean termosPendentes(int versaoAtual) {
        return termosVersao == null || termosVersao < versaoAtual;
    }
}
