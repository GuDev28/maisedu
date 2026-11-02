package br.com.maisedu.app.model;

import br.com.maisedu.app.exception.NegocioException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Atividade regular criada por um professor para uma turma em que leciona (RF15). */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Atividade {

    public static final int TAMANHO_MAXIMO_TITULO = 150;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = TAMANHO_MAXIMO_TITULO)
    private String titulo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turma_id", nullable = false)
    private Turma turma;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Usuario professor;

    @OneToMany(mappedBy = "atividade", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<AtividadeQuestao> questoes = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private final LocalDateTime criadoEm = LocalDateTime.now();

    public Atividade(String titulo, Turma turma, Usuario professor) {
        if (titulo == null || titulo.isBlank()) {
            throw new NegocioException("Informe o título da atividade");
        }
        String tituloLimpo = titulo.strip();
        if (tituloLimpo.length() > TAMANHO_MAXIMO_TITULO) {
            throw new NegocioException("O título deve ter no máximo " + TAMANHO_MAXIMO_TITULO + " caracteres");
        }
        this.titulo = tituloLimpo;
        this.turma = turma;
        this.professor = professor;
    }

    /**
     * Define as questões na ordem recebida. Só questões APROVADAS (de qualquer autor e instituição),
     * pelo menos uma e sem repetição.
     */
    public void definirQuestoes(List<Questao> questoesEmOrdem) {
        if (questoesEmOrdem == null || questoesEmOrdem.isEmpty()) {
            throw new NegocioException("Selecione pelo menos uma questão para a atividade");
        }
        Set<Long> ids = new HashSet<>();
        for (Questao questao : questoesEmOrdem) {
            if (!questao.isAprovada()) {
                throw new NegocioException("Só questões aprovadas no banco compartilhado podem entrar em atividades"
                        + " (questão " + questao.getId() + " está " + questao.getStatus() + ")");
            }
            if (!ids.add(questao.getId())) {
                throw new NegocioException("A mesma questão não pode aparecer duas vezes na atividade");
            }
        }

        questoes.clear();
        short ordem = 1;
        for (Questao questao : questoesEmOrdem) {
            questoes.add(new AtividadeQuestao(this, questao, ordem));
            ordem++;
        }
    }

    public List<AtividadeQuestao> getQuestoes() {
        return Collections.unmodifiableList(questoes);
    }
}
