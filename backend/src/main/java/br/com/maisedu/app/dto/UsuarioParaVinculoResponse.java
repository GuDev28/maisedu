package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.Usuario;

public record UsuarioParaVinculoResponse(Long id, String nome, String identificadorDeAcesso) {

    public static UsuarioParaVinculoResponse from(Usuario usuario) {
        return new UsuarioParaVinculoResponse(usuario.getId(), usuario.getNome(), usuario.identificadorDeAcesso());
    }
}
