package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.Usuario;


public record UsuarioCadastradoResponse(
        Long id,
        String nome,
        String identificadorDeAcesso,
        String senhaProvisoria) {

    public static UsuarioCadastradoResponse from(Usuario usuario, String senhaProvisoria) {
        return new UsuarioCadastradoResponse(
                usuario.getId(), usuario.getNome(), usuario.identificadorDeAcesso(), senhaProvisoria);
    }
}
