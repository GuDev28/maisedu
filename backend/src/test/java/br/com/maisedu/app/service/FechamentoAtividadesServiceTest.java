package br.com.maisedu.app.service;

import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.Atividade;
import br.com.maisedu.app.model.AtividadeAluno;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.Resposta;
import br.com.maisedu.app.model.StatusQuestao;
import br.com.maisedu.app.model.Topico;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AtividadeRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RN05 com entrega única: no fim do prazo, as respostas enviadas entram no desempenho uma única vez. */
@ExtendWith(MockitoExtension.class)
class FechamentoAtividadesServiceTest {

    private static final Long ATIVIDADE_ID = 40L;

    @Mock AtividadeRepository atividadeRepository;
    @Mock RespostaRepository respostaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock DesempenhoService desempenhoService;
    @Mock AuditoriaService auditoriaService;
    @Mock PlatformTransactionManager transactionManager;

    private FechamentoAtividadesService service;
    private Atividade atividade;
    private Usuario aluno;
    private Questao questao;

    @BeforeEach
    void setUp() {
        service = new FechamentoAtividadesService(atividadeRepository, respostaRepository, usuarioRepository,
                desempenhoService, auditoriaService, transactionManager);
        Instituicao escola = new Instituicao("Escola", "00.000.000/0001-00", "Rua 1");
        Turma turma = new Turma("7º A", escola, AnoEscolar.SETIMO_ANO);
        aluno = Usuario.novoAluno("Ana", "a123456", "hash", escola);
        ReflectionTestUtils.setField(aluno, "id", 10L);
        Topico topico = new Topico("Frações");
        ReflectionTestUtils.setField(topico, "id", 3L);
        questao = new Questao("Quanto é 1/2 + 1/2?", null, topico, NivelDificuldade.FACIL, AnoEscolar.SETIMO_ANO);
        ReflectionTestUtils.setField(questao, "id", 1L);
        questao.definirAlternativas(List.of("1", "2", "1/4", "0", "3"), 0);
        ReflectionTestUtils.setField(questao, "status", StatusQuestao.APROVADA);
        atividade = new Atividade("Lista", turma, null);
        atividade.definirQuestoes(List.of(questao));
        ReflectionTestUtils.setField(atividade, "id", ATIVIDADE_ID);
    }

    @Test
    void atividadeVencidaContabilizaAsRespostasEnviadasEFicaFechada() {
        ReflectionTestUtils.setField(atividade, "prazo", LocalDateTime.now().minusMinutes(1));
        AtividadeAluno folha = AtividadeAluno.regular(aluno, atividade);
        Resposta resposta = new Resposta(folha.getQuestoes().get(0), questao.getAlternativas().get(0));
        when(atividadeRepository.findByIdParaFechamento(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
        when(respostaRepository.findNaoContabilizadasDaAtividade(ATIVIDADE_ID)).thenReturn(List.of(resposta));
        when(usuarioRepository.findByIdParaAtualizacao(10L)).thenReturn(Optional.of(aluno));

        assertTrue(service.fechar(ATIVIDADE_ID));

        verify(desempenhoService).registrarResposta(aluno, questao, true);
        assertTrue(resposta.isContabilizada());
        assertTrue(atividade.isFechada());
        verify(auditoriaService).registrar(isNull(), eq("Sistema"), isNull(), eq(AcaoAuditoria.FECHAMENTO_ATIVIDADE),
                eq("Atividade"), eq(ATIVIDADE_ID), eq(true), anyString());
    }

    @Test
    void atividadeJaFechadaNaoContabilizaDeNovo() {
        ReflectionTestUtils.setField(atividade, "prazo", LocalDateTime.now().minusMinutes(5));
        atividade.fechar(LocalDateTime.now());
        when(atividadeRepository.findByIdParaFechamento(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));

        assertFalse(service.fechar(ATIVIDADE_ID));

        verify(respostaRepository, never()).findNaoContabilizadasDaAtividade(any());
        verifyNoInteractions(desempenhoService);
    }

    @Test
    void atividadeDentroDoPrazoNaoEFechada() {
        when(atividadeRepository.findByIdParaFechamento(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));

        assertFalse(service.fechar(ATIVIDADE_ID));

        assertFalse(atividade.isFechada());
        verify(desempenhoService, never()).registrarResposta(any(), any(), anyBoolean());
    }
}
