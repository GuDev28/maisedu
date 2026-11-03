package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.PraticaAdaptativaResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.AcaoAuditoria;
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
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AtividadeAlunoRepository;
import br.com.maisedu.app.repository.DesempenhoTopicoRepository;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
    @Mock AuditoriaService auditoriaService;

    private PraticaAdaptativaService service;
    private Usuario aluno;
    private Topico fracoes;
    private Topico porcentagem;

    @BeforeEach
    void setUp() {
        service = new PraticaAdaptativaService(usuarioRepository, desempenhoTopicoRepository, questaoRepository,
                respostaRepository, atividadeAlunoRepository, auditoriaService, 3, 10, 5, BigDecimal.valueOf(70));

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
        when(questaoRepository.findByStatusAndTopicoId(StatusQuestao.APROVADA, FRACOES_ID)).thenReturn(List.of(
                questao(11L, fracoes, NivelDificuldade.FACIL), questao(12L, fracoes, NivelDificuldade.FACIL),
                questao(13L, fracoes, NivelDificuldade.MEDIO)));
        when(questaoRepository.findByStatusAndTopicoId(StatusQuestao.APROVADA, PORCENTAGEM_ID)).thenReturn(List.of(
                questao(21L, porcentagem, NivelDificuldade.FACIL), questao(22L, porcentagem, NivelDificuldade.FACIL)));
        when(respostaRepository.findQuestoesRespondidasPeloAluno(ALUNO_ID)).thenReturn(List.of(12L));
        when(atividadeAlunoRepository.save(any(AtividadeAluno.class))).thenAnswer(invocacao -> {
            AtividadeAluno salva = invocacao.getArgument(0);
            ReflectionTestUtils.setField(salva, "id", 77L);
            return salva;
        });

        PraticaAdaptativaResponse resposta = service.gerarOuRetomar(ALUNO_ID);

        assertTrue(resposta.nova());
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
        verify(auditoriaService).registrar(eq(ALUNO_ID), eq("Ana Aluna"), eq(AcaoAuditoria.GERACAO_PRATICA_ADAPTATIVA),
                eq("AtividadeAluno"), eq(77L), eq(true), anyString());
    }

    @Test
    void rf18Ca03_semQuestoesAprovadasCompativeisAPraticaNaoEGerada() {
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findByAlunoIdAndTipo(ALUNO_ID, TipoAtividade.ADAPTATIVA)).thenReturn(List.of());
        when(desempenhoTopicoRepository.findByAlunoId(ALUNO_ID))
                .thenReturn(List.of(desempenho(fracoes, NivelDificuldade.FACIL, 5, 2)));
        when(questaoRepository.findByStatusAndTopicoId(StatusQuestao.APROVADA, FRACOES_ID)).thenReturn(List.of());
        when(respostaRepository.findQuestoesRespondidasPeloAluno(ALUNO_ID)).thenReturn(List.of());

        NegocioException ex = assertThrows(NegocioException.class, () -> service.gerarOuRetomar(ALUNO_ID));

        assertTrue(ex.getMessage().contains("Ainda não há questões aprovadas"));
        verify(atividadeAlunoRepository, never()).save(any());
    }

    @Test
    void semDesempenhoRegistradoAPraticaNaoEGerada() {
        when(usuarioRepository.findByIdParaAtualizacao(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(atividadeAlunoRepository.findByAlunoIdAndTipo(ALUNO_ID, TipoAtividade.ADAPTATIVA)).thenReturn(List.of());
        when(desempenhoTopicoRepository.findByAlunoId(ALUNO_ID)).thenReturn(List.of());

        NegocioException ex = assertThrows(NegocioException.class, () -> service.gerarOuRetomar(ALUNO_ID));

        assertTrue(ex.getMessage().contains("ainda não tem desempenho"));
        verifyNoInteractions(questaoRepository);
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

    private static Questao questao(Long id, Topico topico, NivelDificuldade nivel) {
        Questao questao = new Questao("Questão " + id, null, topico, nivel, AnoEscolar.SETIMO_ANO);
        ReflectionTestUtils.setField(questao, "id", id);
        questao.setStatus(StatusQuestao.APROVADA);
        return questao;
    }
}
