package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.QuestaoResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.AvaliacaoQuestao;
import br.com.maisedu.app.model.NivelDificuldade;
import br.com.maisedu.app.model.Questao;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Banco de questões: criação e edição do rascunho pelo autor, envio para a validação colaborativa
 * (RN02), fila de avaliação de cada professor e consulta do banco compartilhado (RF15).
 */
@Service
@RequiredArgsConstructor
public class QuestaoService {

    private final QuestaoRepository questaoRepository;
    private final TopicoRepository topicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AvaliacaoQuestaoRepository avaliacaoQuestaoRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public Questao criar(UsuarioAutenticado ator, String enunciado, Long topicoId,
                         NivelDificuldade nivelDificuldade, AnoEscolar anoEscolar,
                         List<String> alternativas, int indiceCorreta) {
        if (!ator.possuiRole(RoleNome.PROFESSOR)) {
            throw new NegocioException("Usuário informado não possui papel de professor");
        }
        Usuario professor = usuarioRepository.findById(ator.id())
                .orElseThrow(() -> new NegocioException("Professor não encontrado"));
        Topico topico = buscarTopico(topicoId);

        Questao questao = new Questao(enunciado.strip(), professor, topico, nivelDificuldade, anoEscolar);
        questao.definirAlternativas(alternativas, indiceCorreta);

        Questao salva = questaoRepository.save(questao);
        auditoriaService.registrar(ator, AcaoAuditoria.CRIACAO_QUESTAO, "Questao", salva.getId(), true, null);
        return salva;
    }

    /** Só o autor edita, e só enquanto a questão é rascunho: depois do envio ela fica congelada para os votos. */
    @Transactional
    public Questao editar(UsuarioAutenticado ator, Long questaoId, String enunciado, Long topicoId,
                          NivelDificuldade nivelDificuldade, AnoEscolar anoEscolar,
                          List<String> alternativas, int indiceCorreta) {
        Questao questao = buscarDoAutor(ator, questaoId, "Só o autor pode editar a questão");
        questao.editar(enunciado.strip(), buscarTopico(topicoId), nivelDificuldade, anoEscolar, alternativas,
                indiceCorreta);
        auditoriaService.registrar(ator, AcaoAuditoria.EDICAO_QUESTAO, "Questao", questaoId, true, null);
        return questao;
    }

    /** Rascunho descartado pelo autor. Questões enviadas não são excluídas: os votos e o uso em atividades ficam no histórico. */
    @Transactional
    public void excluir(UsuarioAutenticado ator, Long questaoId) {
        Questao questao = buscarDoAutor(ator, questaoId, "Só o autor pode excluir a questão");
        if (!questao.isRascunho()) {
            throw new NegocioException("Só é possível excluir um rascunho; a questão já foi enviada para validação");
        }
        questaoRepository.delete(questao);
        auditoriaService.registrar(ator, AcaoAuditoria.EXCLUSAO_QUESTAO, "Questao", questaoId, true, null);
    }

    @Transactional
    public Questao enviarParaValidacao(UsuarioAutenticado ator, Long questaoId) {
        Questao questao = buscarDoAutor(ator, questaoId, "Só o autor pode enviar a questão para validação");
        questao.enviarParaValidacao();
        auditoriaService.registrar(ator, AcaoAuditoria.ENVIO_VALIDACAO_QUESTAO, "Questao", questaoId, true, null);
        return questao;
    }

    /** Questões do autor com o placar da validação e os motivos de quem rejeitou (uma consulta para todos os votos). */
    @Transactional(readOnly = true)
    public List<QuestaoResponse> listarMinhas(UsuarioAutenticado ator) {
        return comPlacar(questaoRepository.findByAutorIdOrderByCriadoEmDesc(ator.id()), true);
    }

    /**
     * Fila de avaliação com o placar parcial, para o avaliador ver quantos votos faltam. Os motivos
     * de quem já rejeitou não aparecem aqui, para não influenciar o voto.
     */
    @Transactional(readOnly = true)
    public List<QuestaoResponse> listarPendentesComPlacar(UsuarioAutenticado ator) {
        return comPlacar(listarPendentesParaAvaliar(ator), false);
    }

    /** Pendentes de outros autores que este professor ainda não votou: a fila de avaliação dele. */
    @Transactional(readOnly = true)
    public List<Questao> listarPendentesParaAvaliar(UsuarioAutenticado ator) {
        return questaoRepository.findPendentesParaAvaliar(StatusQuestao.PENDENTE, ator.id());
    }

    /** Banco compartilhado (RF15): questões APROVADAS de qualquer autor e instituição, com filtros opcionais. */
    @Transactional(readOnly = true)
    public List<Questao> buscarAprovadas(Long topicoId, NivelDificuldade nivel, AnoEscolar anoEscolar) {
        return questaoRepository.buscarNoBanco(StatusQuestao.APROVADA, topicoId, nivel, anoEscolar);
    }

    private List<QuestaoResponse> comPlacar(List<Questao> questoes, boolean comMotivos) {
        List<Long> ids = new ArrayList<>();
        for (Questao questao : questoes) {
            ids.add(questao.getId());
        }

        Map<Long, long[]> placar = new HashMap<>();
        Map<Long, List<String>> motivos = new HashMap<>();
        if (!ids.isEmpty()) {
            for (AvaliacaoQuestao avaliacao : avaliacaoQuestaoRepository.findByQuestaoIdIn(ids)) {
                Long questaoId = avaliacao.getQuestao().getId();
                long[] contagem = placar.computeIfAbsent(questaoId, id -> new long[2]);
                if (avaliacao.getVoto() == VotoAvaliacao.APROVAR) {
                    contagem[0]++;
                } else {
                    contagem[1]++;
                    if (comMotivos && avaliacao.getComentario() != null) {
                        motivos.computeIfAbsent(questaoId, id -> new ArrayList<>()).add(avaliacao.getComentario());
                    }
                }
            }
        }

        List<QuestaoResponse> respostas = new ArrayList<>();
        for (Questao questao : questoes) {
            long[] contagem = placar.getOrDefault(questao.getId(), new long[2]);
            respostas.add(QuestaoResponse.comPlacar(questao, contagem[0], contagem[1],
                    motivos.getOrDefault(questao.getId(), List.of())));
        }
        return respostas;
    }

    private Questao buscarDoAutor(UsuarioAutenticado ator, Long questaoId, String mensagemSemPermissao) {
        Questao questao = questaoRepository.findById(questaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Questão não encontrada"));
        if (!questao.isAutor(ator.id())) {
            throw new AccessDeniedException(mensagemSemPermissao);
        }
        return questao;
    }

    private Topico buscarTopico(Long topicoId) {
        return topicoRepository.findById(topicoId)
                .orElseThrow(() -> new NegocioException("Tópico não encontrado"));
    }
}
