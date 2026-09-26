package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.AlunoTurmaResponse;
import br.com.maisedu.app.dto.ProfessorTurmaResponse;
import br.com.maisedu.app.dto.UsuarioParaVinculoResponse;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.VinculoTurmaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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

    @GetMapping("/alunos-disponiveis")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public List<UsuarioParaVinculoResponse> listarAlunosDisponiveis(
            @AuthenticationPrincipal UsuarioAutenticado ator,
            @PathVariable Long turmaId) {
        List<UsuarioParaVinculoResponse> resposta = new ArrayList<>();
        for (Usuario aluno : vinculoTurmaService.listarAlunosDisponiveis(turmaId, ator)) {
            resposta.add(UsuarioParaVinculoResponse.from(aluno));
        }
        return resposta;
    }

    @GetMapping("/professores-disponiveis")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UsuarioParaVinculoResponse> listarProfessoresDisponiveis(
            @AuthenticationPrincipal UsuarioAutenticado ator,
            @PathVariable Long turmaId) {
        List<UsuarioParaVinculoResponse> resposta = new ArrayList<>();
        for (Usuario professor : vinculoTurmaService.listarProfessoresDisponiveis(turmaId, ator)) {
            resposta.add(UsuarioParaVinculoResponse.from(professor));
        }
        return resposta;
    }

}
