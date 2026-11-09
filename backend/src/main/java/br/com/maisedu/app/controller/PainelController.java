package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.PainelInstituicaoResponse;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.PainelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Página Início do administrador. As do professor e do aluno usam os endpoints que já existem. */
@RestController
@RequiredArgsConstructor
public class PainelController {

    private final PainelService painelService;

    @GetMapping("/painel/instituicao")
    @PreAuthorize("hasRole('ADMIN')")
    public PainelInstituicaoResponse instituicao(@AuthenticationPrincipal UsuarioAutenticado admin) {
        return painelService.resumoDaInstituicao(admin);
    }
}
