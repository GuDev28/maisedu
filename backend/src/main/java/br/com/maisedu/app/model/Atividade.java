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

/**
 * Atividade regular criada por um professor para uma turma em que leciona (RF15), com prazo de
 * entrega. Até o prazo o aluno marca e troca as alternativas e envia tudo de uma vez; depois do
 * prazo a correção é liberada e a rotina de fechamento contabiliza o desempenho (RN05).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Atividade {

    public static final int TAMANHO_MAXIMO_TITULO = 150;
    /** Prazo usado quando nenhum é informado (só nos testes de unidade e no cenário de demonstração). */
    public static final int DIAS_PRAZO_PADRAO = 7;
    /** O prazo precisa ficar pelo menos alguns minutos à frente, para os alunos conseguirem responder. */
    public static final int MINUTOS_MINIMOS_ATE_O_PRAZO = 5;
    public static final int DIAS_MAXIMOS_ATE_O_PRAZO = 365;

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

    /** Até quando os alunos podem enviar as respostas. Depois dele a correção é liberada. */
    @Column(nullable = false)
    private LocalDateTime prazo;

    /** Quando a rotina de fechamento contabilizou o desempenho das respostas enviadas (nulo até lá). */
    @Column(name = "fechada_em")
    private LocalDateTime fechadaEm;

    public Atividade(String titulo, Turma turma, Usuario professor, LocalDateTime prazo) {
        this(titulo, turma, professor);
        definirPrazo(prazo, LocalDateTime.now());
    }

    /** Atividade com o prazo padrão de {@value #DIAS_PRAZO_PADRAO} dias. */
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
        this.prazo = criadoEm.plusDays(DIAS_PRAZO_PADRAO);
    }

    /** Novo prazo: no futuro (com folga mínima) e no máximo um ano à frente. */
    private void definirPrazo(LocalDateTime novoPrazo, LocalDateTime agora) {
        if (novoPrazo == null) {
            throw new NegocioException("Informe o prazo de entrega da atividade");
        }
        if (novoPrazo.isBefore(agora.plusMinutes(MINUTOS_MINIMOS_ATE_O_PRAZO))) {
            throw new NegocioException("O prazo de entrega precisa ser pelo menos "
                    + MINUTOS_MINIMOS_ATE_O_PRAZO + " minutos depois de agora");
        }
        if (novoPrazo.isAfter(agora.plusDays(DIAS_MAXIMOS_ATE_O_PRAZO))) {
            throw new NegocioException("O prazo de entrega pode ser de no máximo " + DIAS_MAXIMOS_ATE_O_PRAZO
                    + " dias");
        }
        this.prazo = novoPrazo;
    }

    /**
     * Professor estende ou antecipa o prazo. Só enquanto o prazo atual não terminou: depois dele a
     * correção já foi liberada aos alunos e o desempenho pode ter sido contabilizado.
     */
    public void alterarPrazo(LocalDateTime novoPrazo, LocalDateTime agora) {
        if (prazoEncerrado(agora)) {
            throw new NegocioException("O prazo desta atividade já terminou e não pode mais ser alterado");
        }
        definirPrazo(novoPrazo, agora);
    }

    /** O prazo terminou (a partir do instante do prazo, nada mais é aceito). */
    public boolean prazoEncerrado(LocalDateTime agora) {
        return !agora.isBefore(prazo);
    }

    public boolean isFechada() {
        return fechadaEm != null;
    }

    /** Marca que o desempenho das respostas enviadas já foi contabilizado. */
    public void fechar(LocalDateTime agora) {
        if (!prazoEncerrado(agora)) {
            throw new NegocioException("A atividade só é fechada depois do prazo");
        }
        if (fechadaEm == null) {
            fechadaEm = agora;
        }
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
