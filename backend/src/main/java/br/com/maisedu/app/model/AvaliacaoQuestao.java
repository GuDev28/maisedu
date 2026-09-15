package br.com.maisedu.app.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"questao_id", "professor_id"}))
public class AvaliacaoQuestao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questao_id", nullable = false)
    private Questao questao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Usuario professor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VotoAvaliacao voto;

    @Column(nullable = false, updatable = false)
    private final LocalDateTime avaliadoEm = LocalDateTime.now();

    public AvaliacaoQuestao(Questao questao, Usuario professor, VotoAvaliacao voto) {
        this.questao = questao;
        this.professor = professor;
        this.voto = voto;
    }
}
