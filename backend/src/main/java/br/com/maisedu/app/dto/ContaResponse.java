package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/** Tela "Minha conta": os dados cadastrais do próprio usuário e o aceite dos Termos/Política. */
public record ContaResponse(
        Long id,
        String nome,
        String identificadorDeAcesso,
        String emailResponsavel,
        Set<RoleNome> roles,
        String instituicao,
        LocalDateTime criadoEm,
        LocalDateTime termosAceitosEm,
        Integer versaoTermosAceita,
        int versaoTermosAtual) {

    public static ContaResponse from(Usuario usuario, int versaoTermosAtual) {
        Set<RoleNome> roles = EnumSet.noneOf(RoleNome.class);
        for (Role role : usuario.getRoles()) {
            roles.add(role.getNome());
        }
        return new ContaResponse(usuario.getId(), usuario.getNome(), usuario.identificadorDeAcesso(),
                usuario.getEmailResponsavel(), roles,
                usuario.getInstituicao().getNome(), usuario.getCriadoEm(), usuario.getTermosAceitosEm(),
                usuario.getTermosVersao(), versaoTermosAtual);
    }
}
