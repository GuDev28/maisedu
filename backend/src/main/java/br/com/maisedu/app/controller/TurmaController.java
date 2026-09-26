package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.TurmaResponse;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.VinculoTurmaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/turmas")
@RequiredArgsConstructor
public class TurmaController {

    private final VinculoTurmaService vinculoTurmaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public List<TurmaResponse> listarParaVinculo(@AuthenticationPrincipal UsuarioAutenticado ator) {
        List<TurmaResponse> resposta = new ArrayList<>();
        for (Turma turma : vinculoTurmaService.listarTurmasParaVinculo(ator)) {
            resposta.add(TurmaResponse.from(turma));
        }
        return resposta;
    }
}
