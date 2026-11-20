package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.DesempenhoAlunoResponse;
import br.com.maisedu.app.dto.DesempenhoTurmaResponse;
import br.com.maisedu.app.dto.RespostaHistorico;
import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
import br.com.maisedu.app.model.AlunoTurma;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.DesempenhoTopico;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Topico;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlunoTurmaRepository;
import br.com.maisedu.app.repository.DesempenhoTopicoRepository;
import br.com.maisedu.app.repository.Projecoes;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * RN05 (registro da resposta no desempenho acumulado) e RF19 (painel do aluno, do aluno visto pelo
 * professor e da turma). Os cálculos estáticos de evolução e efeito da prática têm testes próprios.
 */
@ExtendWith(MockitoExtension.class)
class DesempenhoServiceTest {

    private static final Long TURMA_ID = 100L;

    @Mock DesempenhoTopicoRepository desempenhoTopicoRepository;
    @Mock RespostaRepository respostaRepository;
    @Mock AlunoTurmaRepository alunoTurmaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock AcessoTurma acessoTurma;

    private DesempenhoService service;
    private Turma turma;
    private Usuario ana;
    private Usuario bia;
    private Usuario caio;
    private Topico fracoes;
    private Topico potencias;
    private UsuarioAutenticado professor;

    @BeforeEach
    void setUp() {
        service = new DesempenhoService(desempenhoTopicoRepository, respostaRepository, alunoTurmaRepository,
                usuarioRepository, acessoTurma, new MotorAdaptativo(MotorAdaptativo.Parametros.padrao()));

        Instituicao escola = new Instituicao("Escola", "00.000.000/0001-00", "Rua 1");
        ReflectionTestUtils.setField(escola, "id", 1L);
        turma = new Turma("7º ano A", escola, AnoEscolar.SETIMO_ANO);
        ReflectionTestUtils.setField(turma, "id", TURMA_ID);

        ana = aluno(20L, "Ana", escola);
        bia = aluno(21L, "Bia", escola);
        caio = aluno(22L, "Caio", escola);

        fracoes = topico(5L, "Frações");
        potencias = topico(6L, "Potências");

        professor = new UsuarioAutenticado(7L, "Paulo", 1L, EnumSet.of(RoleNome.PROFESSOR), false);
    }

    private static Usuario aluno(Long id, String nome, Instituicao escola) {
        Usuario aluno = Usuario.novoAluno(nome, "login-" + id, "hash", escola);
        ReflectionTestUtils.setField(aluno, "id", id);
        return aluno;
    }

    private static Topico topico(Long id, String nome) {
        Topico topico = new Topico(nome);
        ReflectionTestUtils.setField(topico, "id", id);
        return topico;
    }

    /** Registro já com {@code respostas} respostas, das quais {@code acertos} certas. */
    private static DesempenhoTopico registro(Usuario aluno, Topico topico, NivelDificuldade nivel,
                                             int respostas, int acertos) {
        DesempenhoTopico registro = new DesempenhoTopico(aluno, topico, nivel);
        for (int i = 0; i < respostas; i++) {
            registro.registrarResposta(i < acertos);
        }
        return registro;
    }

    // ---------- RN05: registrar resposta ----------

    @Test
    void registrarResposta_somaNoRegistroExistente() {
        Questao questao = new Questao("Quanto é 1/2 + 1/4?", null, fracoes, NivelDificuldade.FACIL,
                AnoEscolar.SETIMO_ANO);
        DesempenhoTopico existente = registro(ana, fracoes, NivelDificuldade.FACIL, 2, 1);
        when(desempenhoTopicoRepository.buscar(20L, 5L, NivelDificuldade.FACIL)).thenReturn(Optional.of(existente));
        when(desempenhoTopicoRepository.save(any(DesempenhoTopico.class))).thenAnswer(inv -> inv.getArgument(0));

        DesempenhoTopico atualizado = service.registrarResposta(ana, questao, true);

        assertSame(existente, atualizado);
        assertEquals(3, atualizado.getTotalRespostas());
        assertEquals(2, atualizado.getTotalAcertos());
        assertEquals(new BigDecimal("66.67"), atualizado.getPercentualAcerto());
    }

    @Test
    void registrarResposta_criaORegistroNaPrimeiraRespostaDoTopicoENivel() {
        Questao questao = new Questao("Quanto é 2³?", null, potencias, NivelDificuldade.MEDIO, AnoEscolar.SETIMO_ANO);
        when(desempenhoTopicoRepository.buscar(20L, 6L, NivelDificuldade.MEDIO)).thenReturn(Optional.empty());
        when(desempenhoTopicoRepository.save(any(DesempenhoTopico.class))).thenAnswer(inv -> inv.getArgument(0));

        DesempenhoTopico novo = service.registrarResposta(ana, questao, false);

        assertSame(ana, novo.getAluno());
        assertSame(potencias, novo.getTopico());
        assertEquals(NivelDificuldade.MEDIO, novo.getNivelDificuldade());
        assertEquals(1, novo.getTotalRespostas());
        assertEquals(0, novo.getTotalAcertos());
    }

    // ---------- RF19: painel do aluno ----------

    @Test
    void consultarDoAluno_somaOsNiveisOrdenaOsTopicosEMarcaODominio() {
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(ana));
        when(desempenhoTopicoRepository.findByAlunoId(20L)).thenReturn(List.of(
                registro(ana, fracoes, NivelDificuldade.FACIL, 6, 5),     // 83,33% com 6 respostas: dominado
                registro(ana, fracoes, NivelDificuldade.MEDIO, 2, 0),
                registro(ana, potencias, NivelDificuldade.FACIL, 4, 4))); // 100%, mas só 4 respostas
        when(respostaRepository.findHistoricoDoAluno(20L)).thenReturn(List.of(
                new RespostaHistorico(LocalDateTime.of(2026, 11, 2, 10, 0), true),
                new RespostaHistorico(LocalDateTime.of(2026, 11, 4, 10, 0), false)));

        DesempenhoAlunoResponse painel = service.consultarDoAluno(20L);

        assertEquals(20L, painel.alunoId());
        assertEquals("Ana", painel.nome());
        assertEquals(12, painel.totalRespostas());
        assertEquals(9, painel.totalAcertos());
        assertEquals(new BigDecimal("75.00"), painel.percentualGeral());

        // do menor para o maior percentual: Frações (5 de 8) antes de Potências (4 de 4)
        assertEquals(2, painel.topicos().size());
        DesempenhoAlunoResponse.TopicoDesempenho primeiro = painel.topicos().get(0);
        assertEquals("Frações", primeiro.topico());
        assertEquals(8, primeiro.totalRespostas());
        assertEquals(new BigDecimal("62.50"), primeiro.percentual());
        assertEquals(2, primeiro.niveis().size());
        assertEquals("FACIL", primeiro.niveis().get(0).nivelDificuldade());
        assertTrue(primeiro.niveis().get(0).dominado());
        assertFalse(primeiro.niveis().get(1).dominado());
        DesempenhoAlunoResponse.TopicoDesempenho segundo = painel.topicos().get(1);
        assertEquals("Potências", segundo.topico());
        assertFalse(segundo.niveis().get(0).dominado(), "100% com poucas respostas ainda não é domínio");

        assertEquals(1, painel.evolucao().size());
        assertEquals(2, painel.evolucao().get(0).respostas());
    }

    @Test
    void consultarDoAluno_inexistente_recusa() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.consultarDoAluno(99L));
    }

    @Test
    void consultarAlunoDaTurma_alunoDeOutraTurma_recusa() {
        when(alunoTurmaRepository.existsByAlunoIdAndTurmaId(20L, TURMA_ID)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.consultarAlunoDaTurma(professor, TURMA_ID, 20L));

        verify(acessoTurma).buscarComAcesso(professor, TURMA_ID);
        verifyNoInteractions(usuarioRepository, desempenhoTopicoRepository);
    }

    @Test
    void consultarAlunoDaTurma_alunoDaTurmaSemRespostas_mostraPainelZerado() {
        when(alunoTurmaRepository.existsByAlunoIdAndTurmaId(20L, TURMA_ID)).thenReturn(true);
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(ana));
        when(desempenhoTopicoRepository.findByAlunoId(20L)).thenReturn(List.of());
        when(respostaRepository.findHistoricoDoAluno(20L)).thenReturn(List.of());

        DesempenhoAlunoResponse painel = service.consultarAlunoDaTurma(professor, TURMA_ID, 20L);

        assertEquals(20L, painel.alunoId());
        assertEquals(0, painel.totalRespostas());
        assertEquals(new BigDecimal("0.00"), painel.percentualGeral());
        assertTrue(painel.topicos().isEmpty());
        assertTrue(painel.evolucao().isEmpty());
    }

    // ---------- RF19: visão da turma ----------

    @Test
    void consultarTurma_resumePorAlunoPorTopicoEMontaOMapaDeCalor() {
        Set<Long> alunosDaTurma = Set.of(20L, 21L, 22L);
        when(acessoTurma.buscarComAcesso(professor, TURMA_ID)).thenReturn(turma);
        when(alunoTurmaRepository.findByTurmaIdComAluno(TURMA_ID)).thenReturn(List.of(
                new AlunoTurma(ana, turma), new AlunoTurma(bia, turma), new AlunoTurma(caio, turma)));
        when(desempenhoTopicoRepository.findByAlunoIdIn(alunosDaTurma)).thenReturn(List.of(
                registro(ana, fracoes, NivelDificuldade.FACIL, 4, 2),
                registro(ana, potencias, NivelDificuldade.FACIL, 2, 2),
                registro(bia, fracoes, NivelDificuldade.FACIL, 6, 6)));
        when(respostaRepository.findHistoricoDosAlunos(alunosDaTurma)).thenReturn(List.of(
                new Projecoes.RespostaDoAluno(20L, LocalDateTime.of(2026, 11, 2, 10, 0), true),
                new Projecoes.RespostaDoAluno(21L, LocalDateTime.of(2026, 11, 3, 9, 0), false)));
        when(respostaRepository.findPorTopicoDosAlunos(alunosDaTurma)).thenReturn(List.of());

        DesempenhoTurmaResponse visao = service.consultarTurma(professor, TURMA_ID);

        assertEquals(TURMA_ID, visao.turmaId());
        assertEquals("7º ano A", visao.turmaNome());
        assertEquals("SETIMO_ANO", visao.anoEscolar());

        // alunos na ordem da turma, com o tópico mais fraco de cada um
        assertEquals(3, visao.alunos().size());
        DesempenhoTurmaResponse.AlunoResumo resumoAna = visao.alunos().get(0);
        assertEquals("Ana", resumoAna.nome());
        assertEquals(6, resumoAna.totalRespostas());
        assertEquals(4, resumoAna.totalAcertos());
        assertEquals(new BigDecimal("66.67"), resumoAna.percentual());
        assertEquals("Frações", resumoAna.topicoMaisFraco());
        assertEquals(new BigDecimal("50.00"), resumoAna.percentualTopicoMaisFraco());
        DesempenhoTurmaResponse.AlunoResumo resumoCaio = visao.alunos().get(2);
        assertEquals(0, resumoCaio.totalRespostas());
        assertNull(resumoCaio.topicoMaisFraco(), "quem não respondeu nada não tem tópico mais fraco");

        // tópicos da turma do menor para o maior percentual
        assertEquals(2, visao.topicos().size());
        DesempenhoTurmaResponse.TopicoTurma topicoFracoes = visao.topicos().get(0);
        assertEquals("Frações", topicoFracoes.topico());
        assertEquals(10, topicoFracoes.totalRespostas());
        assertEquals(8, topicoFracoes.totalAcertos());
        assertEquals(new BigDecimal("80.00"), topicoFracoes.percentual());
        assertEquals(2, topicoFracoes.alunosComRespostas());
        assertEquals("Potências", visao.topicos().get(1).topico());

        // mapa de calor: uma célula por aluno e tópico com respostas
        assertEquals(3, visao.celulas().size());

        assertEquals(1, visao.evolucao().size());
        assertEquals(2, visao.evolucao().get(0).respostas());
        assertEquals(2, visao.evolucao().get(0).alunosAtivos());
        assertTrue(visao.efeitoPratica().isEmpty());
    }

    @Test
    void consultarTurma_semAlunos_naoConsultaDesempenhoNemHistorico() {
        when(acessoTurma.buscarComAcesso(professor, TURMA_ID)).thenReturn(turma);
        when(alunoTurmaRepository.findByTurmaIdComAluno(TURMA_ID)).thenReturn(List.of());

        DesempenhoTurmaResponse visao = service.consultarTurma(professor, TURMA_ID);

        assertTrue(visao.alunos().isEmpty());
        assertTrue(visao.topicos().isEmpty());
        assertTrue(visao.evolucao().isEmpty());
        verifyNoInteractions(desempenhoTopicoRepository, respostaRepository);
    }
}
