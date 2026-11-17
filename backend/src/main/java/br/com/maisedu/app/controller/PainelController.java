package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.PainelInstituicaoResponse;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.PainelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import br.com.maisedu.app.dto.PainelEscolaResponse;

/** Página Início e painel da escola do administrador. Os do professor e do aluno usam os endpoints que já existem. */
@RestController
@RequiredArgsConstructor
public class PainelController {

    private final PainelService painelService;

    @GetMapping("/painel/instituicao")
    @PreAuthorize("hasRole('ADMIN')")
    public PainelInstituicaoResponse instituicao(@AuthenticationPrincipal UsuarioAutenticado admin) {
        return painelService.resumoDaInstituicao(admin);
    }

    /** Painel pedagógico da escola (só dados agregados, sem identificar alunos). */
    @GetMapping("/painel/escola")
    @PreAuthorize("hasRole('ADMIN')")
    public PainelEscolaResponse escola(@AuthenticationPrincipal UsuarioAutenticado admin) {
        return painelService.desempenhoDaEscola(admin);
    }
}
