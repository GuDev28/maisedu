package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.AlunoTurmaResponse;
import br.com.maisedu.app.dto.ProfessorTurmaResponse;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.VinculoTurmaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/turmas/{turmaId}")
@RequiredArgsConstructor
public class VinculoTurmaController {

    private final VinculoTurmaService vinculoTurmaService;

    @PostMapping("/alunos/{alunoId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public AlunoTurmaResponse vincularAluno(
            @AuthenticationPrincipal UsuarioAutenticado ator,
            @PathVariable Long turmaId,
            @PathVariable Long alunoId) {
        var vinculo = vinculoTurmaService.vincularAluno(alunoId, turmaId, ator);
        return AlunoTurmaResponse.from(vinculo);
    }

    @PostMapping("/professores/{professorId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProfessorTurmaResponse vincularProfessor(
            @AuthenticationPrincipal UsuarioAutenticado ator,
            @PathVariable Long turmaId,
            @PathVariable Long professorId) {
        var vinculo = vinculoTurmaService.vincularProfessor(professorId, turmaId, ator);
        return ProfessorTurmaResponse.from(vinculo);
    }

}
