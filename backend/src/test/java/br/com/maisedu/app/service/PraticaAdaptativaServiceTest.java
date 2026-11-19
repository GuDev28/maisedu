package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.PainelPraticaResponse;
import br.com.maisedu.app.dto.PraticaAdaptativaResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.AlunoTurma;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.AtividadeAluno;
import br.com.maisedu.app.model.DesempenhoTopico;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.StatusQuestao;
import br.com.maisedu.app.model.TipoAtividade;
import br.com.maisedu.app.model.Topico;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlunoTurmaRepository;
import br.com.maisedu.app.repository.AtividadeAlunoRepository;
import br.com.maisedu.app.repository.DesempenhoTopicoRepository;
import br.com.maisedu.app.repository.Projecoes;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.TopicoRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** RF18: geração (ou retomada) da prática adaptativa. */
@ExtendWith(MockitoExtension.class)
class PraticaAdaptativaServiceTest {

    private static final Long ALUNO_ID = 10L;
    private static final Long FRACOES_ID = 3L;
    private static final Long PORCENTAGEM_ID = 6L;

    @Mock UsuarioRepository usuarioRepository;
    @Mock DesempenhoTopicoRepository desempenhoTopicoRepository;
    @Mock QuestaoRepository questaoRepository;
    @Mock RespostaRepository respostaRepository;
    @Mock AtividadeAlunoRepository atividadeAlunoRepository;
    @Mock AlunoTurmaRepository alunoTurmaRepository;
    @Mock TopicoRepository topicoRepository;
    @Mock AuditoriaService auditoriaService;

    private PraticaAdaptativaService service;
    private Usuario aluno;
    private Topico fracoes;
    private Topico porcentagem;

    @BeforeEach
    void setUp() {
        MotorAdaptativo motor = new MotorAdaptativo(new MotorAdaptativo.Parametros(3, 10, 5, BigDecimal.valueOf(70)));
        service = new PraticaAdaptativaService(usuarioRepository, desempenhoTopicoRepository, questaoRepository,
                respostaRepository, atividadeAlunoRepository, alunoTurmaRepository, topicoRepository, auditoriaService,
                motor);

        Instituicao escola = new Instituicao("Escola", "00.000.000/0001-00", "Rua 1");
        ReflectionTestUtils.setField(escola, "id", 1L);
        aluno = Usuario.novoAluno("Ana Aluna", "2026001", "hash", escola);
        ReflectionTestUtils.setField(aluno, "id", ALUNO_ID);
        aluno.adicionarRole(new Role(RoleNome.ALUNO));

        fracoes = topico(FRACOES_ID, "Frações");
        porcentagem = topico(PORCENTAGEM_ID, "Porcentagem");
    }

    @Test
    void rf18Ca01Ca04_geraPraticaAdaptativaComAprovadasPriorizandoOTopicoMaisFraco() {
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findByAlunoIdAndTipo(ALUNO_ID, TipoAtividade.ADAPTATIVA)).thenReturn(List.of());
        when(desempenhoTopicoRepository.findByAlunoId(ALUNO_ID)).thenReturn(List.of(
                desempenho(fracoes, NivelDificuldade.FACIL, 10, 4),
                desempenho(porcentagem, NivelDificuldade.FACIL, 10, 8)));
        when(questaoRepository.findByStatusAndTopicoIdIn(StatusQuestao.APROVADA, List.of(FRACOES_ID, PORCENTAGEM_ID)))
                .thenReturn(List.of(
                        questao(11L, fracoes, NivelDificuldade.FACIL), questao(12L, fracoes, NivelDificuldade.FACIL),
                        questao(13L, fracoes, NivelDificuldade.MEDIO),
                        questao(21L, porcentagem, NivelDificuldade.FACIL), questao(22L, porcentagem, NivelDificuldade.FACIL)));
        when(respostaRepository.findQuestoesRespondidasPeloAluno(ALUNO_ID)).thenReturn(List.of(12L));
        when(atividadeAlunoRepository.save(any(AtividadeAluno.class))).thenAnswer(invocacao -> {
            AtividadeAluno salva = invocacao.getArgument(0);
            ReflectionTestUtils.setField(salva, "id", 77L);
            return salva;
        });

        PraticaAdaptativaResponse resposta = service.gerarOuRetomar(ALUNO_ID);

        assertTrue(resposta.nova());
        assertFalse(resposta.diagnostica());
        assertEquals(77L, resposta.atividadeAlunoId());
        assertEquals(5, resposta.totalQuestoes());

        ArgumentCaptor<AtividadeAluno> salva = ArgumentCaptor.forClass(AtividadeAluno.class);
        verify(atividadeAlunoRepository).save(salva.capture());
        AtividadeAluno pratica = salva.getValue();
        assertEquals(TipoAtividade.ADAPTATIVA, pratica.getTipo());
        assertNull(pratica.getAtividade());
        // Frações (40%) vem antes de Porcentagem (80%), e a questão inédita do nível-alvo vem primeiro
        assertEquals(11L, pratica.getQuestoes().get(0).getQuestao().getId());
        for (var entregue : pratica.getQuestoes()) {
            assertEquals(StatusQuestao.APROVADA, entregue.getQuestao().getStatus());
        }
        verify(auditoriaService).registrar(eq(ALUNO_ID), eq("Ana Aluna"), isNull(),
                eq(AcaoAuditoria.GERACAO_PRATICA_ADAPTATIVA), eq("AtividadeAluno"), eq(77L), eq(true), anyString());
    }

    @Test
    void rf18Ca03_semQuestoesAprovadasCompativeisAPraticaNaoEGerada() {
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findByAlunoIdAndTipo(ALUNO_ID, TipoAtividade.ADAPTATIVA)).thenReturn(List.of());
        when(desempenhoTopicoRepository.findByAlunoId(ALUNO_ID))
                .thenReturn(List.of(desempenho(fracoes, NivelDificuldade.FACIL, 5, 2)));
        when(questaoRepository.findByStatusAndTopicoIdIn(StatusQuestao.APROVADA, List.of(FRACOES_ID))).thenReturn(List.of());
        when(respostaRepository.findQuestoesRespondidasPeloAluno(ALUNO_ID)).thenReturn(List.of());

        NegocioException ex = assertThrows(NegocioException.class, () -> service.gerarOuRetomar(ALUNO_ID));

        assertTrue(ex.getMessage().contains("Ainda não há questões aprovadas"));
        verify(atividadeAlunoRepository, never()).save(any());
    }

    @Test
    void semDesempenhoGeraPraticaDiagnosticaComQuestoesFaceisDeVariosTopicosDoAnoDaTurma() {
        Turma turma = new Turma("7º ano A", aluno.getInstituicao(), AnoEscolar.SETIMO_ANO);
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findByAlunoIdAndTipo(ALUNO_ID, TipoAtividade.ADAPTATIVA)).thenReturn(List.of());
        when(desempenhoTopicoRepository.findByAlunoId(ALUNO_ID)).thenReturn(List.of());
        when(alunoTurmaRepository.findByAlunoIdComTurma(ALUNO_ID)).thenReturn(List.of(new AlunoTurma(aluno, turma)));
        when(questaoRepository.buscarNoBanco(StatusQuestao.APROVADA, null, null, AnoEscolar.SETIMO_ANO)).thenReturn(List.of(
                questao(11L, fracoes, NivelDificuldade.FACIL), questao(12L, fracoes, NivelDificuldade.FACIL),
                questao(13L, fracoes, NivelDificuldade.MEDIO), questao(14L, fracoes, NivelDificuldade.DIFICIL),
                questao(21L, porcentagem, NivelDificuldade.FACIL)));
        when(respostaRepository.findQuestoesRespondidasPeloAluno(ALUNO_ID)).thenReturn(List.of());
        when(atividadeAlunoRepository.save(any(AtividadeAluno.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        PraticaAdaptativaResponse resposta = service.gerarOuRetomar(ALUNO_ID);

        assertTrue(resposta.nova());
        assertTrue(resposta.diagnostica());
        // 3 fáceis + a média como reserva; a difícil fica de fora da diagnóstica
        assertEquals(4, resposta.totalQuestoes());
        ArgumentCaptor<AtividadeAluno> salva = ArgumentCaptor.forClass(AtividadeAluno.class);
        verify(atividadeAlunoRepository).save(salva.capture());
        List<Long> ids = new ArrayList<>();
        for (var entregue : salva.getValue().getQuestoes()) {
            ids.add(entregue.getQuestao().getId());
        }
        assertFalse(ids.contains(14L));
        assertEquals(13L, ids.get(ids.size() - 1));
        // as duas primeiras vagas são de tópicos diferentes (intercalação)
        assertNotEquals(salva.getValue().getQuestoes().get(0).getQuestao().getTopico(),
                salva.getValue().getQuestoes().get(1).getQuestao().getTopico());
        verify(auditoriaService).registrar(eq(ALUNO_ID), eq("Ana Aluna"), isNull(),
                eq(AcaoAuditoria.GERACAO_PRATICA_ADAPTATIVA), eq("AtividadeAluno"), any(), eq(true),
                startsWith("diagnóstica"));
        verify(questaoRepository, never()).findByStatusAndTopicoIdIn(any(), any());
    }

    @Test
    void diagnosticaDeAlunoSemTurmaUsaQuestoesDeTodosOsAnos() {
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findByAlunoIdAndTipo(ALUNO_ID, TipoAtividade.ADAPTATIVA)).thenReturn(List.of());
        when(desempenhoTopicoRepository.findByAlunoId(ALUNO_ID)).thenReturn(List.of());
        when(alunoTurmaRepository.findByAlunoIdComTurma(ALUNO_ID)).thenReturn(List.of());
        when(questaoRepository.buscarNoBanco(StatusQuestao.APROVADA, null, null, null))
                .thenReturn(List.of(questao(21L, porcentagem, NivelDificuldade.FACIL)));
        when(respostaRepository.findQuestoesRespondidasPeloAluno(ALUNO_ID)).thenReturn(List.of());
        when(atividadeAlunoRepository.save(any(AtividadeAluno.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        PraticaAdaptativaResponse resposta = service.gerarOuRetomar(ALUNO_ID);

        assertTrue(resposta.diagnostica());
        assertEquals(1, resposta.totalQuestoes());
    }

    @Test
    void diagnosticaSemQuestoesAprovadasNoAnoAvisaOAluno() {
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findByAlunoIdAndTipo(ALUNO_ID, TipoAtividade.ADAPTATIVA)).thenReturn(List.of());
        when(desempenhoTopicoRepository.findByAlunoId(ALUNO_ID)).thenReturn(List.of());
        when(alunoTurmaRepository.findByAlunoIdComTurma(ALUNO_ID)).thenReturn(List.of());
        when(questaoRepository.buscarNoBanco(StatusQuestao.APROVADA, null, null, null)).thenReturn(List.of());
        when(respostaRepository.findQuestoesRespondidasPeloAluno(ALUNO_ID)).thenReturn(List.of());

        NegocioException ex = assertThrows(NegocioException.class, () -> service.gerarOuRetomar(ALUNO_ID));

        assertTrue(ex.getMessage().contains("para o seu ano escolar"));
        verify(atividadeAlunoRepository, never()).save(any());
    }

    @Test
    void praticaEmAndamentoERetomadaEmVezDeGerarOutra() {
        List<Questao> questoes = new ArrayList<>();
        questoes.add(questao(11L, fracoes, NivelDificuldade.FACIL));
        AtividadeAluno aberta = AtividadeAluno.adaptativa(aluno, questoes);
        ReflectionTestUtils.setField(aberta, "id", 55L);
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findByAlunoIdAndTipo(ALUNO_ID, TipoAtividade.ADAPTATIVA))
                .thenReturn(List.of(aberta));

        PraticaAdaptativaResponse resposta = service.gerarOuRetomar(ALUNO_ID);

        assertFalse(resposta.nova());
        assertEquals(55L, resposta.atividadeAlunoId());
        assertEquals("Frações", resposta.topicos().get(0).topico());
        verifyNoInteractions(desempenhoTopicoRepository, questaoRepository);
        verify(atividadeAlunoRepository, never()).save(any());
    }

    @Test
    void praticaRecomendadaUsaSoQuestoesDoAnoDoAlunoQuandoOTopicoTemQuestoesDesseAno() {
        Turma sexto = new Turma("6º ano B", aluno.getInstituicao(), AnoEscolar.SEXTO_ANO);
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findByAlunoIdAndTipo(ALUNO_ID, TipoAtividade.ADAPTATIVA)).thenReturn(List.of());
        when(desempenhoTopicoRepository.findByAlunoId(ALUNO_ID))
                .thenReturn(List.of(desempenho(fracoes, NivelDificuldade.FACIL, 10, 4)));
        when(alunoTurmaRepository.findByAlunoIdComTurma(ALUNO_ID)).thenReturn(List.of(new AlunoTurma(aluno, sexto)));
        // Frações tem questões do 6º e do 9º ano no banco compartilhado
        when(questaoRepository.findByStatusAndTopicoIdIn(StatusQuestao.APROVADA, List.of(FRACOES_ID))).thenReturn(List.of(
                questaoDoAno(31L, fracoes, NivelDificuldade.FACIL, AnoEscolar.SEXTO_ANO),
                questaoDoAno(32L, fracoes, NivelDificuldade.FACIL, AnoEscolar.NONO_ANO),
                questaoDoAno(33L, fracoes, NivelDificuldade.MEDIO, AnoEscolar.SEXTO_ANO),
                questaoDoAno(34L, fracoes, NivelDificuldade.MEDIO, AnoEscolar.NONO_ANO)));
        when(respostaRepository.findQuestoesRespondidasPeloAluno(ALUNO_ID)).thenReturn(List.of());
        when(atividadeAlunoRepository.save(any(AtividadeAluno.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        PraticaAdaptativaResponse resposta = service.gerarOuRetomar(ALUNO_ID);

        assertEquals(2, resposta.totalQuestoes());
        ArgumentCaptor<AtividadeAluno> salva = ArgumentCaptor.forClass(AtividadeAluno.class);
        verify(atividadeAlunoRepository).save(salva.capture());
        for (var entregue : salva.getValue().getQuestoes()) {
            assertEquals(AnoEscolar.SEXTO_ANO, entregue.getQuestao().getAnoEscolar());
        }
    }

    @Test
    void topicoSemNenhumaQuestaoDoAnoUsaAsDeOutrosAnosEmVezDeGerarPraticaVazia() {
        Turma oitavo = new Turma("8º ano C", aluno.getInstituicao(), AnoEscolar.OITAVO_ANO);
        Topico pitagoras = topico(15L, "Teorema de Pitágoras");
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findByAlunoIdAndTipo(ALUNO_ID, TipoAtividade.ADAPTATIVA)).thenReturn(List.of());
        when(desempenhoTopicoRepository.findByAlunoId(ALUNO_ID))
                .thenReturn(List.of(desempenho(pitagoras, NivelDificuldade.FACIL, 6, 2)));
        when(alunoTurmaRepository.findByAlunoIdComTurma(ALUNO_ID)).thenReturn(List.of(new AlunoTurma(aluno, oitavo)));
        // o professor usou questões de Pitágoras do 9º ano numa atividade do 8º
        when(questaoRepository.findByStatusAndTopicoIdIn(StatusQuestao.APROVADA, List.of(15L))).thenReturn(List.of(
                questaoDoAno(41L, pitagoras, NivelDificuldade.FACIL, AnoEscolar.NONO_ANO)));
        when(respostaRepository.findQuestoesRespondidasPeloAluno(ALUNO_ID)).thenReturn(List.of());
        when(atividadeAlunoRepository.save(any(AtividadeAluno.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        PraticaAdaptativaResponse resposta = service.gerarOuRetomar(ALUNO_ID);

        assertEquals(1, resposta.totalQuestoes());
    }

    @Test
    void praticaDoTopicoEscolhidoTemAteCincoQuestoesDoNivelEmQueOAlunoEsta() {
        Turma sexto = new Turma("6º ano B", aluno.getInstituicao(), AnoEscolar.SEXTO_ANO);
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findByAlunoIdAndTipo(ALUNO_ID, TipoAtividade.ADAPTATIVA)).thenReturn(List.of());
        // FÁCIL dominado (6 de 7): o nível-alvo passa a ser o MÉDIO
        when(desempenhoTopicoRepository.findByAlunoId(ALUNO_ID))
                .thenReturn(List.of(desempenho(fracoes, NivelDificuldade.FACIL, 7, 6)));
        when(alunoTurmaRepository.findByAlunoIdComTurma(ALUNO_ID)).thenReturn(List.of(new AlunoTurma(aluno, sexto)));
        when(topicoRepository.findById(FRACOES_ID)).thenReturn(Optional.of(fracoes));
        List<Questao> doTopico = new ArrayList<>();
        for (long i = 1; i <= 4; i++) {
            doTopico.add(questaoDoAno(50 + i, fracoes, NivelDificuldade.FACIL, AnoEscolar.SEXTO_ANO));
            doTopico.add(questaoDoAno(60 + i, fracoes, NivelDificuldade.MEDIO, AnoEscolar.SEXTO_ANO));
        }
        when(questaoRepository.findByStatusAndTopicoIdIn(StatusQuestao.APROVADA, List.of(FRACOES_ID))).thenReturn(doTopico);
        when(respostaRepository.findQuestoesRespondidasPeloAluno(ALUNO_ID)).thenReturn(List.of());
        when(atividadeAlunoRepository.save(any(AtividadeAluno.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        PraticaAdaptativaResponse resposta = service.gerarOuRetomar(ALUNO_ID, FRACOES_ID);

        assertTrue(resposta.nova());
        assertEquals("Frações", resposta.topico());
        assertEquals(5, resposta.totalQuestoes());
        ArgumentCaptor<AtividadeAluno> salva = ArgumentCaptor.forClass(AtividadeAluno.class);
        verify(atividadeAlunoRepository).save(salva.capture());
        assertEquals(fracoes, salva.getValue().getTopico());
        // as 4 médias (nível-alvo) vêm primeiro; a 5ª vaga é de uma fácil (nível adjacente)
        for (int i = 0; i < 4; i++) {
            assertEquals(NivelDificuldade.MEDIO, salva.getValue().getQuestoes().get(i).getQuestao().getNivelDificuldade());
        }
    }

    @Test
    void pedirOutroTopicoNaoRetomaAPraticaRecomendadaAberta() {
        AtividadeAluno recomendadaAberta = AtividadeAluno.adaptativa(aluno,
                new ArrayList<>(List.of(questao(11L, fracoes, NivelDificuldade.FACIL))));
        ReflectionTestUtils.setField(recomendadaAberta, "id", 55L);
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findByAlunoIdAndTipo(ALUNO_ID, TipoAtividade.ADAPTATIVA))
                .thenReturn(List.of(recomendadaAberta));
        when(desempenhoTopicoRepository.findByAlunoId(ALUNO_ID)).thenReturn(List.of());
        when(topicoRepository.findById(PORCENTAGEM_ID)).thenReturn(Optional.of(porcentagem));
        when(questaoRepository.findByStatusAndTopicoIdIn(StatusQuestao.APROVADA, List.of(PORCENTAGEM_ID)))
                .thenReturn(List.of(questao(21L, porcentagem, NivelDificuldade.FACIL)));
        when(respostaRepository.findQuestoesRespondidasPeloAluno(ALUNO_ID)).thenReturn(List.of());
        when(atividadeAlunoRepository.save(any(AtividadeAluno.class))).thenAnswer(invocacao -> {
            AtividadeAluno salva = invocacao.getArgument(0);
            ReflectionTestUtils.setField(salva, "id", 56L);
            return salva;
        });

        PraticaAdaptativaResponse resposta = service.gerarOuRetomar(ALUNO_ID, PORCENTAGEM_ID);

        assertTrue(resposta.nova());
        assertEquals(56L, resposta.atividadeAlunoId());
    }

    @Test
    void topicoSemQuestaoDoAnoESemHistoricoNaoEDaSerieDoAluno() {
        Turma sexto = new Turma("6º ano B", aluno.getInstituicao(), AnoEscolar.SEXTO_ANO);
        Topico pitagoras = topico(15L, "Teorema de Pitágoras");
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findByAlunoIdAndTipo(ALUNO_ID, TipoAtividade.ADAPTATIVA)).thenReturn(List.of());
        when(desempenhoTopicoRepository.findByAlunoId(ALUNO_ID)).thenReturn(List.of());
        when(alunoTurmaRepository.findByAlunoIdComTurma(ALUNO_ID)).thenReturn(List.of(new AlunoTurma(aluno, sexto)));
        when(topicoRepository.findById(15L)).thenReturn(Optional.of(pitagoras));
        when(questaoRepository.findByStatusAndTopicoIdIn(StatusQuestao.APROVADA, List.of(15L))).thenReturn(List.of(
                questaoDoAno(41L, pitagoras, NivelDificuldade.FACIL, AnoEscolar.NONO_ANO)));

        NegocioException ex = assertThrows(NegocioException.class, () -> service.gerarOuRetomar(ALUNO_ID, 15L));

        assertTrue(ex.getMessage().contains("para o seu ano escolar"));
        verify(atividadeAlunoRepository, never()).save(any());
    }

    @Test
    void painelMostraARecomendadaEUmCardPorTopicoDoAno() {
        Turma setimo = new Turma("7º ano A", aluno.getInstituicao(), AnoEscolar.SETIMO_ANO);
        AtividadeAluno abertaDeFracoes = AtividadeAluno.adaptativa(aluno, fracoes,
                new ArrayList<>(List.of(questao(11L, fracoes, NivelDificuldade.FACIL))));
        ReflectionTestUtils.setField(abertaDeFracoes, "id", 90L);
        when(desempenhoTopicoRepository.findByAlunoId(ALUNO_ID)).thenReturn(List.of(
                desempenho(fracoes, NivelDificuldade.FACIL, 10, 4),
                desempenho(porcentagem, NivelDificuldade.FACIL, 10, 9)));
        when(alunoTurmaRepository.findByAlunoIdComTurma(ALUNO_ID)).thenReturn(List.of(new AlunoTurma(aluno, setimo)));
        when(atividadeAlunoRepository.findAbertasPorTipo(ALUNO_ID, TipoAtividade.ADAPTATIVA))
                .thenReturn(List.of(abertaDeFracoes));
        when(questaoRepository.contarPorTopicoNosAnos(StatusQuestao.APROVADA, Set.of(AnoEscolar.SETIMO_ANO)))
                .thenReturn(List.of(new Projecoes.ContagemTopico(FRACOES_ID, "Frações", 12L),
                        new Projecoes.ContagemTopico(PORCENTAGEM_ID, "Porcentagem", 8L)));

        PainelPraticaResponse painel = service.painel(ALUNO_ID);

        assertNull(painel.recomendada().praticaEmAndamentoId());
        assertFalse(painel.recomendada().diagnostica());
        assertEquals("Frações", painel.recomendada().topicos().get(0).topico());
        assertEquals(2, painel.topicos().size());
        PainelPraticaResponse.TopicoParaPraticar cardFracoes = painel.topicos().get(0);
        assertEquals("Frações", cardFracoes.topico());
        assertEquals(12, cardFracoes.questoesDisponiveis());
        assertEquals(90L, cardFracoes.praticaEmAndamentoId());
        assertEquals("FACIL", cardFracoes.nivelAtual());
        assertTrue(cardFracoes.recomendado());
        assertNotNull(cardFracoes.percentual());
    }

    @Test
    void soAlunosGeramPratica() {
        Usuario professor = new Usuario("Paulo", "paulo@escola.test", "hash", aluno.getInstituicao());
        ReflectionTestUtils.setField(professor, "id", 7L);
        professor.adicionarRole(new Role(RoleNome.PROFESSOR));
        when(usuarioRepository.findByIdParaAtualizacao(7L)).thenReturn(Optional.of(professor));

        assertThrows(AccessDeniedException.class, () -> service.gerarOuRetomar(7L));
    }

    private static Topico topico(Long id, String nome) {
        Topico topico = new Topico(nome);
        ReflectionTestUtils.setField(topico, "id", id);
        return topico;
    }

    private DesempenhoTopico desempenho(Topico topico, NivelDificuldade nivel, int respostas, int acertos) {
        DesempenhoTopico registro = new DesempenhoTopico(aluno, topico, nivel);
        for (int i = 0; i < respostas; i++) {
            registro.registrarResposta(i < acertos);
        }
        return registro;
    }

    private static Questao questaoDoAno(Long id, Topico topico, NivelDificuldade nivel, AnoEscolar ano) {
        Questao questao = new Questao("Questão " + id, null, topico, nivel, ano);
        ReflectionTestUtils.setField(questao, "id", id);
        ReflectionTestUtils.setField(questao, "status", StatusQuestao.APROVADA);
        return questao;
    }

    private static Questao questao(Long id, Topico topico, NivelDificuldade nivel) {
        Questao questao = new Questao("Questão " + id, null, topico, nivel, AnoEscolar.SETIMO_ANO);
        ReflectionTestUtils.setField(questao, "id", id);
        ReflectionTestUtils.setField(questao, "status", StatusQuestao.APROVADA);
        return questao;
    }
}
