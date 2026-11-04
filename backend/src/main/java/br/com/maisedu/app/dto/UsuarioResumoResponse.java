package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.security.UsuarioAutenticado;

import java.util.HashSet;
import java.util.Set;

/**
 * Dados da sessão para o front-end. {@code termosPendentes} indica que há uma versão nova dos
 * Termos/Política ainda não aceita (o usuário precisa aceitá-la antes de continuar).
 */
public record UsuarioResumoResponse(
        Long id,
        String nome,
        Set<RoleNome> roles,
        boolean senhaTemporaria,
        boolean termosPendentes) {

    public static UsuarioResumoResponse from(Usuario usuario, int versaoTermosAtual) {
        Set<RoleNome> roles = new HashSet<>();
        for (var role : usuario.getRoles()) {
            roles.add(role.getNome());
        }

        return new UsuarioResumoResponse(
                usuario.getId(),
                usuario.getNome(),
                roles,
                usuario.isSenhaTemporaria(),
                !usuario.isSenhaTemporaria() && usuario.termosPendentes(versaoTermosAtual));
    }

    public static UsuarioResumoResponse from(UsuarioAutenticado usuario) {
        return new UsuarioResumoResponse(usuario.id(), usuario.nome(), usuario.roles(), usuario.senhaTemporaria(),
                usuario.termosPendentes());
    }
}
