package br.com.maisedu.app.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Questão entregue ao aluno (item do snapshot da RN03). Admite no máximo uma resposta (RN04). */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "atividade_aluno_questao", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"atividade_aluno_id", "questao_id"}),
        @UniqueConstraint(columnNames = {"atividade_aluno_id", "ordem"})
})
public class AtividadeAlunoQuestao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atividade_aluno_id", nullable = false)
    private AtividadeAluno atividadeAluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questao_id", nullable = false)
    private Questao questao;

    @Column(nullable = false)
    private short ordem;

    AtividadeAlunoQuestao(AtividadeAluno atividadeAluno, Questao questao, short ordem) {
        this.atividadeAluno = atividadeAluno;
        this.questao = questao;
        this.ordem = ordem;
    }
}
