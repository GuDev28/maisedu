package br.com.maisedu.app.service;

import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.Questao;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.Topico;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AvaliacaoQuestaoRepository;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.TopicoRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestaoService {

    private final QuestaoRepository questaoRepository;
    private final TopicoRepository topicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AvaliacaoQuestaoRepository avaliacaoQuestaoRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public Questao criar(Long professorId, String enunciado, Long topicoId,
                          br.com.maisedu.app.model.NivelDificuldade nivelDificuldade,
                          List<String> alternativas, int indiceCorreta) {
        Usuario professor = usuarioRepository.findById(professorId)
                .orElseThrow(() -> new NegocioException("Professor não encontrado"));
        if (!professor.possuiRole(RoleNome.PROFESSOR)) {
            throw new NegocioException("Usuário informado não possui papel de professor");
        }

        Topico topico = topicoRepository.findById(topicoId)
                .orElseThrow(() -> new NegocioException("Tópico não encontrado"));

        Questao questao = new Questao(enunciado, professor, topico, nivelDificuldade);
        questao.definirAlternativas(alternativas, indiceCorreta);

        Questao salva = questaoRepository.save(questao);
        auditoriaService.registrar(professor.getId(), professor.getNome(), AcaoAuditoria.CRIACAO_QUESTAO,
                "Questao", salva.getId(), true, null);

        return salva;
    }

    @Transactional
    public Questao enviarParaValidacao(Long questaoId, Long professorId) {
        Questao questao = questaoRepository.findById(questaoId)
                .orElseThrow(() -> new NegocioException("Questão não encontrada"));

        if (!questao.isAutor(professorId)) {
            throw new AccessDeniedException("Só o autor pode enviar a questão para validação");
        }

        questao.enviarParaValidacao();

        auditoriaService.registrar(professorId, questao.getAutor().getNome(), AcaoAuditoria.ENVIO_VALIDACAO_QUESTAO,
                "Questao", questaoId, true, null);

        return questao;
    }

    @Transactional(readOnly = true)
    public List<Questao> listarMinhas(Long professorId) {
        return questaoRepository.findByAutorIdOrderByCriadoEmDesc(professorId);
    }

    /** Pendentes de outros autores que este professor ainda não votou — a fila de avaliação dele. */
    @Transactional(readOnly = true)
    public List<Questao> listarPendentesParaAvaliar(Long professorId) {
        List<Questao> pendentes = questaoRepository.findByStatusAndAutorIdNotOrderByCriadoEmAsc(
                br.com.maisedu.app.model.StatusQuestao.PENDENTE, professorId);

        List<Questao> aindaNaoVotadas = new ArrayList<>();
        for (Questao questao : pendentes) {
            if (!avaliacaoQuestaoRepository.existsByQuestaoIdAndProfessorId(questao.getId(), professorId)) {
                aindaNaoVotadas.add(questao);
            }
        }
        return aindaNaoVotadas;
    }
}
