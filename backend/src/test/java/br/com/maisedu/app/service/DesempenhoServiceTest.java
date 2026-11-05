package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.DesempenhoTurmaResponse;
import br.com.maisedu.app.dto.RespostaHistorico;
import br.com.maisedu.app.model.AlunoTurma;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.DesempenhoTopico;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Topico;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlunoTurmaRepository;
import br.com.maisedu.app.repository.AtividadeRepository;
import br.com.maisedu.app.repository.DesempenhoTopicoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RF19: painel da turma (indicadores, tópicos por nível e evolução semanal). */
@ExtendWith(MockitoExtension.class)
class DesempenhoServiceTest {

    private static final Long TURMA_ID = 5L;
    private static final Long INSTITUICAO_ID = 1L;

    @Mock DesempenhoTopicoRepository desempenhoTopicoRepository;
    @Mock RespostaRepository respostaRepository;
    @Mock AlunoTurmaRepository alunoTurmaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock AtividadeRepository atividadeRepository;
    @Mock AcessoTurma acessoTurma;

    private DesempenhoService service;
    private UsuarioAutenticado professor;
    private Instituicao escola;
    private Turma turma;

    @BeforeEach
    void setUp() {
        service = new DesempenhoService(desempenhoTopicoRepository, respostaRepository, alunoTurmaRepository,
                usuarioRepository, atividadeRepository, acessoTurma, 5, BigDecimal.valueOf(70));

        escola = new Instituicao("Escola", "00.000.000/0001-00", "Rua 1");
        ReflectionTestUtils.setField(escola, "id", INSTITUICAO_ID);
        turma = new Turma("7º ano A", escola, AnoEscolar.SETIMO_ANO);
        ReflectionTestUtils.setField(turma, "id", TURMA_ID);
        professor = new UsuarioAutenticado(7L, "Paulo", INSTITUICAO_ID, EnumSet.of(RoleNome.PROFESSOR), false);
    }

    @Test
    void rf19_painelDaTurmaSomaOsAlunosPorTopicoENivelEMostraAEvolucao() {
        Usuario ana = aluno(10L, "Ana");
        Usuario bruno = aluno(11L, "Bruno");
        Topico fracoes = topico(3L, "Frações");
        Topico porcentagem = topico(6L, "Porcentagem");

        when(acessoTurma.buscarComAcesso(professor, TURMA_ID)).thenReturn(turma);
        when(alunoTurmaRepository.findByTurmaIdComAluno(TURMA_ID))
                .thenReturn(List.of(new AlunoTurma(ana, turma), new AlunoTurma(bruno, turma)));
        when(desempenhoTopicoRepository.findByAlunoIdIn(any())).thenReturn(List.of(
                desempenho(ana, fracoes, NivelDificuldade.FACIL, 10, 4),
                desempenho(ana, fracoes, NivelDificuldade.MEDIO, 2, 1),
                desempenho(ana, porcentagem, NivelDificuldade.FACIL, 10, 8)));
        LocalDateTime segunda = LocalDateTime.of(2026, 11, 2, 10, 0);
        when(respostaRepository.findHistoricoDosAlunos(any())).thenReturn(List.of(
                new RespostaHistorico(segunda, true),
                new RespostaHistorico(segunda.plusDays(1), false),
                new RespostaHistorico(segunda.plusDays(7), true)));
        when(atividadeRepository.contarDaTurma(TURMA_ID)).thenReturn(3L);

        DesempenhoTurmaResponse painel = service.consultarTurma(professor, TURMA_ID);

        // Big numbers: 2 alunos, só a Ana respondeu; 22 respostas e 13 acertos no total
        DesempenhoTurmaResponse.ResumoTurma resumo = painel.resumo();
        assertEquals(2, resumo.totalAlunos());
        assertEquals(1, resumo.alunosComRespostas());
        assertEquals(3L, resumo.totalAtividades());
        assertEquals(22, resumo.totalRespostas());
        assertEquals(13, resumo.totalAcertos());
        assertEquals(new BigDecimal("59.09"), resumo.percentual());

        // Tópicos do mais fraco para o mais forte, com o detalhe por nível na ordem FÁCIL, MÉDIO, DIFÍCIL
        assertEquals(2, painel.topicos().size());
        DesempenhoTurmaResponse.TopicoTurma maisFraco = painel.topicos().get(0);
        assertEquals("Frações", maisFraco.topico());
        assertEquals(12, maisFraco.totalRespostas());
        assertEquals(5, maisFraco.totalAcertos());
        assertEquals(new BigDecimal("41.67"), maisFraco.percentual());
        assertEquals(1, maisFraco.alunosComRespostas());
        assertEquals(2, maisFraco.niveis().size());
        assertEquals("FACIL", maisFraco.niveis().get(0).nivelDificuldade());
        assertEquals(new BigDecimal("40.00"), maisFraco.niveis().get(0).percentual());
        assertEquals("MEDIO", maisFraco.niveis().get(1).nivelDificuldade());
        assertEquals(new BigDecimal("50.00"), maisFraco.niveis().get(1).percentual());
        assertEquals("Porcentagem", painel.topicos().get(1).topico());

        // Alunos: o que ainda não respondeu aparece zerado, sem tópico mais fraco
        DesempenhoTurmaResponse.AlunoResumo resumoBruno = painel.alunos().get(1);
        assertEquals("Bruno", resumoBruno.nome());
        assertEquals(0, resumoBruno.totalRespostas());
        assertNull(resumoBruno.topicoMaisFraco());

        // Evolução: duas semanas, a primeira com 1 acerto em 2 respostas
        assertEquals(2, painel.evolucao().size());
        assertEquals(2, painel.evolucao().get(0).respostas());
        assertEquals(new BigDecimal("50.00"), painel.evolucao().get(0).percentualSemana());
        assertEquals(new BigDecimal("66.67"), painel.evolucao().get(1).percentualAcumulado());
    }

    @Test
    void rf19_turmaSemAlunosTemPainelZeradoSemConsultarRespostas() {
        when(acessoTurma.buscarComAcesso(professor, TURMA_ID)).thenReturn(turma);
        when(alunoTurmaRepository.findByTurmaIdComAluno(TURMA_ID)).thenReturn(List.of());
        when(atividadeRepository.contarDaTurma(TURMA_ID)).thenReturn(0L);

        DesempenhoTurmaResponse painel = service.consultarTurma(professor, TURMA_ID);

        assertEquals(0, painel.resumo().totalAlunos());
        assertEquals(0, painel.resumo().totalRespostas());
        assertEquals(new BigDecimal("0.00"), painel.resumo().percentual());
        assertTrue(painel.alunos().isEmpty());
        assertTrue(painel.topicos().isEmpty());
        assertTrue(painel.evolucao().isEmpty());
        verify(acessoTurma).buscarComAcesso(professor, TURMA_ID);
        verifyNoInteractions(desempenhoTopicoRepository, respostaRepository);
    }

    private Usuario aluno(Long id, String nome) {
        Usuario aluno = Usuario.novoAluno(nome, nome.toLowerCase(), "hash", escola);
        ReflectionTestUtils.setField(aluno, "id", id);
        return aluno;
    }

    private static Topico topico(Long id, String nome) {
        Topico topico = new Topico(nome);
        ReflectionTestUtils.setField(topico, "id", id);
        return topico;
    }

    private static DesempenhoTopico desempenho(Usuario aluno, Topico topico, NivelDificuldade nivel, int respostas,
                                               int acertos) {
        DesempenhoTopico registro = new DesempenhoTopico(aluno, topico, nivel);
        for (int i = 0; i < respostas; i++) {
            registro.registrarResposta(i < acertos);
        }
        return registro;
    }
}
