package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.CadastroAlunoRequest;
import br.com.maisedu.app.dto.CadastroProfessorRequest;
import br.com.maisedu.app.dto.UsuarioCadastradoResponse;
import br.com.maisedu.app.security.UsuarioAutenticado;
import br.com.maisedu.app.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/professores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioCadastradoResponse cadastrarProfessor(
            @AuthenticationPrincipal UsuarioAutenticado admin,
            @Valid @RequestBody CadastroProfessorRequest request) {
        var novo = usuarioService.cadastrarProfessor(admin, request.nome(), request.email());
        return UsuarioCadastradoResponse.semSenha(novo.usuario());
    }

    @PostMapping("/alunos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROFESSOR')")
    public UsuarioCadastradoResponse cadastrarAluno(
            @AuthenticationPrincipal UsuarioAutenticado professor,
            @Valid @RequestBody CadastroAlunoRequest request) {
        var novo = usuarioService.cadastrarAluno(professor.id(), request.nome(), request.login());
        return UsuarioCadastradoResponse.from(novo.usuario(), novo.senhaProvisoria());
    }
}
