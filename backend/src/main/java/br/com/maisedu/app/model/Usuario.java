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

    public void trocarSenha(String novoHash) {
        this.senha = novoHash;
        this.senhaTemporaria = false;
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
    }

    public void aceitarTermos() {
        this.termosAceitosEm = LocalDateTime.now();
    }
}
