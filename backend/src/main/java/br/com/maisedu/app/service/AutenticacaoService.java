package br.com.maisedu.app.service;

import br.com.maisedu.app.exception.AutenticacaoException;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private static final String MENSAGEM_INVALIDA = "Identificador ou senha inválidos.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${maisedu.login.max-tentativas}")
    private int maxTentativas;

    @Value("${maisedu.login.bloqueio-minutos}")
    private int bloqueioMinutos;

    public record Sessao(String token, Usuario usuario) {
    }

    @Transactional
    public Sessao login(String identificador, String senha) {
        Usuario usuario = usuarioRepository.findByIdentificadorDeAcesso(identificador)
                .orElseThrow(() -> new AutenticacaoException(MENSAGEM_INVALIDA));

        if (!usuario.isAtivo()) {
            // Mensagem específica aqui: uma conta desativada não é o mesmo caso de
            // "não existe" ou "senha errada", e quem foi desativado sabe que foi.
            throw new AutenticacaoException("Esta conta está desativada.");
        }

        if (usuario.estaBloqueado()) {
            long minutosRestantes = Duration.between(LocalDateTime.now(), usuario.getBloqueadoAte()).toMinutes() + 1;
            throw new AutenticacaoException(
                    "Login bloqueado por excesso de tentativas. Tente novamente em " + minutosRestantes + " minuto(s).");
        }

        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            usuario.registrarTentativaFalha(maxTentativas, bloqueioMinutos);
            usuarioRepository.save(usuario);
            throw new AutenticacaoException(MENSAGEM_INVALIDA);
        }

        usuario.registrarLoginComSucesso();
        usuarioRepository.save(usuario);

        return new Sessao(jwtService.gerarToken(usuario), usuario);
    }

    @Transactional
    public void trocarSenha(Long usuarioId, String senhaAtual, String novaSenha) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new AutenticacaoException("Usuário não encontrado."));

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new AutenticacaoException("Senha atual incorreta.");
        }
        if (passwordEncoder.matches(novaSenha, usuario.getSenha())) {
            throw new NegocioException("A nova senha deve ser diferente da atual.");
        }

        usuario.trocarSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }
}
