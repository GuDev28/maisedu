package br.com.maisedu.app.security;

import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;

import java.util.EnumSet;
import java.util.Set;


public record UsuarioAutenticado(
        Long id,
        String nome,
        Long instituicaoId,
        Set<RoleNome> roles,
        boolean senhaTemporaria) {

    public static UsuarioAutenticado de(Usuario usuario) {
        EnumSet<RoleNome> roles = EnumSet.noneOf(RoleNome.class);
        for (RoleNome role : RoleNome.values()) {
            if (usuario.possuiRole(role)) {
                roles.add(role);
            }
        }
        return new UsuarioAutenticado(
                usuario.getId(),
                usuario.getNome(),
                usuario.getInstituicao().getId(),
                roles,
                usuario.isSenhaTemporaria());
    }

    public boolean possuiRole(RoleNome role) {
        return roles.contains(role);
    }
}
