package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.AvaliacaoQuestaoResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.*;
import br.com.maisedu.app.repository.AvaliacaoQuestaoRepository;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvaliacaoQuestaoServiceTest {

    @Mock QuestaoRepository questaoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock AvaliacaoQuestaoRepository avaliacaoQuestaoRepository;
    @Mock AuditoriaService auditoriaService;
    @InjectMocks AvaliacaoQuestaoService service;

    private static final long QUESTAO_ID = 10L;
    private static final long AUTOR_ID = 1L;
    private static final long AVALIADOR_ID = 2L;

    private Questao questao;
    private Usuario avaliador;

    private static final UsuarioAutenticado ATOR_AVALIADOR = professorAutenticado(AVALIADOR_ID);
    private static final UsuarioAutenticado ATOR_AUTOR = professorAutenticado(AUTOR_ID);

    @BeforeEach
    void setUp() {
        Instituicao escola = mock(Instituicao.class);
        Usuario autor = professor(AUTOR_ID, escola);
        avaliador = professor(AVALIADOR_ID, escola);
        questao = new Questao("Quanto é 2 + 2?", autor,
                new Topico("Números naturais e operações"), NivelDificuldade.FACIL);
        ReflectionTestUtils.setField(questao, "id", QUESTAO_ID);
        // a votação só é possível depois de a questão sair do rascunho (RN2 opera sobre PENDENTE)
        questao.definirAlternativas(List.of("1", "2", "3", "4", "5"), 3);
        questao.enviarParaValidacao();
    }

    private static Usuario professor(long id, Instituicao escola) {
        Usuario u = new Usuario("Prof " + id, "prof" + id + "@escola.test", "hash", escola);
        ReflectionTestUtils.setField(u, "id", id);
        u.getRoles().add(new Role(RoleNome.PROFESSOR));
        return u;
    }

    private static UsuarioAutenticado professorAutenticado(long id) {
        return new UsuarioAutenticado(id, "Prof " + id, 1L, Set.of(RoleNome.PROFESSOR), false);
    }

    private void placarAntes(long aprovacoes, long rejeicoes) {
        when(questaoRepository.findByIdParaVotacao(QUESTAO_ID)).thenReturn(Optional.of(questao));
        when(usuarioRepository.findById(AVALIADOR_ID)).thenReturn(Optional.of(avaliador));
        when(avaliacaoQuestaoRepository.existsByQuestaoIdAndProfessorId(QUESTAO_ID, AVALIADOR_ID)).thenReturn(false);
        when(avaliacaoQuestaoRepository.countByQuestaoId(QUESTAO_ID)).thenReturn(aprovacoes + rejeicoes);
    }

    private void placarDepois(long aprovacoes, long rejeicoes) {
        when(avaliacaoQuestaoRepository.countByQuestaoIdAndVoto(QUESTAO_ID, VotoAvaliacao.APROVAR)).thenReturn(aprovacoes);
        when(avaliacaoQuestaoRepository.countByQuestaoIdAndVoto(QUESTAO_ID, VotoAvaliacao.REJEITAR)).thenReturn(rejeicoes);
    }

    @Test
    void primeiroVotoNaoDecideAQuestao() {
        placarAntes(0, 0);
        placarDepois(1, 0);

        AvaliacaoQuestaoResponse r = service.avaliar(ATOR_AVALIADOR, QUESTAO_ID, VotoAvaliacao.APROVAR, null);

        assertThat(r.status()).isEqualTo(StatusQuestao.PENDENTE);
        assertThat(questao.getStatus()).isEqualTo(StatusQuestao.PENDENTE);
        assertThat(r.aprovacoes()).isEqualTo(1);
        verify(avaliacaoQuestaoRepository).save(any(AvaliacaoQuestao.class));
    }

    @Test
    void terceiraAprovacaoAprovaAQuestao() {
        placarAntes(2, 1);
        placarDepois(3, 1);

        AvaliacaoQuestaoResponse r = service.avaliar(ATOR_AVALIADOR, QUESTAO_ID, VotoAvaliacao.APROVAR, null);

        assertThat(r.status()).isEqualTo(StatusQuestao.APROVADA);
        assertThat(questao.getStatus()).isEqualTo(StatusQuestao.APROVADA);
    }

    @Test
    void terceiraRejeicaoRejeitaAQuestao() {
        placarAntes(2, 2);
        placarDepois(2, 3);

        AvaliacaoQuestaoResponse r = service.avaliar(ATOR_AVALIADOR, QUESTAO_ID, VotoAvaliacao.REJEITAR,
                "Enunciado ambíguo");

        assertThat(r.status()).isEqualTo(StatusQuestao.REJEITADA);
    }

    @Test
    void doisADoisContinuaPendente() {
        placarAntes(2, 1);
        placarDepois(2, 2);

        AvaliacaoQuestaoResponse r = service.avaliar(ATOR_AVALIADOR, QUESTAO_ID, VotoAvaliacao.REJEITAR,
                "Enunciado ambíguo");

        assertThat(r.status()).isEqualTo(StatusQuestao.PENDENTE);
    }

    @Test
    void naoAceitaVotoEmQuestaoAindaRascunho() {
        Questao rascunho = new Questao("Ainda escrevendo...", questao.getAutor(),
                new Topico("Frações"), NivelDificuldade.FACIL);
        ReflectionTestUtils.setField(rascunho, "id", 20L);
        when(questaoRepository.findByIdParaVotacao(20L)).thenReturn(Optional.of(rascunho));
        when(usuarioRepository.findById(AVALIADOR_ID)).thenReturn(Optional.of(avaliador));

        assertThatThrownBy(() -> service.avaliar(ATOR_AVALIADOR, 20L, VotoAvaliacao.APROVAR, null))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("rascunho");
        verify(avaliacaoQuestaoRepository, never()).save(any());
    }

    @Test
    void autorNaoPodeVotarNaPropriaQuestao() {
        when(questaoRepository.findByIdParaVotacao(QUESTAO_ID)).thenReturn(Optional.of(questao));
        when(usuarioRepository.findById(AUTOR_ID)).thenReturn(Optional.of(questao.getAutor()));

        assertThatThrownBy(() -> service.avaliar(ATOR_AUTOR, QUESTAO_ID, VotoAvaliacao.APROVAR, null))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("própria questão");
        verify(avaliacaoQuestaoRepository, never()).save(any());
    }

    @Test
    void professorNaoVotaDuasVezes() {
        when(questaoRepository.findByIdParaVotacao(QUESTAO_ID)).thenReturn(Optional.of(questao));
        when(usuarioRepository.findById(AVALIADOR_ID)).thenReturn(Optional.of(avaliador));
        when(avaliacaoQuestaoRepository.existsByQuestaoIdAndProfessorId(QUESTAO_ID, AVALIADOR_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.avaliar(ATOR_AVALIADOR, QUESTAO_ID, VotoAvaliacao.APROVAR, null))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("já avaliou");
        verify(avaliacaoQuestaoRepository, never()).save(any());
    }

    @Test
    void naoAceitaVotoEmQuestaoJaDecidida() {
        questao.apurarVotos(3, 0);
        when(questaoRepository.findByIdParaVotacao(QUESTAO_ID)).thenReturn(Optional.of(questao));
        when(usuarioRepository.findById(AVALIADOR_ID)).thenReturn(Optional.of(avaliador));

        assertThatThrownBy(() -> service.avaliar(ATOR_AVALIADOR, QUESTAO_ID, VotoAvaliacao.REJEITAR,
                "Enunciado ambíguo"))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("encerrada");
    }

    @Test
    void naoAceitaSextoVoto() {
        placarAntes(2, 3); // cenário inconsistente: 5 votos e ainda pendente

        assertThatThrownBy(() -> service.avaliar(ATOR_AVALIADOR, QUESTAO_ID, VotoAvaliacao.APROVAR, null))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("máximo");
        verify(avaliacaoQuestaoRepository, never()).save(any());
    }

    @Test
    void alunoNaoPodeVotar() {
        Usuario aluno = Usuario.novoAluno("Aluno", "2026001", "hash", mock(Instituicao.class));
        ReflectionTestUtils.setField(aluno, "id", 99L);
        aluno.getRoles().add(new Role(RoleNome.ALUNO));
        when(questaoRepository.findByIdParaVotacao(QUESTAO_ID)).thenReturn(Optional.of(questao));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(aluno));

        UsuarioAutenticado atorAluno = new UsuarioAutenticado(99L, "Aluno", 1L, Set.of(RoleNome.ALUNO), false);
        assertThatThrownBy(() -> service.avaliar(atorAluno, QUESTAO_ID, VotoAvaliacao.APROVAR, null))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("professor");
    }

    @Test
    void rejeicaoSemMotivoERecusadaAntesDeTocarNoBanco() {
        assertThatThrownBy(() -> service.avaliar(ATOR_AVALIADOR, QUESTAO_ID, VotoAvaliacao.REJEITAR, "   "))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("motivo da rejeição");
        verifyNoInteractions(questaoRepository, usuarioRepository, avaliacaoQuestaoRepository, auditoriaService);
    }

    @Test
    void motivoDaRejeicaoEGravadoComOVoto() {
        placarAntes(0, 0);
        placarDepois(0, 1);

        service.avaliar(ATOR_AVALIADOR, QUESTAO_ID, VotoAvaliacao.REJEITAR, "  A alternativa correta está errada.  ");

        ArgumentCaptor<AvaliacaoQuestao> salvo = ArgumentCaptor.forClass(AvaliacaoQuestao.class);
        verify(avaliacaoQuestaoRepository).save(salvo.capture());
        assertThat(salvo.getValue().getVoto()).isEqualTo(VotoAvaliacao.REJEITAR);
        assertThat(salvo.getValue().getComentario()).isEqualTo("A alternativa correta está errada.");
    }

    @Test
    void aprovacaoNaoExigeComentario() {
        placarAntes(0, 0);
        placarDepois(1, 0);

        service.avaliar(ATOR_AVALIADOR, QUESTAO_ID, VotoAvaliacao.APROVAR, "");

        ArgumentCaptor<AvaliacaoQuestao> salvo = ArgumentCaptor.forClass(AvaliacaoQuestao.class);
        verify(avaliacaoQuestaoRepository).save(salvo.capture());
        assertThat(salvo.getValue().getComentario()).isNull();
    }
}
