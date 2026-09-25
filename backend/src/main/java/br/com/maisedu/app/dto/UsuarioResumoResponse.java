package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.security.UsuarioAutenticado;

import java.util.HashSet;
import java.util.Set;

public record UsuarioResumoResponse(
        Long id,
        String nome,
        Set<RoleNome> roles,
        boolean senhaTemporaria) {

    public static UsuarioResumoResponse from(Usuario usuario) {
        Set<RoleNome> roles = new HashSet<>();
        for (var role : usuario.getRoles()) {
            roles.add(role.getNome());
        }

        return new UsuarioResumoResponse(
                usuario.getId(),
                usuario.getNome(),
                roles,
                usuario.isSenhaTemporaria());
    }

    public static UsuarioResumoResponse from(UsuarioAutenticado usuario) {
        return new UsuarioResumoResponse(usuario.id(), usuario.nome(), usuario.roles(), usuario.senhaTemporaria());
    }
}
