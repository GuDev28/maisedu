package br.com.maisedu.app.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Questão escolhida pelo professor para a atividade, com a posição em que aparece. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "atividade_questao", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"atividade_id", "questao_id"}),
        @UniqueConstraint(columnNames = {"atividade_id", "ordem"})
})
public class AtividadeQuestao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atividade_id", nullable = false)
    private Atividade atividade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questao_id", nullable = false)
    private Questao questao;

    @Column(nullable = false)
    private short ordem;

    AtividadeQuestao(Atividade atividade, Questao questao, short ordem) {
        this.atividade = atividade;
        this.questao = questao;
        this.ordem = ordem;
    }
}
