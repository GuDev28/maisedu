package br.com.maisedu.app.model;

import br.com.maisedu.app.exception.NegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDateTime;

/** RF15 (composição da atividade) e RN03 (snapshot imutável das questões entregues). */
class AtividadeTest {

    @Test
    void atividadeAceitaSoQuestoesAprovadasNaOrdemInformada() {
        Questao q1 = questao(1L, StatusQuestao.APROVADA);
        Questao q2 = questao(2L, StatusQuestao.APROVADA);
        Atividade atividade = new Atividade("  Revisão de frações  ", null, null);

        atividade.definirQuestoes(List.of(q2, q1));

        assertEquals("Revisão de frações", atividade.getTitulo());
        assertEquals(2, atividade.getQuestoes().size());
        assertSame(q2, atividade.getQuestoes().get(0).getQuestao());
        assertEquals(1, atividade.getQuestoes().get(0).getOrdem());
        assertSame(q1, atividade.getQuestoes().get(1).getQuestao());
        assertEquals(2, atividade.getQuestoes().get(1).getOrdem());
    }

    @Test
    void rf15Ca02_questaoNaoAprovadaNaoEntraNaAtividade() {
        for (StatusQuestao status : List.of(StatusQuestao.RASCUNHO, StatusQuestao.PENDENTE, StatusQuestao.REJEITADA)) {
            Atividade atividade = new Atividade("Revisão", null, null);
            Questao questao = questao(1L, status);

            NegocioException ex = assertThrows(NegocioException.class,
                    () -> atividade.definirQuestoes(List.of(questao)));

            assertTrue(ex.getMessage().startsWith("Só questões aprovadas"), status + ": " + ex.getMessage());
        }
    }

    @Test
    void atividadePrecisaDeAoMenosUmaQuestaoSemRepeticao() {
        Atividade atividade = new Atividade("Revisão", null, null);
        Questao q1 = questao(1L, StatusQuestao.APROVADA);

        assertThrows(NegocioException.class, () -> atividade.definirQuestoes(List.of()));
        NegocioException repetida = assertThrows(NegocioException.class,
                () -> atividade.definirQuestoes(List.of(q1, q1)));
        assertTrue(repetida.getMessage().contains("duas vezes"));
    }

    @Test
    void tituloEmBrancoERecusado() {
        assertThrows(NegocioException.class, () -> new Atividade("   ", null, null));
    }

    @Test
    void rn03_snapshotCopiaAsQuestoesEAOrdemENaoMudaComAAtividade() {
        Questao q1 = questao(1L, StatusQuestao.APROVADA);
        Questao q2 = questao(2L, StatusQuestao.APROVADA);
        Questao q3 = questao(3L, StatusQuestao.APROVADA);
        Atividade atividade = new Atividade("Revisão", null, null);
        atividade.definirQuestoes(List.of(q1, q2));

        AtividadeAluno snapshot = AtividadeAluno.regular(null, atividade);
        // A atividade é alterada depois que o aluno começou
        atividade.definirQuestoes(List.of(q3));

        assertEquals(TipoAtividade.REGULAR, snapshot.getTipo());
        assertSame(atividade, snapshot.getAtividade());
        assertEquals(2, snapshot.getQuestoes().size());
        assertSame(q1, snapshot.getQuestoes().get(0).getQuestao());
        assertSame(q2, snapshot.getQuestoes().get(1).getQuestao());
        assertFalse(snapshot.isConcluida());
    }

    @Test
    void praticaAdaptativaNaoTemAtividadeDeOrigem() {
        List<Questao> questoes = new ArrayList<>();
        questoes.add(questao(5L, StatusQuestao.APROVADA));

        AtividadeAluno pratica = AtividadeAluno.adaptativa(null, questoes);

        assertEquals(TipoAtividade.ADAPTATIVA, pratica.getTipo());
        assertNull(pratica.getAtividade());
        assertEquals(1, pratica.getQuestoes().size());
    }

    @Test
    void conclusaoEDefinitiva() {
        AtividadeAluno pratica = AtividadeAluno.adaptativa(null, List.of(questao(5L, StatusQuestao.APROVADA)));
        pratica.concluir();
        var primeira = pratica.getConcluidaEm();

        pratica.concluir();

        assertTrue(pratica.isConcluida());
        assertSame(primeira, pratica.getConcluidaEm());
    }

    // ---------- prazo de entrega ----------

    @Test
    void prazoPrecisaFicarAlgunsMinutosNoFuturoEAteUmAno() {
        assertThrows(NegocioException.class, () -> new Atividade("Lista", null, null, null));
        assertThrows(NegocioException.class,
                () -> new Atividade("Lista", null, null, LocalDateTime.now().minusMinutes(1)));
        assertThrows(NegocioException.class,
                () -> new Atividade("Lista", null, null, LocalDateTime.now().plusMinutes(2)));
        assertThrows(NegocioException.class,
                () -> new Atividade("Lista", null, null, LocalDateTime.now().plusDays(400)));

        LocalDateTime prazo = LocalDateTime.now().plusDays(3);
        assertEquals(prazo, new Atividade("Lista", null, null, prazo).getPrazo());
    }

    @Test
    void semPrazoInformadoValeOPadraoDeSeteDias() {
        Atividade atividade = new Atividade("Lista", null, null);

        assertEquals(atividade.getCriadoEm().plusDays(Atividade.DIAS_PRAZO_PADRAO), atividade.getPrazo());
    }

    @Test
    void oPrazoEncerraNoInstanteDoPrazo() {
        Atividade atividade = new Atividade("Lista", null, null, LocalDateTime.now().plusDays(1));
        LocalDateTime prazo = atividade.getPrazo();

        assertFalse(atividade.prazoEncerrado(prazo.minusSeconds(1)));
        assertTrue(atividade.prazoEncerrado(prazo));
    }

    @Test
    void prazoSoPodeSerAlteradoAntesDeTerminar() {
        Atividade atividade = new Atividade("Lista", null, null, LocalDateTime.now().plusDays(1));
        LocalDateTime novo = LocalDateTime.now().plusDays(5);

        atividade.alterarPrazo(novo, LocalDateTime.now());
        assertEquals(novo, atividade.getPrazo());

        assertThrows(NegocioException.class,
                () -> atividade.alterarPrazo(LocalDateTime.now().plusDays(9), novo.plusMinutes(1)));
        assertEquals(novo, atividade.getPrazo());
    }

    @Test
    void fechamentoSoDepoisDoPrazoEUmaVezSo() {
        Atividade atividade = new Atividade("Lista", null, null, LocalDateTime.now().plusDays(1));
        assertThrows(NegocioException.class, () -> atividade.fechar(LocalDateTime.now()));
        assertFalse(atividade.isFechada());

        LocalDateTime depois = atividade.getPrazo().plusMinutes(1);
        atividade.fechar(depois);
        atividade.fechar(depois.plusHours(1));

        assertTrue(atividade.isFechada());
        assertEquals(depois, atividade.getFechadaEm());
    }

    @Test
    void folhaRegularAceitaRespostasAteOPrazoECorrecaoSoDepoisDele() {
        Atividade atividade = new Atividade("Lista", null, null, LocalDateTime.now().plusDays(1));
        atividade.definirQuestoes(List.of(questao(1L, StatusQuestao.APROVADA)));
        AtividadeAluno folha = AtividadeAluno.regular(null, atividade);
        LocalDateTime antes = atividade.getPrazo().minusHours(1);
        LocalDateTime depois = atividade.getPrazo().plusMinutes(1);

        assertTrue(folha.aceitaRespostas(antes));
        assertFalse(folha.correcaoLiberada(antes));

        folha.concluir();
        // enviada: não aceita mais nada, mas a correção continua escondida até o prazo
        assertFalse(folha.aceitaRespostas(antes));
        assertFalse(folha.correcaoLiberada(antes));
        assertTrue(folha.correcaoLiberada(depois));
        assertEquals(atividade.getPrazo(), folha.prazo());
    }

    @Test
    void folhaNaoEnviadaDepoisDoPrazoNaoAceitaRespostasMasMostraOGabarito() {
        Atividade atividade = new Atividade("Lista", null, null, LocalDateTime.now().plusDays(1));
        atividade.definirQuestoes(List.of(questao(1L, StatusQuestao.APROVADA)));
        AtividadeAluno folha = AtividadeAluno.regular(null, atividade);
        LocalDateTime depois = atividade.getPrazo().plusMinutes(1);

        assertFalse(folha.aceitaRespostas(depois));
        assertTrue(folha.correcaoLiberada(depois));
    }

    @Test
    void praticaAdaptativaNaoTemPrazoECorrecaoSaiNoEnvio() {
        AtividadeAluno pratica = AtividadeAluno.adaptativa(null, List.of(questao(5L, StatusQuestao.APROVADA)));
        LocalDateTime agora = LocalDateTime.now();

        assertNull(pratica.prazo());
        assertTrue(pratica.aceitaRespostas(agora.plusYears(1)));
        assertFalse(pratica.correcaoLiberada(agora));
        pratica.concluir();
        assertTrue(pratica.correcaoLiberada(agora));
    }

    @Test
    void respostaEContabilizadaNoDesempenhoUmaVezSo() {
        Questao questao = questao(7L, StatusQuestao.RASCUNHO);
        questao.definirAlternativas(List.of("a", "b", "c", "d", "e"), 0);
        ReflectionTestUtils.setField(questao, "status", StatusQuestao.APROVADA);
        AtividadeAluno pratica = AtividadeAluno.adaptativa(null, List.of(questao));
        Resposta resposta = new Resposta(pratica.getQuestoes().get(0), questao.getAlternativas().get(0));
        assertFalse(resposta.isContabilizada());

        resposta.marcarContabilizada(LocalDateTime.now());

        assertTrue(resposta.isContabilizada());
        assertThrows(NegocioException.class, () -> resposta.marcarContabilizada(LocalDateTime.now()));
    }

    static Questao questao(Long id, StatusQuestao status) {
        Questao questao = new Questao("Questão " + id, null, null, NivelDificuldade.FACIL, AnoEscolar.SEXTO_ANO);
        ReflectionTestUtils.setField(questao, "id", id);
        ReflectionTestUtils.setField(questao, "status", status);
        return questao;
    }
}
