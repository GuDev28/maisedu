package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.PainelEscolaResponse;
import br.com.maisedu.app.dto.PainelInstituicaoResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.StatusQuestao;
import br.com.maisedu.app.model.TipoAtividade;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AtividadeAlunoRepository;
import br.com.maisedu.app.repository.AtividadeRepository;
import br.com.maisedu.app.repository.AvaliacaoQuestaoRepository;
import br.com.maisedu.app.repository.DesempenhoTopicoRepository;
import br.com.maisedu.app.repository.InstituicaoRepository;
import br.com.maisedu.app.repository.LogAuditoriaRepository;
import br.com.maisedu.app.repository.Projecoes;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.TurmaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Página Início do admin e painel da escola: contagens, minimização (grupos pequenos sem %) e ordenação. */
@ExtendWith(MockitoExtension.class)
class PainelServiceTest {

    private static final Long INSTITUICAO_ID = 1L;

    @Mock InstituicaoRepository instituicaoRepository;
    @Mock TurmaRepository turmaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock LogAuditoriaRepository logAuditoriaRepository;
    @Mock QuestaoRepository questaoRepository;
    @Mock DesempenhoTopicoRepository desempenhoTopicoRepository;
    @Mock AtividadeRepository atividadeRepository;
    @Mock AtividadeAlunoRepository atividadeAlunoRepository;
    @Mock RespostaRepository respostaRepository;
    @Mock AvaliacaoQuestaoRepository avaliacaoQuestaoRepository;
    @InjectMocks PainelService service;

    private Instituicao escola;
    private UsuarioAutenticado admin;
    private UsuarioAutenticado professor;

    @BeforeEach
    void setUp() {
        escola = new Instituicao("Escola Modelo", "00.000.000/0001-00", "Rua 1");
        ReflectionTestUtils.setField(escola, "id", INSTITUICAO_ID);
        admin = new UsuarioAutenticado(1L, "Admin", INSTITUICAO_ID, EnumSet.of(RoleNome.ADMIN), false);
        professor = new UsuarioAutenticado(7L, "Paulo", INSTITUICAO_ID, EnumSet.of(RoleNome.PROFESSOR), false);
    }

    private Usuario usuario(String nome, RoleNome papel) {
        Usuario usuario = new Usuario(nome, nome.toLowerCase() + "@escola.test", "hash", escola);
        usuario.adicionarRole(new Role(papel));
        return usuario;
    }

    private static Turma turma(String nome, Instituicao escola, boolean ativa) {
        Turma turma = new Turma(nome, escola, AnoEscolar.SETIMO_ANO);
        turma.setAtivo(ativa);
        return turma;
    }

    // ---------- Página Início ----------

    @Test
    void resumo_soParaOAdministrador() {
        assertThrows(AccessDeniedException.class, () -> service.resumoDaInstituicao(professor));
        assertThrows(AccessDeniedException.class, () -> service.desempenhoDaEscola(professor));
        verifyNoInteractions(instituicaoRepository);
    }

    @Test
    void resumo_comInstituicaoInexistente_recusa() {
        when(instituicaoRepository.findById(INSTITUICAO_ID)).thenReturn(Optional.empty());

        assertThrows(NegocioException.class, () -> service.resumoDaInstituicao(admin));
    }

    @Test
    void resumo_contaTurmasEContasPorSituacao() {
        when(instituicaoRepository.findById(INSTITUICAO_ID)).thenReturn(Optional.of(escola));
        when(turmaRepository.findByInstituicaoIdOrderByAtivoDescNomeAsc(INSTITUICAO_ID)).thenReturn(List.of(
                turma("7º A", escola, true), turma("7º B", escola, true), turma("6º C", escola, false)));

        Usuario professorNovo = usuario("Paulo", RoleNome.PROFESSOR);          // senha provisória ainda
        Usuario alunoAtivo = usuario("Ana", RoleNome.ALUNO);
        alunoAtivo.trocarSenha("hash-novo");
        Usuario alunoBloqueado = usuario("Bia", RoleNome.ALUNO);
        alunoBloqueado.trocarSenha("hash-novo");
        alunoBloqueado.registrarTentativaFalha(1, 30);
        Usuario desativado = usuario("Caio", RoleNome.PROFESSOR);
        desativado.desativar();
        Usuario anonimizado = usuario("Davi", RoleNome.ALUNO);
        anonimizado.anonimizar("hash-aleatorio");
        when(usuarioRepository.findDaInstituicao(INSTITUICAO_ID, null)).thenReturn(List.of(
                professorNovo, alunoAtivo, alunoBloqueado, desativado, anonimizado));

        when(logAuditoriaRepository.countByInstituicaoIdAndAcaoAndSucessoTrueAndCriadoEmAfter(
                eq(INSTITUICAO_ID), eq(AcaoAuditoria.LOGIN), any(LocalDateTime.class))).thenReturn(12L);
        when(questaoRepository.countByStatus(StatusQuestao.APROVADA)).thenReturn(40L);

        PainelInstituicaoResponse resumo = service.resumoDaInstituicao(admin);

        assertEquals("Escola Modelo", resumo.instituicao());
        assertEquals(2, resumo.turmasAtivas());
        assertEquals(1, resumo.turmasInativas());
        assertEquals(1, resumo.professores());
        assertEquals(2, resumo.alunos());
        assertEquals(1, resumo.contasBloqueadas());
        assertEquals(1, resumo.primeiroAcessoPendente());
        // o anonimizado não entra em nenhuma contagem, nem como desativado
        assertEquals(1, resumo.contasDesativadas());
        assertEquals(12, resumo.loginsUltimos7Dias());
        assertEquals(40, resumo.questoesNoBancoCompartilhado());
    }

    // ---------- Painel da escola ----------

    @Test
    void painelDaEscola_comInstituicaoInexistente_recusa() {
        when(instituicaoRepository.findById(INSTITUICAO_ID)).thenReturn(Optional.empty());

        assertThrows(NegocioException.class, () -> service.desempenhoDaEscola(admin));
    }

    @Test
    void painelDaEscola_agregaMapaTurmasEngajamentoEBanco() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDate segundaAtual = agora.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        when(instituicaoRepository.findById(INSTITUICAO_ID)).thenReturn(Optional.of(escola));

        // mapa ano x tópico: grupos com menos de 3 alunos ficam sem percentual
        when(desempenhoTopicoRepository.somarPorAnoETopico(INSTITUICAO_ID)).thenReturn(List.of(
                new Projecoes.DesempenhoAnoTopico(AnoEscolar.OITAVO_ANO, 5L, "Fracoes", 20L, 15L, 4L),
                new Projecoes.DesempenhoAnoTopico(AnoEscolar.SEXTO_ANO, 6L, "Potencias", 10L, 5L, 2L),
                new Projecoes.DesempenhoAnoTopico(AnoEscolar.NONO_ANO, 7L, "Geometria", 10L, null, 3L),
                new Projecoes.DesempenhoAnoTopico(AnoEscolar.SEXTO_ANO, 8L, "Medidas", null, null, 5L)));

        // comparativo entre turmas
        when(respostaRepository.contarAlunosAtivosPorTurma(eq(INSTITUICAO_ID), any(LocalDateTime.class)))
                .thenReturn(List.of(new Projecoes.ContagemPorId(100L, 3L)));
        when(atividadeRepository.contarPorTurma(INSTITUICAO_ID))
                .thenReturn(List.of(new Projecoes.ContagemPorId(100L, 2L)));
        when(desempenhoTopicoRepository.somarPorTurma(INSTITUICAO_ID)).thenReturn(List.of(
                new Projecoes.DesempenhoDaTurma(100L, "7A", AnoEscolar.SETIMO_ANO, 30L, 4L, 40L, 30L),
                new Projecoes.DesempenhoDaTurma(200L, "6B", AnoEscolar.SEXTO_ANO, 25L, null, null, null)));

        // engajamento: duas respostas de um aluno e uma de outro nesta semana, uma há 3 semanas e uma
        // antiga demais (fora da janela de 8 semanas)
        when(respostaRepository.findDaInstituicaoDesde(eq(INSTITUICAO_ID), any(LocalDateTime.class))).thenReturn(List.of(
                new Projecoes.RespostaDoAluno(10L, agora, true),
                new Projecoes.RespostaDoAluno(10L, agora, false),
                new Projecoes.RespostaDoAluno(11L, agora, true),
                new Projecoes.RespostaDoAluno(10L, agora.minusWeeks(3), true),
                new Projecoes.RespostaDoAluno(12L, agora.minusWeeks(20), true)));
        when(atividadeRepository.findDatasDeCriacao(eq(INSTITUICAO_ID), any(LocalDateTime.class)))
                .thenReturn(List.of(agora, agora.minusWeeks(30)));
        when(atividadeAlunoRepository.findDatasDeInicio(eq(INSTITUICAO_ID), eq(TipoAtividade.ADAPTATIVA),
                any(LocalDateTime.class))).thenReturn(List.of(agora.minusWeeks(1)));

        // banco compartilhado
        when(questaoRepository.contarDaInstituicaoPorStatus(INSTITUICAO_ID)).thenReturn(List.of(
                new Projecoes.ContagemStatusQuestao(StatusQuestao.RASCUNHO, 2L),
                new Projecoes.ContagemStatusQuestao(StatusQuestao.PENDENTE, 3L),
                new Projecoes.ContagemStatusQuestao(StatusQuestao.APROVADA, 10L),
                new Projecoes.ContagemStatusQuestao(StatusQuestao.REJEITADA, 1L)));
        when(avaliacaoQuestaoRepository.contarVotosDaInstituicao(INSTITUICAO_ID)).thenReturn(25L);
        when(atividadeRepository.contarUsosPorOutrasEscolas(INSTITUICAO_ID)).thenReturn(4L);
        when(atividadeRepository.contarUsosNaPropriaEscola(INSTITUICAO_ID)).thenReturn(9L);
        when(atividadeRepository.contarQuestoesDeOutrasEscolasUsadas(INSTITUICAO_ID)).thenReturn(6L);

        PainelEscolaResponse painel = service.desempenhoDaEscola(admin);

        assertEquals("Escola Modelo", painel.instituicao());
        assertEquals(PainelService.MINIMO_ALUNOS_POR_GRUPO, painel.minimoAlunosPorGrupo());

        // mapa na ordem do ano escolar (6º ao 9º) e, dentro do ano, pelo nome do tópico
        List<PainelEscolaResponse.CelulaAnoTopico> mapa = painel.mapaAnoTopico();
        assertEquals(4, mapa.size());
        assertEquals("Medidas", mapa.get(0).topico());
        assertNull(mapa.get(0).percentual(), "sem respostas não há percentual");
        assertEquals(0, mapa.get(0).respostas());
        assertEquals("Potencias", mapa.get(1).topico());
        assertNull(mapa.get(1).percentual(), "2 alunos é menos que o mínimo do grupo");
        assertEquals("Fracoes", mapa.get(2).topico());
        assertEquals("OITAVO_ANO", mapa.get(2).anoEscolar());
        assertEquals(new BigDecimal("75.00"), mapa.get(2).percentual());
        assertEquals("Geometria", mapa.get(3).topico());
        assertEquals(new BigDecimal("0.00"), mapa.get(3).percentual(), "acertos nulos contam como zero");

        // turmas na ordem do ano escolar
        List<PainelEscolaResponse.TurmaComparada> turmas = painel.turmas();
        assertEquals(2, turmas.size());
        PainelEscolaResponse.TurmaComparada sexto = turmas.get(0);
        assertEquals("6B", sexto.turma());
        assertEquals(0, sexto.alunosComRespostas());
        assertEquals(0, sexto.alunosAtivosRecentes());
        assertEquals(0, sexto.atividades());
        assertNull(sexto.percentual());
        PainelEscolaResponse.TurmaComparada setimo = turmas.get(1);
        assertEquals("7A", setimo.turma());
        assertEquals(30, setimo.alunos());
        assertEquals(4, setimo.alunosComRespostas());
        assertEquals(3, setimo.alunosAtivosRecentes());
        assertEquals(2, setimo.atividades());
        assertEquals(40, setimo.respostas());
        assertEquals(new BigDecimal("75.00"), setimo.percentual());

        // 8 semanas, da mais antiga para a atual, com zero nas semanas vazias
        List<PainelEscolaResponse.SemanaEngajamento> semanas = painel.engajamento();
        assertEquals(PainelService.SEMANAS_DE_ENGAJAMENTO, semanas.size());
        PainelEscolaResponse.SemanaEngajamento atual = semanas.get(semanas.size() - 1);
        assertEquals(segundaAtual, atual.semana());
        assertEquals(2, atual.alunosAtivos());
        assertEquals(3, atual.respostas());
        assertEquals(1, atual.atividadesCriadas());
        assertEquals(0, atual.praticasIniciadas());
        assertEquals(1, semanas.get(semanas.size() - 2).praticasIniciadas());
        assertEquals(1, semanas.get(semanas.size() - 4).respostas());
        assertEquals(0, semanas.get(0).respostas());

        PainelEscolaResponse.BancoCompartilhado banco = painel.banco();
        assertEquals(2, banco.rascunhos());
        assertEquals(3, banco.pendentes());
        assertEquals(10, banco.aprovadas());
        assertEquals(1, banco.rejeitadas());
        assertEquals(25, banco.votosDados());
        assertEquals(4, banco.usosPorOutrasEscolas());
        assertEquals(9, banco.usosNaPropriaEscola());
        assertEquals(6, banco.questoesDeOutrasEscolasUsadas());
    }
}
