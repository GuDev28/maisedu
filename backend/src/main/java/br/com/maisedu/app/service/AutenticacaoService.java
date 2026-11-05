package br.com.maisedu.app.service;

import br.com.maisedu.app.exception.AutenticacaoException;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.AcaoAuditoria;
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
    private final AuditoriaService auditoriaService;

    @Value("${maisedu.login.max-tentativas}")
    private int maxTentativas;

    @Value("${maisedu.login.bloqueio-minutos}")
    private int bloqueioMinutos;

    @Value("${maisedu.termos.versao-atual:1}")
    private int versaoTermosAtual = 1;

    /**
     * Hash de uma senha descartável, gerado uma única vez. Quando o identificador não existe, a
     * senha digitada é comparada com ele mesmo assim, para que a resposta demore o mesmo tempo de
     * uma senha errada e não revele, pelo tempo de resposta, se a conta existe.
     */
    private volatile String hashFicticio;

    public record Sessao(String token, Usuario usuario) {
    }

    // noRollbackFor: a senha errada grava tentativasFalhas/bloqueadoAte e depois lança
    // AutenticacaoException; sem isso o rollback desfazia a contagem e o bloqueio nunca acontecia.
    @Transactional(noRollbackFor = AutenticacaoException.class)
    public Sessao login(String identificadorDigitado, String senha) {
        String identificador = Identificadores.normalizar(identificadorDigitado);
        Usuario usuario = usuarioRepository.findByIdentificadorDeAcesso(identificador).orElse(null);
        if (usuario == null) {
            compararComHashFicticio(senha);
            auditoriaService.registrar(null, null, identificador, AcaoAuditoria.LOGIN,
                    null, null, false, "Identificador não encontrado");
            throw new AutenticacaoException(MENSAGEM_INVALIDA);
        }

        // RF01/CA03: com a conta bloqueada, o acesso é negado mesmo com a senha correta, informando o tempo restante.
        if (usuario.estaBloqueado()) {
            long minutosRestantes = Duration.between(LocalDateTime.now(), usuario.getBloqueadoAte()).toMinutes() + 1;
            auditoriaService.registrar(usuario.getId(), usuario.getNome(), identificador, AcaoAuditoria.LOGIN,
                    null, null, false, "Bloqueado por excesso de tentativas");
            throw new AutenticacaoException(
                    "Login bloqueado por excesso de tentativas. Tente novamente em " + minutosRestantes + " minuto(s).");
        }

        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            usuario.registrarTentativaFalha(maxTentativas, bloqueioMinutos);
            usuarioRepository.save(usuario);
            auditoriaService.registrar(usuario.getId(), usuario.getNome(), identificador, AcaoAuditoria.LOGIN,
                    null, null, false, "Senha incorreta");
            throw new AutenticacaoException(MENSAGEM_INVALIDA);
        }

        // Só revela que a conta está desativada para quem acertou a senha: quem não sabe a senha
        // recebe a mesma mensagem genérica e não descobre se a conta existe.
        if (!usuario.isAtivo()) {
            auditoriaService.registrar(usuario.getId(), usuario.getNome(), identificador, AcaoAuditoria.LOGIN,
                    null, null, false, "Conta desativada");
            throw new AutenticacaoException("Esta conta está desativada. Procure a administração da sua escola.");
        }

        usuario.registrarLoginComSucesso();
        usuarioRepository.save(usuario);
        auditoriaService.registrar(usuario.getId(), usuario.getNome(), identificador, AcaoAuditoria.LOGIN,
                null, null, true, null);

        return new Sessao(jwtService.gerarToken(usuario), usuario);
    }

    @Transactional
    public void trocarSenha(Long usuarioId, String senhaAtual, String novaSenha) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new AutenticacaoException("Usuário não encontrado."));

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            auditoriaService.registrar(usuario.getId(), usuario.getNome(), AcaoAuditoria.TROCA_SENHA,
                    "Usuario", usuario.getId(), false, "Senha atual incorreta");
            throw new AutenticacaoException("Senha atual incorreta.");
        }
        if (passwordEncoder.matches(novaSenha, usuario.getSenha())) {
            throw new NegocioException("A nova senha deve ser diferente da atual.");
        }

        usuario.trocarSenha(passwordEncoder.encode(novaSenha));
        usuario.aceitarTermos(versaoTermosAtual);
        usuarioRepository.save(usuario);
        auditoriaService.registrar(usuario.getId(), usuario.getNome(), AcaoAuditoria.TROCA_SENHA,
                "Usuario", usuario.getId(), true, null);
        auditoriaService.registrar(usuario.getId(), usuario.getNome(), AcaoAuditoria.ACEITE_TERMOS,
                "Usuario", usuario.getId(), true, "versão " + versaoTermosAtual);
    }

    /** Novo aceite exigido quando a versão dos Termos/Política muda (maisedu.termos.versao-atual). */
    @Transactional
    public void aceitarTermos(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new AutenticacaoException("Usuário não encontrado."));
        usuario.aceitarTermos(versaoTermosAtual);
        usuarioRepository.save(usuario);
        auditoriaService.registrar(usuario.getId(), usuario.getNome(), AcaoAuditoria.ACEITE_TERMOS,
                "Usuario", usuario.getId(), true, "versão " + versaoTermosAtual);
    }

    /** Logout: além de apagar o cookie, invalida o token no servidor (todas as sessões do usuário). */
    @Transactional
    public void encerrarSessoes(Long usuarioId) {
        usuarioRepository.findById(usuarioId).ifPresent(usuario -> {
            usuario.encerrarSessoes();
            usuarioRepository.save(usuario);
        });
    }

    public int versaoTermosAtual() {
        return versaoTermosAtual;
    }

    private void compararComHashFicticio(String senha) {
        if (hashFicticio == null) {
            hashFicticio = passwordEncoder.encode("senha-descartavel-" + System.nanoTime());
        }
        if (hashFicticio != null && senha != null) {
            passwordEncoder.matches(senha, hashFicticio);
        }
    }
}
