package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.AlunoTurma;
import br.com.maisedu.app.model.ProfessorTurma;
import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Usuario;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Filtros da lista paginada de usuários. Como na auditoria, cada condição só entra na consulta
 * quando foi informada: nenhum parâmetro nulo chega ao banco. O papel e as turmas do professor são
 * conferidos com subconsultas (exists), para a paginação não repetir linhas.
 */
public final class FiltroUsuarios {

    private FiltroUsuarios() {
    }

    /** Admin: usuários da instituição, com papel e busca opcionais. */
    public static Specification<Usuario> daInstituicao(Long instituicaoId, RoleNome papel, String busca) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            condicoes.add(cb.equal(raiz.get("instituicao").get("id"), instituicaoId));
            if (papel != null) {
                Subquery<Long> comPapel = consulta.subquery(Long.class);
                Root<Usuario> outro = comPapel.from(Usuario.class);
                Join<Usuario, Role> role = outro.join("roles");
                comPapel.select(outro.<Long>get("id"))
                        .where(cb.equal(outro.get("id"), raiz.get("id")), cb.equal(role.get("nome"), papel));
                condicoes.add(cb.exists(comPapel));
            }
            adicionarBusca(condicoes, raiz, cb, busca);
            return cb.and(condicoes.toArray(new Predicate[0]));
        };
    }

    /** Professor: alunos vinculados às turmas em que ele leciona, com busca opcional. */
    public static Specification<Usuario> alunosDasTurmasDoProfessor(Long professorId, String busca) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            Subquery<Long> turmasDoProfessor = consulta.subquery(Long.class);
            Root<ProfessorTurma> leciona = turmasDoProfessor.from(ProfessorTurma.class);
            turmasDoProfessor.select(leciona.get("turma").<Long>get("id"))
                    .where(cb.equal(leciona.get("professor").get("id"), professorId));

            Subquery<Long> vinculado = consulta.subquery(Long.class);
            Root<AlunoTurma> vinculo = vinculado.from(AlunoTurma.class);
            vinculado.select(vinculo.<Long>get("id"))
                    .where(cb.equal(vinculo.get("aluno").get("id"), raiz.get("id")),
                            vinculo.get("turma").get("id").in(turmasDoProfessor));
            condicoes.add(cb.exists(vinculado));
            adicionarBusca(condicoes, raiz, cb, busca);
            return cb.and(condicoes.toArray(new Predicate[0]));
        };
    }

    /** Parte do nome, do login ou do e-mail, sem diferenciar maiúsculas. */
    private static void adicionarBusca(List<Predicate> condicoes, Root<Usuario> raiz,
                                       jakarta.persistence.criteria.CriteriaBuilder cb, String busca) {
        if (busca == null || busca.isBlank()) {
            return;
        }
        // % e _ digitados pelo usuário viram texto comum (não curingas)
        String termo = "%" + busca.strip().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "") + "%";
        condicoes.add(cb.or(
                cb.like(cb.lower(raiz.get("nome")), termo),
                cb.like(cb.lower(raiz.get("login")), termo),
                cb.like(cb.lower(raiz.get("email")), termo)));
    }
}
