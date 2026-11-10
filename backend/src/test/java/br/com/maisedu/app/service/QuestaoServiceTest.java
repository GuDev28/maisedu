package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.QuestaoResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.Alternativa;
import br.com.maisedu.app.model.AvaliacaoQuestao;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.Role;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.StatusQuestao;
import br.com.maisedu.app.model.Topico;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.model.VotoAvaliacao;
import br.com.maisedu.app.repository.AvaliacaoQuestaoRepository;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.TopicoRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Cadastro de questões do banco compartilhado: criação como rascunho com 5 alternativas (A a E),
 * edição e exclusão do rascunho pelo autor, envio para a validação colaborativa (RN2), placar
 * das questões do autor e fila de avaliação do professor.
 */
@ExtendWith(MockitoExtension.class)
class QuestaoServiceTest {

    private static final long AUTOR_ID = 1L;
    private static final long OUTRO_PROFESSOR_ID = 2L;
    private static final long TOPICO_ID = 5L;
    private static final long QUESTAO_ID = 50L;
    private static final String ENUNCIADO = "Quanto é 3/4 de 20?";
    private static final List<String> CINCO_ALTERNATIVAS = List.of("5", "10", "15", "20", "25");

    @Mock QuestaoRepository questaoRepository;
    @Mock TopicoRepository topicoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock AvaliacaoQuestaoRepository avaliacaoQuestaoRepository;
    @Mock AuditoriaService auditoriaService;
    @InjectMocks QuestaoService service;

    private Instituicao escola;
    private Usuario autor;
    private Topico fracoes;
    private UsuarioAutenticado atorAutor;
    private UsuarioAutenticado atorOutroProfessor;

    @BeforeEach
    void setUp() {
        escola = new Instituicao("Escola Teste", "00.000.000/0001-00", "Rua Teste, 1");
        ReflectionTestUtils.setField(escola, "id", 1L);

        autor = new Usuario("Paula Autora", "paula@escola.test", "hash", escola);
        ReflectionTestUtils.setField(autor, "id", AUTOR_ID);
        autor.adicionarRole(new Role(RoleNome.PROFESSOR));

        fracoes = new Topico("Frações");
        ReflectionTestUtils.setField(fracoes, "id", TOPICO_ID);

        atorAutor = new UsuarioAutenticado(AUTOR_ID, "Paula Autora", 1L, Set.of(RoleNome.PROFESSOR), false);
        atorOutroProfessor = new UsuarioAutenticado(OUTRO_PROFESSOR_ID, "Otávio", 1L, Set.of(RoleNome.PROFESSOR), false);
    }

    private void dadoQueAutorETopicoExistem() {
        when(usuarioRepository.findById(AUTOR_ID)).thenReturn(Optional.of(autor));
        when(topicoRepository.findById(TOPICO_ID)).thenReturn(Optional.of(fracoes));
    }

    private Questao rascunhoCompletoDoAutor(long id) {
        Questao questao = new Questao(ENUNCIADO, autor, fracoes, NivelDificuldade.MEDIO);
        ReflectionTestUtils.setField(questao, "id", id);
        questao.definirAlternativas(CINCO_ALTERNATIVAS, 2);
        return questao;
    }

    private Questao pendenteDoAutor(long id) {
        Questao questao = rascunhoCompletoDoAutor(id);
        questao.enviarParaValidacao();
        return questao;
    }

    /** Percorre as alternativas, garante que só uma está marcada como correta e a devolve. */
    private static Alternativa unicaAlternativaCorreta(Questao questao) {
        Alternativa correta = null;
        int quantidadeDeCorretas = 0;
        for (Alternativa alternativa : questao.getAlternativas()) {
            if (alternativa.isCorreta()) {
                correta = alternativa;
                quantidadeDeCorretas++;
            }
        }
        assertEquals(1, quantidadeDeCorretas, "a questão deve ter exatamente uma alternativa correta");
        return correta;
    }

    static List<List<String>> listasComQuantidadeInvalidaDeAlternativas() {
        return List.of(
                List.of(),                                    // nenhuma alternativa
                List.of("5", "10", "15", "20"),               // 4: uma a menos que o exigido
                List.of("5", "10", "15", "20", "25", "30"));  // 6: uma a mais que o exigido
    }

    @Test
    void deveCriarQuestaoComoRascunhoComCincoAlternativasEUmaCorreta() {
        // Arrange
        dadoQueAutorETopicoExistem();
        when(questaoRepository.save(any(Questao.class))).thenAnswer(invocacao -> {
            Questao salva = invocacao.getArgument(0);
            ReflectionTestUtils.setField(salva, "id", QUESTAO_ID);
            return salva;
        });

        // Act
        Questao questao = service.criar(atorAutor, ENUNCIADO, TOPICO_ID, NivelDificuldade.MEDIO, AnoEscolar.SEXTO_ANO,
                CINCO_ALTERNATIVAS, 2);

        // Assert
        assertEquals(StatusQuestao.RASCUNHO, questao.getStatus());
        assertSame(autor, questao.getAutor());
        assertSame(fracoes, questao.getTopico());
        assertEquals(5, questao.getAlternativas().size());
        Alternativa correta = unicaAlternativaCorreta(questao);
        assertEquals("15", correta.getTexto());
        assertEquals('C', correta.letra());
        verify(auditoriaService).registrar(atorAutor, AcaoAuditoria.CRIACAO_QUESTAO, "Questao", QUESTAO_ID, true, null);
    }

    @Test
    void deveLancarExcecaoQuandoQuemCriaAQuestaoNaoEProfessor() {
        // Arrange
        UsuarioAutenticado aluno = new UsuarioAutenticado(9L, "Ana Aluna", 1L, Set.of(RoleNome.ALUNO), false);

        // Act
        NegocioException ex = assertThrows(NegocioException.class, () -> service.criar(aluno, ENUNCIADO,
                TOPICO_ID, NivelDificuldade.FACIL, AnoEscolar.SEXTO_ANO, CINCO_ALTERNATIVAS, 2));

        // Assert
        assertEquals("Usuário informado não possui papel de professor", ex.getMessage());
        verifyNoInteractions(questaoRepository, usuarioRepository, topicoRepository, auditoriaService);
    }

    @ParameterizedTest(name = "{index}: alternativas = {0}")
    @MethodSource("listasComQuantidadeInvalidaDeAlternativas")
    void deveRecusarQuestaoQueNaoTenhaExatamenteCincoAlternativas(List<String> alternativas) {
        // Arrange
        dadoQueAutorETopicoExistem();

        // Act
        NegocioException ex = assertThrows(NegocioException.class, () -> service.criar(atorAutor, ENUNCIADO,
                TOPICO_ID, NivelDificuldade.MEDIO, AnoEscolar.SEXTO_ANO, alternativas, 0));

        // Assert
        assertEquals("A questão deve ter exatamente 5 alternativas", ex.getMessage());
        verify(questaoRepository, never()).save(any());
    }

    @ParameterizedTest(name = "índice {0} marca a alternativa {1} (\"{2}\") como correta")
    @CsvSource({
            "0, A, 5",   // primeira alternativa
            "4, E, 25"   // última alternativa
    })
    void deveAceitarAlternativaCorretaNasExtremidadesDaLista(int indiceCorreta, char letraEsperada,
                                                             String textoEsperado) {
        // Arrange
        dadoQueAutorETopicoExistem();
        when(questaoRepository.save(any(Questao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        // Act
        Questao questao = service.criar(atorAutor, ENUNCIADO, TOPICO_ID, NivelDificuldade.MEDIO, AnoEscolar.SEXTO_ANO,
                CINCO_ALTERNATIVAS, indiceCorreta);

        // Assert
        Alternativa correta = unicaAlternativaCorreta(questao);
        assertEquals(letraEsperada, correta.letra());
        assertEquals(textoEsperado, correta.getTexto());
    }

    @ParameterizedTest(name = "índice {0} está fora das 5 alternativas")
    @ValueSource(ints = {-1, 5})
    void deveRecusarIndiceDaAlternativaCorretaForaDoIntervalo(int indiceCorreta) {
        // Arrange
        dadoQueAutorETopicoExistem();

        // Act
        NegocioException ex = assertThrows(NegocioException.class, () -> service.criar(atorAutor, ENUNCIADO,
                TOPICO_ID, NivelDificuldade.MEDIO, AnoEscolar.SEXTO_ANO, CINCO_ALTERNATIVAS, indiceCorreta));

        // Assert
        assertEquals("Indique qual alternativa é a correta", ex.getMessage());
        verify(questaoRepository, never()).save(any());
    }

    @Test
    void deveEnviarQuestaoParaValidacaoQuandoSolicitadoPeloAutor() {
        // Arrange
        Questao rascunho = rascunhoCompletoDoAutor(QUESTAO_ID);
        when(questaoRepository.findById(QUESTAO_ID)).thenReturn(Optional.of(rascunho));

        // Act
        Questao enviada = service.enviarParaValidacao(atorAutor, QUESTAO_ID);

        // Assert
        assertEquals(StatusQuestao.PENDENTE, enviada.getStatus());
        verify(auditoriaService).registrar(atorAutor, AcaoAuditoria.ENVIO_VALIDACAO_QUESTAO, "Questao", QUESTAO_ID,
                true, null);
    }

    @Test
    void deveNegarEnvioParaValidacaoQuandoQuemPedeNaoEOAutor() {
        // Arrange
        Questao rascunho = rascunhoCompletoDoAutor(QUESTAO_ID);
        when(questaoRepository.findById(QUESTAO_ID)).thenReturn(Optional.of(rascunho));

        // Act
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.enviarParaValidacao(atorOutroProfessor, QUESTAO_ID));

        // Assert
        assertEquals("Só o autor pode enviar a questão para validação", ex.getMessage());
        assertEquals(StatusQuestao.RASCUNHO, rascunho.getStatus());
        verifyNoInteractions(auditoriaService);
    }

    @Test
    void deveEditarORascunhoQuandoSolicitadoPeloAutor() {
        // Arrange
        Questao rascunho = rascunhoCompletoDoAutor(QUESTAO_ID);
        Topico porcentagem = new Topico("Porcentagem");
        ReflectionTestUtils.setField(porcentagem, "id", 6L);
        when(questaoRepository.findById(QUESTAO_ID)).thenReturn(Optional.of(rascunho));
        when(topicoRepository.findById(6L)).thenReturn(Optional.of(porcentagem));

        // Act
        Questao editada = service.editar(atorAutor, QUESTAO_ID, "  Quanto é 10% de 50?  ", 6L, NivelDificuldade.FACIL,
                AnoEscolar.SETIMO_ANO, List.of("1", "5", "10", "15", "50"), 1);

        // Assert
        assertEquals("Quanto é 10% de 50?", editada.getEnunciado());
        assertSame(porcentagem, editada.getTopico());
        assertEquals(NivelDificuldade.FACIL, editada.getNivelDificuldade());
        assertEquals(AnoEscolar.SETIMO_ANO, editada.getAnoEscolar());
        assertEquals("5", unicaAlternativaCorreta(editada).getTexto());
        assertEquals(StatusQuestao.RASCUNHO, editada.getStatus());
        verify(auditoriaService).registrar(atorAutor, AcaoAuditoria.EDICAO_QUESTAO, "Questao", QUESTAO_ID, true, null);
    }

    @Test
    void deveRecusarEdicaoDeQuestaoJaEnviadaParaValidacao() {
        // Arrange
        Questao pendente = pendenteDoAutor(QUESTAO_ID);
        when(questaoRepository.findById(QUESTAO_ID)).thenReturn(Optional.of(pendente));
        when(topicoRepository.findById(TOPICO_ID)).thenReturn(Optional.of(fracoes));

        // Act
        NegocioException ex = assertThrows(NegocioException.class, () -> service.editar(atorAutor, QUESTAO_ID,
                "Outro enunciado", TOPICO_ID, NivelDificuldade.MEDIO, AnoEscolar.SEXTO_ANO, CINCO_ALTERNATIVAS, 0));

        // Assert
        assertEquals("Só é possível editar a questão enquanto ela é um rascunho", ex.getMessage());
        assertEquals(ENUNCIADO, pendente.getEnunciado());
        verifyNoInteractions(auditoriaService);
    }

    @Test
    void deveNegarEdicaoQuandoQuemPedeNaoEOAutor() {
        // Arrange
        when(questaoRepository.findById(QUESTAO_ID)).thenReturn(Optional.of(rascunhoCompletoDoAutor(QUESTAO_ID)));

        // Act
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> service.editar(atorOutroProfessor,
                QUESTAO_ID, "Outro", TOPICO_ID, NivelDificuldade.MEDIO, AnoEscolar.SEXTO_ANO, CINCO_ALTERNATIVAS, 0));

        // Assert
        assertEquals("Só o autor pode editar a questão", ex.getMessage());
        verifyNoInteractions(topicoRepository, auditoriaService);
    }

    @Test
    void deveExcluirORascunhoDoAutor() {
        // Arrange
        Questao rascunho = rascunhoCompletoDoAutor(QUESTAO_ID);
        when(questaoRepository.findById(QUESTAO_ID)).thenReturn(Optional.of(rascunho));

        // Act
        service.excluir(atorAutor, QUESTAO_ID);

        // Assert
        verify(questaoRepository).delete(rascunho);
        verify(auditoriaService).registrar(atorAutor, AcaoAuditoria.EXCLUSAO_QUESTAO, "Questao", QUESTAO_ID, true, null);
    }

    @Test
    void deveRecusarExclusaoDeQuestaoJaEnviada() {
        // Arrange
        Questao pendente = pendenteDoAutor(QUESTAO_ID);
        when(questaoRepository.findById(QUESTAO_ID)).thenReturn(Optional.of(pendente));

        // Act
        NegocioException ex = assertThrows(NegocioException.class, () -> service.excluir(atorAutor, QUESTAO_ID));

        // Assert
        assertEquals("Só é possível excluir um rascunho; a questão já foi enviada para validação", ex.getMessage());
        verify(questaoRepository, never()).delete(any());
        verifyNoInteractions(auditoriaService);
    }

    @Test
    void deveListarAsQuestoesDoAutorComPlacarEMotivosDaRejeicao() {
        // Arrange
        Questao pendente = pendenteDoAutor(60L);
        Questao rascunho = rascunhoCompletoDoAutor(61L);
        Usuario avaliador = new Usuario("Bruno", "bruno@escola.test", "hash", escola);
        when(questaoRepository.findByAutorIdOrderByCriadoEmDesc(AUTOR_ID)).thenReturn(List.of(pendente, rascunho));
        when(avaliacaoQuestaoRepository.findByQuestaoIdIn(List.of(60L, 61L))).thenReturn(List.of(
                new AvaliacaoQuestao(pendente, avaliador, VotoAvaliacao.APROVAR),
                new AvaliacaoQuestao(pendente, avaliador, VotoAvaliacao.APROVAR),
                new AvaliacaoQuestao(pendente, avaliador, VotoAvaliacao.REJEITAR, "Gabarito errado")));

        // Act
        List<QuestaoResponse> minhas = service.listarMinhas(atorAutor);

        // Assert
        assertEquals(2, minhas.size());
        QuestaoResponse primeira = minhas.get(0);
        assertEquals(60L, primeira.id());
        assertEquals(2L, primeira.aprovacoes());
        assertEquals(1L, primeira.rejeicoes());
        assertEquals(List.of("Gabarito errado"), primeira.motivosRejeicao());
        assertEquals("Escola Teste", primeira.autorInstituicao());
        QuestaoResponse segunda = minhas.get(1);
        assertEquals(0L, segunda.aprovacoes());
        assertEquals(0L, segunda.rejeicoes());
        assertTrue(segunda.motivosRejeicao().isEmpty());
    }

    @Test
    void deveMontarFilaDeAvaliacaoComAConsultaQueJaExcluiOsVotosDoProfessor() {
        // Arrange
        Questao aindaNaoVotada = pendenteDoAutor(61L);
        when(questaoRepository.findPendentesParaAvaliar(StatusQuestao.PENDENTE, OUTRO_PROFESSOR_ID))
                .thenReturn(List.of(aindaNaoVotada));

        // Act
        List<Questao> fila = service.listarPendentesParaAvaliar(atorOutroProfessor);

        // Assert
        assertEquals(List.of(aindaNaoVotada), fila);
        verifyNoInteractions(avaliacaoQuestaoRepository);
        verifyNoMoreInteractions(questaoRepository);
    }

    @Test
    void filaDoAvaliadorTrazOPlacarParcialMasNaoOsMotivosDeQuemRejeitou() {
        // Arrange
        Questao pendente = pendenteDoAutor(61L);
        Usuario avaliador = new Usuario("Bruno", "bruno@escola.test", "hash", escola);
        when(questaoRepository.findPendentesParaAvaliar(StatusQuestao.PENDENTE, OUTRO_PROFESSOR_ID))
                .thenReturn(List.of(pendente));
        when(avaliacaoQuestaoRepository.findByQuestaoIdIn(List.of(61L))).thenReturn(List.of(
                new AvaliacaoQuestao(pendente, avaliador, VotoAvaliacao.APROVAR),
                new AvaliacaoQuestao(pendente, avaliador, VotoAvaliacao.REJEITAR, "Enunciado confuso")));

        // Act
        List<QuestaoResponse> fila = service.listarPendentesComPlacar(atorOutroProfessor);

        // Assert
        assertEquals(1, fila.size());
        assertEquals(1L, fila.get(0).aprovacoes());
        assertEquals(1L, fila.get(0).rejeicoes());
        assertTrue(fila.get(0).motivosRejeicao().isEmpty());
    }
}
