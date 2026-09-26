package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.TopicoResponse;
import br.com.maisedu.app.model.Topico;
import br.com.maisedu.app.repository.TopicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/topicos")
@RequiredArgsConstructor
public class TopicoController {

    private final TopicoRepository topicoRepository;

    @GetMapping
    @PreAuthorize("hasRole('PROFESSOR')")
    public List<TopicoResponse> listar() {
        List<TopicoResponse> respostas = new ArrayList<>();
        for (Topico topico : topicoRepository.findAll()) {
            respostas.add(TopicoResponse.from(topico));
        }
        return respostas;
    }
}
