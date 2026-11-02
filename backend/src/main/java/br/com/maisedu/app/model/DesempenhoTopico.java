package br.com.maisedu.app.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Desempenho acumulado (histórico total) de um aluno em um tópico e nível de dificuldade (RN05).
 * Existe um único registro por aluno + tópico + nível, atualizado a cada resposta.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "desempenho_topico",
        uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id", "topico_id", "nivel_dificuldade"}))
public class DesempenhoTopico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topico_id", nullable = false)
    private Topico topico;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_dificuldade", nullable = false, length = 10)
    private NivelDificuldade nivelDificuldade;

    @Column(name = "total_respostas", nullable = false)
    private int totalRespostas = 0;

    @Column(name = "total_acertos", nullable = false)
    private int totalAcertos = 0;

    @Column(name = "percentual_acerto", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualAcerto = BigDecimal.ZERO.setScale(2);

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public DesempenhoTopico(Usuario aluno, Topico topico, NivelDificuldade nivelDificuldade) {
        this.aluno = aluno;
        this.topico = topico;
        this.nivelDificuldade = nivelDificuldade;
    }

    /** RN05: incrementa o total de respostas, o de acertos (se houver) e recalcula o percentual. */
    public void registrarResposta(boolean acertou) {
        totalRespostas++;
        if (acertou) {
            totalAcertos++;
        }
        percentualAcerto = percentual(totalAcertos, totalRespostas);
        atualizadoEm = LocalDateTime.now();
    }

    /** RN06: um nível é dominado com um mínimo de respostas e percentual acumulado igual ou acima do limite. */
    public boolean dominado(int minimoRespostas, BigDecimal percentualMinimo) {
        return totalRespostas >= minimoRespostas && percentualAcerto.compareTo(percentualMinimo) >= 0;
    }

    /** Percentual de acerto = acertos ÷ respostas × 100, com duas casas decimais. */
    public static BigDecimal percentual(long acertos, long respostas) {
        if (respostas == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(acertos * 100L)
                .divide(BigDecimal.valueOf(respostas), 2, RoundingMode.HALF_UP);
    }
}
