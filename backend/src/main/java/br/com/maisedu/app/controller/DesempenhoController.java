package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.DesempenhoAlunoResponse;
import br.com.maisedu.app.dto.DesempenhoTurmaResponse;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.DesempenhoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** RF19, visão do professor: desempenho da turma e de cada aluno dela. */
@RestController
@RequiredArgsConstructor
public class DesempenhoController {

    private final DesempenhoService desempenhoService;

    @GetMapping("/turmas/{turmaId}/desempenho")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public DesempenhoTurmaResponse turma(@AuthenticationPrincipal UsuarioAutenticado ator, @PathVariable Long turmaId) {
        return desempenhoService.consultarTurma(ator, turmaId);
    }

    @GetMapping("/turmas/{turmaId}/alunos/{alunoId}/desempenho")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public DesempenhoAlunoResponse aluno(@AuthenticationPrincipal UsuarioAutenticado ator,
                                         @PathVariable Long turmaId, @PathVariable Long alunoId) {
        return desempenhoService.consultarAlunoDaTurma(ator, turmaId, alunoId);
    }
}
