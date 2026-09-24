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

    /** Identificador de acesso do aluno, gerado pela escola, como a matrícula por exemplo. */
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

    @Column(nullable = false, updatable = false)
    private final LocalDateTime criadoEm = LocalDateTime.now();

    /** Professor ou admin: identificado pelo e-mail. */
    public Usuario(String nome, String email, String senha, Instituicao instituicao) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.instituicao = instituicao;
    }

    /** Aluno: identificado pelo login gerado pela escola, sem e-mail. */
    public static Usuario novoAluno(String nome, String login, String senha, Instituicao instituicao) {
        Usuario aluno = new Usuario(nome, null, senha, instituicao);
        aluno.login = login;
        return aluno;
    }

    public boolean possuiRole(RoleNome roleNome) {
        for (Role r : roles) {
            if (r.getNome() == roleNome) {
                return true;
            }
        }
        return false;
    }
}
