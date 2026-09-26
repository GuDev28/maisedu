package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.Topico;

public record TopicoResponse(Long id, String nome) {

    public static TopicoResponse from(Topico topico) {
        return new TopicoResponse(topico.getId(), topico.getNome());
    }
}
