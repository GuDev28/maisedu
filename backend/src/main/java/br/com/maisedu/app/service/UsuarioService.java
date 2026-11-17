package br.com.maisedu.app.service;

import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.InstituicaoRepository;
import br.com.maisedu.app.repository.RoleRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.SenhaGeradora;
import br.com.maisedu.app.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.maisedu.app.security.GeradorLogin;


@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final InstituicaoRepository instituicaoRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SenhaGeradora senhaGeradora;
    private final GeradorLogin geradorLogin;
    private final AuditoriaService auditoriaService;
    private final ResendEmailService emailService;
    private final VinculoTurmaService vinculoTurmaService;

    public record NovoUsuario(Usuario usuario, String senhaProvisoria) {
    }

    /**
     * Cadastra o professor e, se a turma for informada, já o vincula a ela na mesma transação: se o
     * vínculo falhar, o cadastro também é desfeito (e o e-mail, que só sai depois do commit, não é enviado).
     */
    @Transactional
    public NovoUsuario cadastrarProfessor(UsuarioAutenticado admin, String nomeInformado, String emailInformado,
                                          Long turmaId) {
        String nome = nomeInformado.strip();
        String email = Identificadores.normalizar(emailInformado);
        if (usuarioRepository.existsByEmail(email)) {
            throw new NegocioException("Já existe um usuário com este e-mail.");
        }

        Instituicao instituicao = instituicaoRepository.findById(admin.instituicaoId())
                .orElseThrow(() -> new NegocioException("Instituição não encontrada."));

        String senhaProvisoria = senhaGeradora.gerar();
        Usuario professor = new Usuario(nome, email, passwordEncoder.encode(senhaProvisoria), instituicao);
        professor.adicionarRole(buscarRole(RoleNome.PROFESSOR));

        Usuario salvo = usuarioRepository.save(professor);
        auditoriaService.registrar(admin, AcaoAuditoria.CADASTRO_PROFESSOR, "Usuario", salvo.getId(), true, null);
        if (turmaId != null) {
            vinculoTurmaService.vincularProfessor(salvo.getId(), turmaId, admin);
        }
        emailService.enviarSenhaProvisoria(salvo.getId(), salvo.getNome(), email, senhaProvisoria);

        return new NovoUsuario(salvo, senhaProvisoria);
    }

    /**
     * Cadastra o aluno na instituição de quem cadastra. O login é gerado pelo sistema (único em
     * todas as escolas) e o aluno não tem e-mail próprio: o login e a senha provisória vão para o
     * e-mail do responsável (depois do commit). Quem cadastra vê o login, mas não a senha. Com a
     * turma informada, já faz o vínculo no mesmo passo (RN01).
     */
    @Transactional
    public NovoUsuario cadastrarAluno(UsuarioAutenticado professor, String nomeInformado,
                                      String emailResponsavelInformado, Long turmaId) {
        String nome = nomeInformado.strip();
        String emailResponsavel = Identificadores.normalizar(emailResponsavelInformado);
        String login = geradorLogin.gerar(usuarioRepository::existsByLogin);

        Instituicao instituicao = instituicaoRepository.findById(professor.instituicaoId())
                .orElseThrow(() -> new NegocioException("Instituição não encontrada."));

        String senhaProvisoria = senhaGeradora.gerar();
        Usuario aluno = Usuario.novoAluno(nome, login, passwordEncoder.encode(senhaProvisoria), instituicao,
                emailResponsavel);
        aluno.adicionarRole(buscarRole(RoleNome.ALUNO));

        Usuario salvo = usuarioRepository.save(aluno);
        auditoriaService.registrar(professor, AcaoAuditoria.CADASTRO_ALUNO, "Usuario", salvo.getId(), true, null);
        if (turmaId != null) {
            vinculoTurmaService.vincularAluno(salvo.getId(), turmaId, professor);
        }

        emailService.enviarAcessoAoResponsavel(salvo.getId(), nome, login, instituicao.getNome(), emailResponsavel,
                senhaProvisoria);
        return new NovoUsuario(salvo, senhaProvisoria);
    }

    private Role buscarRole(RoleNome nome) {
        return roleRepository.findByNome(nome)
                .orElseThrow(() -> new IllegalStateException("Papel " + nome + " não está cadastrado (ver migrations)."));
    }
}
