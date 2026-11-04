package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

public record UsuarioGestaoResponse(
        Long id,
        String nome,
        String identificadorDeAcesso,
        Set<RoleNome> roles,
        boolean ativo,
        boolean bloqueado,
        boolean senhaTemporaria,
        boolean anonimizado,
        LocalDateTime criadoEm) {

    public static UsuarioGestaoResponse from(Usuario usuario) {
        Set<RoleNome> roles = EnumSet.noneOf(RoleNome.class);
        for (Role role : usuario.getRoles()) {
            roles.add(role.getNome());
        }
        return new UsuarioGestaoResponse(usuario.getId(), usuario.getNome(), usuario.identificadorDeAcesso(), roles,
                usuario.isAtivo(), usuario.estaBloqueado(), usuario.isSenhaTemporaria(), usuario.isAnonimizado(),
                usuario.getCriadoEm());
    }
}
