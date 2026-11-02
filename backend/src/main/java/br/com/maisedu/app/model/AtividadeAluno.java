package br.com.maisedu.app.model;

import br.com.maisedu.app.exception.NegocioException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Atividade individual do aluno: uma atividade regular iniciada por ele ou uma prática adaptativa.
 * Guarda o snapshot imutável das questões entregues (RN03): o que o aluno recebeu não muda mesmo
 * que a atividade de origem seja alterada depois.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "atividade_aluno", uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id", "atividade_id"}))
public class AtividadeAluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Usuario aluno;

    /** Atividade de origem; nula na prática adaptativa (multiplicidade 0..1 do diagrama de classes). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atividade_id")
    private Atividade atividade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAtividade tipo;

    @Column(name = "iniciada_em", nullable = false, updatable = false)
    private final LocalDateTime iniciadaEm = LocalDateTime.now();

    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    @OneToMany(mappedBy = "atividadeAluno", cascade = CascadeType.ALL)
    @OrderBy("ordem ASC")
    private List<AtividadeAlunoQuestao> questoes = new ArrayList<>();

    private AtividadeAluno(Usuario aluno, Atividade atividade, TipoAtividade tipo) {
        this.aluno = aluno;
        this.atividade = atividade;
        this.tipo = tipo;
    }

    /** Snapshot de uma atividade regular, na ordem definida pelo professor. */
    public static AtividadeAluno regular(Usuario aluno, Atividade atividade) {
        AtividadeAluno atividadeAluno = new AtividadeAluno(aluno, atividade, TipoAtividade.REGULAR);
        List<Questao> questoes = new ArrayList<>();
        for (AtividadeQuestao atividadeQuestao : atividade.getQuestoes()) {
            questoes.add(atividadeQuestao.getQuestao());
        }
        atividadeAluno.fixarQuestoes(questoes);
        return atividadeAluno;
    }

    /** Prática adaptativa (RN06): sem atividade de origem, com as questões escolhidas pelo motor. */
    public static AtividadeAluno adaptativa(Usuario aluno, List<Questao> questoesSelecionadas) {
        AtividadeAluno pratica = new AtividadeAluno(aluno, null, TipoAtividade.ADAPTATIVA);
        pratica.fixarQuestoes(questoesSelecionadas);
        return pratica;
    }

    private void fixarQuestoes(List<Questao> questoesEmOrdem) {
        if (questoesEmOrdem.isEmpty()) {
            throw new NegocioException("Não há questões para entregar ao aluno");
        }
        short ordem = 1;
        for (Questao questao : questoesEmOrdem) {
            questoes.add(new AtividadeAlunoQuestao(this, questao, ordem));
            ordem++;
        }
    }

    public List<AtividadeAlunoQuestao> getQuestoes() {
        return Collections.unmodifiableList(questoes);
    }

    public boolean pertenceAo(Long alunoId) {
        return aluno.getId().equals(alunoId);
    }

    public boolean isConcluida() {
        return concluidaEm != null;
    }

    public void concluir() {
        if (concluidaEm == null) {
            concluidaEm = LocalDateTime.now();
        }
    }
}
