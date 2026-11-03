package br.com.maisedu.app.dto;

import java.time.LocalDateTime;

/**
 * Projeção usada na evolução do desempenho: quando a resposta foi dada e se estava certa.
 * Tipo wrapper porque o Hibernate instancia este record direto da consulta JPQL.
 */
public record RespostaHistorico(LocalDateTime respondidaEm, Boolean correta) {

    public boolean acertou() {
        return Boolean.TRUE.equals(correta);
    }
}
