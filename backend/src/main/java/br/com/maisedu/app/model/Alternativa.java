package br.com.maisedu.app.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alternativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questao_id", nullable = false)
    private Questao questao;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(nullable = false)
    private boolean correta;

    @Column(nullable = false)
    private short ordem;

    Alternativa(Questao questao, String texto, boolean correta, short ordem) {
        this.questao = questao;
        this.texto = texto;
        this.correta = correta;
        this.ordem = ordem;
    }

    public char letra() {
        return (char) ('A' + ordem - 1);
    }
}
