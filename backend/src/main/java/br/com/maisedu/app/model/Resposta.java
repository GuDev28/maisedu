package br.com.maisedu.app.model;

import br.com.maisedu.app.exception.NegocioException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Resposta do aluno a uma questão entregue. Não guarda aluno, questão nem acerto: tudo isso é
 * derivado da questão entregue e da alternativa escolhida, evitando dados duplicados (RN04).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atividade_aluno_questao_id", nullable = false, unique = true)
    private AtividadeAlunoQuestao atividadeAlunoQuestao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alternativa_id", nullable = false)
    private Alternativa alternativa;

    @Column(name = "respondida_em", nullable = false, updatable = false)
    private final LocalDateTime respondidaEm = LocalDateTime.now();

    public Resposta(AtividadeAlunoQuestao atividadeAlunoQuestao, Alternativa alternativa) {
        Long questaoEntregue = atividadeAlunoQuestao.getQuestao().getId();
        if (!alternativa.getQuestao().getId().equals(questaoEntregue)) {
            throw new NegocioException("A alternativa escolhida não pertence a esta questão");
        }
        this.atividadeAlunoQuestao = atividadeAlunoQuestao;
        this.alternativa = alternativa;
    }

    public boolean acertou() {
        return alternativa.isCorreta();
    }
}
