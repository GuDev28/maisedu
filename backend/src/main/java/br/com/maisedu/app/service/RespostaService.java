package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.EnvioFolhaRequest;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.Alternativa;
import br.com.maisedu.app.model.AtividadeAluno;
import br.com.maisedu.app.model.AtividadeAlunoQuestao;
import br.com.maisedu.app.model.Resposta;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlternativaRepository;
import br.com.maisedu.app.repository.AtividadeAlunoQuestaoRepository;
import br.com.maisedu.app.repository.AtividadeAlunoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Entrega única (RF16) com integridade e unicidade das respostas (RN04).
 *
 * Até o envio (e, na atividade regular, até o prazo), o aluno marca e troca as alternativas à vontade:
 * a marcação fica salva no servidor e não é resposta. No envio, todas as questões precisam ter uma
 * alternativa; as respostas são gravadas de uma vez, uma por questão, e não mudam mais.
 *
 * O desempenho (RN05) entra no envio da prática adaptativa; na atividade regular, só no fechamento do
 * prazo ({@link FechamentoAtividadesService}), para não revelar o resultado antes da hora.
 */
@Service
@RequiredArgsConstructor
public class RespostaService {

    private final UsuarioRepository usuarioRepository;
    private final AtividadeAlunoRepository atividadeAlunoRepository;
    private final AtividadeAlunoQuestaoRepository atividadeAlunoQuestaoRepository;
    private final AlternativaRepository alternativaRepository;
    private final RespostaRepository respostaRepository;
    private final DesempenhoService desempenhoService;
    private final AuditoriaService auditoriaService;

    /**
     * Marcação provisória: guarda a alternativa escolhida para a questão, sem criar resposta e sem
     * mexer no desempenho. Pode ser trocada à vontade enquanto a folha aceita respostas.
     */
    @Transactional
    public void marcar(Long alunoId, Long atividadeAlunoId, Long atividadeAlunoQuestaoId, Long alternativaId) {
        AtividadeAlunoQuestao entregue = atividadeAlunoQuestaoRepository.findById(atividadeAlunoQuestaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Questão não encontrada nesta atividade."));
        AtividadeAluno atividadeAluno = entregue.getAtividadeAluno();
        if (!atividadeAluno.getId().equals(atividadeAlunoId) || !atividadeAluno.pertenceAo(alunoId)) {
            throw new RecursoNaoEncontradoException("Questão não encontrada nesta atividade.");
        }
        exigirQueAceitaRespostas(atividadeAluno, LocalDateTime.now());
        if (respostaRepository.existsByAtividadeAlunoQuestaoId(entregue.getId())) {
            throw new NegocioException("Esta questão já foi respondida e a resposta não pode ser alterada.");
        }
        Alternativa alternativa = alternativaRepository.findById(alternativaId)
                .orElseThrow(() -> new NegocioException("Alternativa não encontrada."));
        entregue.marcar(alternativa);
        // sem save explícito: "entregue" é gerenciada pela transação (dirty checking)
    }

    /**
     * Envio da folha (entrega única). Recusa o envio repetido, o envio depois do prazo e o envio com
     * questões sem resposta. Questões já respondidas antes (fluxo antigo, uma por vez) são mantidas.
     */
    @Transactional
    public AtividadeAluno enviar(Long alunoId, Long atividadeAlunoId, List<EnvioFolhaRequest.Escolha> escolhas) {
        // Trava a linha do aluno: envios simultâneos dele (duplo clique, duas abas) são processados um de
        // cada vez, o que mantém a unicidade das respostas e os contadores de desempenho corretos.
        Usuario aluno = usuarioRepository.findByIdParaAtualizacao(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
        AtividadeAluno atividadeAluno = atividadeAlunoRepository.findById(atividadeAlunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada."));
        if (!atividadeAluno.pertenceAo(alunoId)) {
            // RN04: o aluno só acessa o que foi entregue a ele; 404 para não revelar folhas de outros alunos.
            throw new RecursoNaoEncontradoException("Atividade não encontrada.");
        }
        LocalDateTime agora = LocalDateTime.now();
        exigirQueAceitaRespostas(atividadeAluno, agora);

        Map<Long, Long> alternativaPorQuestao = new HashMap<>();
        if (escolhas != null) {
            for (EnvioFolhaRequest.Escolha escolha : escolhas) {
                if (escolha == null || escolha.atividadeAlunoQuestaoId() == null || escolha.alternativaId() == null) {
                    continue;
                }
                alternativaPorQuestao.put(escolha.atividadeAlunoQuestaoId(), escolha.alternativaId());
            }
        }

        Set<Long> doAluno = new HashSet<>();
        for (AtividadeAlunoQuestao entregue : atividadeAluno.getQuestoes()) {
            doAluno.add(entregue.getId());
        }
        for (Long questaoId : alternativaPorQuestao.keySet()) {
            if (!doAluno.contains(questaoId)) {
                throw new RecursoNaoEncontradoException("Questão não encontrada nesta atividade.");
            }
        }

        Set<Long> jaRespondidas = new HashSet<>();
        for (Resposta resposta : respostaRepository.findByAtividadeAluno(atividadeAluno.getId())) {
            jaRespondidas.add(resposta.getAtividadeAlunoQuestao().getId());
        }

        List<AtividadeAlunoQuestao> pendentes = new ArrayList<>();
        int semResposta = 0;
        for (AtividadeAlunoQuestao entregue : atividadeAluno.getQuestoes()) {
            if (jaRespondidas.contains(entregue.getId())) {
                continue;
            }
            if (!alternativaPorQuestao.containsKey(entregue.getId())) {
                semResposta++;
            } else {
                pendentes.add(entregue);
            }
        }
        if (semResposta > 0) {
            throw new NegocioException(semResposta == 1
                    ? "Falta responder 1 questão. O envio só é aceito com todas as questões respondidas."
                    : "Faltam responder " + semResposta + " questões. O envio só é aceito com todas as questões respondidas.");
        }

        List<Long> alternativaIds = new ArrayList<>();
        for (AtividadeAlunoQuestao entregue : pendentes) {
            alternativaIds.add(alternativaPorQuestao.get(entregue.getId()));
        }
        Map<Long, Alternativa> alternativas = new HashMap<>();
        for (Alternativa alternativa : alternativaRepository.findAllById(alternativaIds)) {
            alternativas.put(alternativa.getId(), alternativa);
        }

        List<Resposta> novas = new ArrayList<>();
        for (AtividadeAlunoQuestao entregue : pendentes) {
            Alternativa alternativa = alternativas.get(alternativaPorQuestao.get(entregue.getId()));
            if (alternativa == null) {
                throw new NegocioException("Alternativa não encontrada.");
            }
            // RN04: a alternativa precisa pertencer à questão (validado no construtor e por trigger no banco)
            novas.add(new Resposta(entregue, alternativa));
            entregue.limparMarcacao();
        }
        respostaRepository.saveAll(novas);
        atividadeAluno.concluir();

        if (atividadeAluno.isAdaptativa()) {
            // Prática adaptativa: sem prazo, a correção é liberada no envio e o desempenho entra na hora.
            for (Resposta resposta : novas) {
                desempenhoService.registrarResposta(aluno, resposta.getAtividadeAlunoQuestao().getQuestao(),
                        resposta.acertou());
                resposta.marcarContabilizada(agora);
            }
        }

        auditoriaService.registrar(aluno.getId(), aluno.getNome(), null, AcaoAuditoria.ENVIO_ATIVIDADE,
                "AtividadeAluno", atividadeAluno.getId(), true,
                (atividadeAluno.isAdaptativa() ? "prática adaptativa: " : "atividade " + atividadeAluno.getAtividade().getId()
                        + ": ") + novas.size() + " resposta(s)");
        return atividadeAluno;
    }

    private static void exigirQueAceitaRespostas(AtividadeAluno atividadeAluno, LocalDateTime agora) {
        if (atividadeAluno.isConcluida()) {
            throw new NegocioException("Esta atividade já foi enviada e as respostas não podem ser alteradas.");
        }
        if (!atividadeAluno.aceitaRespostas(agora)) {
            throw new NegocioException("O prazo desta atividade terminou: as respostas não podem mais ser enviadas.");
        }
    }
}
