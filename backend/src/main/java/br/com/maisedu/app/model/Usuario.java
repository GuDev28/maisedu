package br.com.maisedu.app.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    public static final String PREFIXO_LOGIN_ANONIMIZADO = "anonimizado-";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true)
    private String email;

    @Column(unique = true, length = 50)
    private String login;

    @Column(nullable = false)
    @Setter
    private String senha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instituicao_id", nullable = false)
    private Instituicao instituicao;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_role",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    @Setter
    private boolean ativo = true;

    @Column(name = "tentativas_falhas", nullable = false)
    private int tentativasFalhas = 0;

    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;

    @Column(name = "senha_temporaria", nullable = false)
    private boolean senhaTemporaria = true;

    @Column(nullable = false, updatable = false)
    private final LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "termos_aceitos_em")
    private LocalDateTime termosAceitosEm;

    /** Versão dos Termos de Uso / Política de Privacidade aceita pelo usuário (null = nunca aceitou). */
    @Column(name = "termos_versao")
    private Integer termosVersao;

    /**
     * Incrementada sempre que as sessões abertas devem deixar de valer (logout, troca ou redefinição
     * de senha, desativação). O JWT carrega a versão em que foi emitido e o filtro compara com esta.
     */
    @Column(name = "versao_sessao", nullable = false)
    private int versaoSessao = 0;

    @Column(name = "anonimizado_em")
    private LocalDateTime anonimizadoEm;

    public Usuario(String nome, String email, String senha, Instituicao instituicao) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.instituicao = instituicao;
    }

    public static Usuario novoAluno(String nome, String login, String senha, Instituicao instituicao) {
        Usuario aluno = new Usuario(nome, null, senha, instituicao);
        aluno.login = login;
        return aluno;
    }

    public void adicionarRole(Role role) {
        this.roles.add(role);
    }

    public boolean possuiRole(RoleNome roleNome) {
        for (Role r : roles) {
            if (r.getNome() == roleNome) {
                return true;
            }
        }
        return false;
    }

    public String identificadorDeAcesso() {
        return email != null ? email : login;
    }

    public boolean estaBloqueado() {
        return bloqueadoAte != null && bloqueadoAte.isAfter(LocalDateTime.now());
    }

    public void registrarTentativaFalha(int maximoTentativas, int minutosDeBloqueio) {
        tentativasFalhas++;
        if (tentativasFalhas >= maximoTentativas) {
            bloqueadoAte = LocalDateTime.now().plusMinutes(minutosDeBloqueio);
        }
    }

    public void registrarLoginComSucesso() {
        tentativasFalhas = 0;
        bloqueadoAte = null;
    }

    /** Troca feita pelo próprio usuário: a senha deixa de ser provisória e as outras sessões caem. */
    public void trocarSenha(String novoHash) {
        this.senha = novoHash;
        this.senhaTemporaria = false;
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
        encerrarSessoes();
    }

    /** Redefinição feita por admin/professor: nova senha provisória, a ser trocada no próximo acesso. */
    public void redefinirSenhaProvisoria(String novoHash) {
        this.senha = novoHash;
        this.senhaTemporaria = true;
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
        encerrarSessoes();
    }

    public void aceitarTermos(int versao) {
        this.termosAceitosEm = LocalDateTime.now();
        this.termosVersao = versao;
    }

    public boolean termosPendentes(int versaoAtual) {
        return termosVersao == null || termosVersao < versaoAtual;
    }

    /** Invalida todos os tokens JWT já emitidos para este usuário. */
    public void encerrarSessoes() {
        this.versaoSessao++;
    }

    public void corrigirNome(String novoNome) {
        this.nome = novoNome;
    }

    public void desativar() {
        this.ativo = false;
        encerrarSessoes();
    }

    public void reativar() {
        this.ativo = true;
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
    }

    public boolean isAnonimizado() {
        return anonimizadoEm != null;
    }

    /**
     * Eliminação a pedido do titular (LGPD, art. 18, VI): remove nome, e-mail e login, troca a senha
     * por um valor aleatório e desativa a conta. O id continua existindo para não quebrar as
     * referências (questões de autoria, respostas e registros de auditoria), que passam a ser anônimas.
     */
    public void anonimizar(String hashAleatorio) {
        this.nome = "Usuário anonimizado #" + id;
        this.email = null;
        this.login = PREFIXO_LOGIN_ANONIMIZADO + id;
        this.senha = hashAleatorio;
        this.ativo = false;
        this.senhaTemporaria = true;
        this.termosAceitosEm = null;
        this.termosVersao = null;
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
        this.anonimizadoEm = LocalDateTime.now();
        encerrarSessoes();
    }
}
