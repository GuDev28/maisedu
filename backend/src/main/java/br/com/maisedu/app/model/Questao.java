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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Questao {

    public static final int NUMERO_ALTERNATIVAS = 5;

    /** RN02: votos iguais que decidem a validação colaborativa (aprovar ou rejeitar). */
    public static final int VOTOS_PARA_DECIDIR = 3;

    /** RN02: cada questão recebe no máximo este número de votos, um por professor. */
    public static final int MAXIMO_VOTOS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String enunciado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topico_id", nullable = false)
    private Topico topico;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_dificuldade", nullable = false, length = 10)
    private NivelDificuldade nivelDificuldade;

    /**
     * Ano escolar a que a questão se destina (6º ao 9º ano). Obrigatório para questões novas; pode
     * ser nulo apenas em questões criadas antes da migration V8.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ano_escolar", length = 20)
    private AnoEscolar anoEscolar;

    @OneToMany(mappedBy = "questao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<Alternativa> alternativas = new ArrayList<>();

    /** Só muda pelas regras da entidade: {@link #enviarParaValidacao()} e {@link #apurarVotos(long, long)}. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusQuestao status;

    @Column(nullable = false, updatable = false)
    private final LocalDateTime criadoEm = LocalDateTime.now();

    public Questao(String enunciado, Usuario autor, Topico topico, NivelDificuldade nivelDificuldade) {
        this(enunciado, autor, topico, nivelDificuldade, null);
    }

    public Questao(String enunciado, Usuario autor, Topico topico, NivelDificuldade nivelDificuldade,
                   AnoEscolar anoEscolar) {
        this.enunciado = enunciado;
        this.autor = autor;
        this.topico = topico;
        this.nivelDificuldade = nivelDificuldade;
        this.anoEscolar = anoEscolar;
        this.status = StatusQuestao.RASCUNHO;
    }

    public List<Alternativa> getAlternativas() {
        return Collections.unmodifiableList(alternativas);
    }

  
    public void definirAlternativas(List<String> textos, int indiceCorreta) {
        if (status != StatusQuestao.RASCUNHO) {
            throw new NegocioException("Só é possível alterar alternativas enquanto a questão é um rascunho");
        }
        if (textos == null || textos.size() != NUMERO_ALTERNATIVAS) {
            throw new NegocioException("A questão deve ter exatamente " + NUMERO_ALTERNATIVAS + " alternativas");
        }
        for (String texto : textos) {
            if (texto == null || texto.isBlank()) {
                throw new NegocioException("Todas as alternativas devem ter texto");
            }
        }
        if (indiceCorreta < 0 || indiceCorreta >= textos.size()) {
            throw new NegocioException("Indique qual alternativa é a correta");
        }

        alternativas.clear();
        for (int i = 0; i < textos.size(); i++) {
            alternativas.add(new Alternativa(this, textos.get(i).strip(), i == indiceCorreta, (short) (i + 1)));
        }
    }

    
    public void enviarParaValidacao() {
        if (status != StatusQuestao.RASCUNHO) {
            throw new NegocioException("Só um rascunho pode ser enviado para validação");
        }
        if (alternativas.size() != NUMERO_ALTERNATIVAS) {
            throw new NegocioException("Defina as " + NUMERO_ALTERNATIVAS
                    + " alternativas antes de enviar a questão para validação");
        }
        status = StatusQuestao.PENDENTE;
    }

    /**
     * Edição do rascunho pelo autor: enquanto a questão não foi enviada para validação, todos os
     * campos podem mudar. Depois do envio, a questão fica congelada para os votos valerem.
     */
    public void editar(String novoEnunciado, Topico novoTopico, NivelDificuldade novoNivel, AnoEscolar novoAno,
                       List<String> textos, int indiceCorreta) {
        if (status != StatusQuestao.RASCUNHO) {
            throw new NegocioException("Só é possível editar a questão enquanto ela é um rascunho");
        }
        definirAlternativas(textos, indiceCorreta);
        this.enunciado = novoEnunciado;
        this.topico = novoTopico;
        this.nivelDificuldade = novoNivel;
        this.anoEscolar = novoAno;
    }

    /**
     * RN02: apura o placar depois de cada voto. A questão é aprovada (entra no banco compartilhado)
     * ou rejeitada quando atinge {@link #VOTOS_PARA_DECIDIR} votos no mesmo sentido; antes disso,
     * continua pendente.
     */
    public void apurarVotos(long aprovacoes, long rejeicoes) {
        if (status != StatusQuestao.PENDENTE) {
            throw new NegocioException("A validação desta questão já foi encerrada");
        }
        if (aprovacoes >= VOTOS_PARA_DECIDIR) {
            status = StatusQuestao.APROVADA;
        } else if (rejeicoes >= VOTOS_PARA_DECIDIR) {
            status = StatusQuestao.REJEITADA;
        }
    }

    public boolean isRascunho() {
        return status == StatusQuestao.RASCUNHO;
    }

    public boolean isAutor(Long usuarioId) {
        return autor.getId().equals(usuarioId);
    }

    public boolean isAprovada() {
        return status == StatusQuestao.APROVADA;
    }

    public Alternativa alternativaCorreta() {
        for (Alternativa alternativa : alternativas) {
            if (alternativa.isCorreta()) {
                return alternativa;
            }
        }
        throw new IllegalStateException("Questão " + id + " sem alternativa correta");
    }
}
