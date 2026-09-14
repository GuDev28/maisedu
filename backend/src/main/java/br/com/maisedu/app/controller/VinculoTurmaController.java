package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.AlunoTurmaResponse;
import br.com.maisedu.app.dto.ProfessorTurmaResponse;
import br.com.maisedu.app.service.VinculoTurmaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/turmas/{turmaId}")
@RequiredArgsConstructor
public class VinculoTurmaController {

    private final VinculoTurmaService vinculoTurmaService;

    @PostMapping("/alunos/{alunoId}")
    @ResponseStatus(HttpStatus.CREATED)
    public AlunoTurmaResponse vincularAluno(
            @PathVariable Long turmaId,
            @PathVariable Long alunoId) {
        var vinculo = vinculoTurmaService.vincularAluno(alunoId, turmaId);
        return AlunoTurmaResponse.from(vinculo);
    }

    @PostMapping("/professores/{professorId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfessorTurmaResponse vincularProfessor(
            @PathVariable Long turmaId,
            @PathVariable Long professorId) {
        var vinculo = vinculoTurmaService.vincularProfessor(professorId, turmaId);
        return ProfessorTurmaResponse.from(vinculo);
    }

}
