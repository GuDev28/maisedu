package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.StatusEmailAcesso;
import br.com.maisedu.app.model.Usuario;

/**
 * Usuário recém-cadastrado. {@code identificadorDeAcesso} é o e-mail (professor) ou o login gerado
 * (aluno); a senha provisória nunca volta para quem cadastrou, vai por e-mail.
 * {@code statusEmailAcesso} diz se esse e-mail saiu (a tela avisa quando falhou).
 */
public record UsuarioCadastradoResponse(
        Long id,
        String nome,
        String identificadorDeAcesso,
        String senhaProvisoria,
        StatusEmailAcesso statusEmailAcesso) {

    public static UsuarioCadastradoResponse from(Usuario usuario, String senhaProvisoria) {
        return new UsuarioCadastradoResponse(
                usuario.getId(), usuario.getNome(), usuario.identificadorDeAcesso(), senhaProvisoria, null);
    }

    public static UsuarioCadastradoResponse semSenha(Usuario usuario) {
        return semSenha(usuario, null);
    }

    public static UsuarioCadastradoResponse semSenha(Usuario usuario, StatusEmailAcesso statusEmailAcesso) {
        return new UsuarioCadastradoResponse(
                usuario.getId(), usuario.getNome(), usuario.identificadorDeAcesso(), null, statusEmailAcesso);
    }
}
