package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;

import java.util.HashSet;
import java.util.Set;

/**
 * Dados da sessão para o front-end (o cabeçalho mostra o nome, a escola e o papel). {@code termosPendentes}
 * indica que há uma versão nova dos Termos/Política ainda não aceita (o usuário precisa aceitá-la antes
 * de continuar).
 */
public record UsuarioResumoResponse(
        Long id,
        String nome,
        String instituicao,
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
                usuario.getInstituicao().getNome(),
                roles,
                usuario.isSenhaTemporaria(),
                !usuario.isSenhaTemporaria() && usuario.termosPendentes(versaoTermosAtual));
    }
}
