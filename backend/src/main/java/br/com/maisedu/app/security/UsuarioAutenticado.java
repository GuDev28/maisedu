package br.com.maisedu.app.security;

import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;

import java.util.EnumSet;
import java.util.Set;


/**
 * Usuário da requisição atual. Os papéis e a instituição vêm do token; senha provisória e termos
 * pendentes são atualizados pelo filtro a partir do banco a cada requisição.
 */
public record UsuarioAutenticado(
        Long id,
        String nome,
        Long instituicaoId,
        Set<RoleNome> roles,
        boolean senhaTemporaria,
        boolean termosPendentes,
        int versaoSessao) {

    public UsuarioAutenticado(Long id, String nome, Long instituicaoId, Set<RoleNome> roles, boolean senhaTemporaria) {
        this(id, nome, instituicaoId, roles, senhaTemporaria, false, 0);
    }

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
                usuario.isSenhaTemporaria(),
                false,
                usuario.getVersaoSessao());
    }

    public boolean possuiRole(RoleNome role) {
        return roles.contains(role);
    }

    public UsuarioAutenticado comEstado(EstadoSessao estado, int versaoTermosAtual) {
        return new UsuarioAutenticado(id, nome, instituicaoId, roles, estado.senhaProvisoria(),
                !estado.senhaProvisoria() && estado.termosPendentes(versaoTermosAtual), versaoSessao);
    }
}
