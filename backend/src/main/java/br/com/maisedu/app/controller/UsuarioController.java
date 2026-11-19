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
import br.com.maisedu.app.model.StatusEmailAcesso;
import br.com.maisedu.app.repository.UsuarioRepository;

@RestController
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/professores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioCadastradoResponse cadastrarProfessor(
            @AuthenticationPrincipal UsuarioAutenticado admin,
            @Valid @RequestBody CadastroProfessorRequest request) {
        var novo = usuarioService.cadastrarProfessor(admin, request.nome(), request.email(), request.turmaId());
        return UsuarioCadastradoResponse.semSenha(novo.usuario(), statusDoEnvio(novo.usuario().getId()));
    }

    @PostMapping("/alunos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROFESSOR')")
    public UsuarioCadastradoResponse cadastrarAluno(
            @AuthenticationPrincipal UsuarioAutenticado professor,
            @Valid @RequestBody CadastroAlunoRequest request) {
        var novo = usuarioService.cadastrarAluno(professor, request.nome(), request.emailResponsavel(),
                request.turmaId());
        // a senha provisória foi para o e-mail do responsável: não volta para quem cadastrou (o login volta)
        return UsuarioCadastradoResponse.semSenha(novo.usuario(), statusDoEnvio(novo.usuario().getId()));
    }

    /**
     * O e-mail sai depois do commit do cadastro, ainda dentro da chamada ao service; aqui a situação
     * gravada já é a do envio. Lida direto do banco para a tela avisar na hora se o envio falhou.
     */
    private StatusEmailAcesso statusDoEnvio(Long usuarioId) {
        return usuarioRepository.findStatusEmailAcesso(usuarioId).orElse(null);
    }
}
