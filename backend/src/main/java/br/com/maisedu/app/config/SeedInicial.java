package br.com.maisedu.app.config;

import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.InstituicaoRepository;
import br.com.maisedu.app.repository.RoleRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.SenhaGeradora;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
public class SeedInicial implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedInicial.class);

    private final InstituicaoRepository instituicaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SenhaGeradora senhaGeradora;

    @Override
    @Transactional
    public void run(String... args) {
        if (instituicaoRepository.count() > 0) {
            return;
        }

        Instituicao instituicao = instituicaoRepository.save(
                new Instituicao("Escola Modelo (dados fictícios)", "00.000.000/0001-00", "Endereço fictício, 123"));

        String senhaProvisoria = senhaGeradora.gerar();
        Usuario admin = new Usuario("Administrador", "admin@maisedu.dev",
                passwordEncoder.encode(senhaProvisoria), instituicao);
        admin.adicionarRole(roleRepository.findByNome(RoleNome.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Papel ADMIN não cadastrado (ver migration V3).")));
        usuarioRepository.save(admin);

        log.warn("""

                 Admin inicial criado (dados ficticios de desenvolvimento):
                   e-mail: admin@maisedu.dev
                   senha provisoria: {}
                 Sera exigida a troca de senha no primeiro login.
                """, senhaProvisoria);
    }
}