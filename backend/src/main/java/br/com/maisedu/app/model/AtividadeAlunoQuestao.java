package br.com.maisedu.app.model;

import br.com.maisedu.app.exception.NegocioException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    /**
     * Marcação provisória: a alternativa escolhida e ainda não confirmada. O aluno troca quantas vezes
     * quiser; só a confirmação grava a {@link Resposta} (única, RN04) e atualiza o desempenho.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alternativa_marcada_id")
    private Alternativa alternativaMarcada;

    @Column(name = "marcada_em")
    private LocalDateTime marcadaEm;

    AtividadeAlunoQuestao(AtividadeAluno atividadeAluno, Questao questao, short ordem) {
        this.atividadeAluno = atividadeAluno;
        this.questao = questao;
        this.ordem = ordem;
    }

    public void marcar(Alternativa alternativa) {
        if (!alternativa.getQuestao().getId().equals(questao.getId())) {
            throw new NegocioException("A alternativa escolhida não pertence a esta questão");
        }
        this.alternativaMarcada = alternativa;
        this.marcadaEm = LocalDateTime.now();
    }

    /** Depois da confirmação, a marcação deixa de existir: o que vale é a resposta. */
    public void limparMarcacao() {
        this.alternativaMarcada = null;
        this.marcadaEm = null;
    }
}
