package br.com.maisedu.app.service;

import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.InstituicaoRepository;
import br.com.maisedu.app.repository.RoleRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.SenhaGeradora;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final InstituicaoRepository instituicaoRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SenhaGeradora senhaGeradora;

    public record NovoUsuario(Usuario usuario, String senhaProvisoria) {
    }

    @Transactional
    public NovoUsuario cadastrarProfessor(Long instituicaoAdminId, String nome, String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new NegocioException("Já existe um usuário com este e-mail.");
        }

        Instituicao instituicao = instituicaoRepository.findById(instituicaoAdminId)
                .orElseThrow(() -> new NegocioException("Instituição não encontrada."));

        String senhaProvisoria = senhaGeradora.gerar();
        Usuario professor = new Usuario(nome, email, passwordEncoder.encode(senhaProvisoria), instituicao);
        professor.adicionarRole(buscarRole(RoleNome.PROFESSOR));

        return new NovoUsuario(usuarioRepository.save(professor), senhaProvisoria);
    }

    @Transactional
    public NovoUsuario cadastrarAluno(Long professorId, String nome, String login) {
        if (usuarioRepository.existsByLogin(login)) {
            throw new NegocioException("Já existe um usuário com este login.");
        }

        Usuario professor = usuarioRepository.findById(professorId)
                .orElseThrow(() -> new NegocioException("Professor não encontrado."));

        String senhaProvisoria = senhaGeradora.gerar();
        Usuario aluno = Usuario.novoAluno(
                nome, login, passwordEncoder.encode(senhaProvisoria), professor.getInstituicao());
        aluno.adicionarRole(buscarRole(RoleNome.ALUNO));

        return new NovoUsuario(usuarioRepository.save(aluno), senhaProvisoria);
    }

    private Role buscarRole(RoleNome nome) {
        return roleRepository.findByNome(nome)
                .orElseThrow(() -> new IllegalStateException("Papel " + nome + " não está cadastrado (ver migrations)."));
    }
}
