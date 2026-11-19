package br.com.maisedu.app.service;

import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.Atividade;
import br.com.maisedu.app.model.Resposta;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AtividadeRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Fechamento das atividades regulares (RN05 com entrega única): quando o prazo termina, as respostas
 * enviadas entram no desempenho acumulado por tópico e nível. Antes disso o aluno não vê a correção, e
 * o desempenho também não muda, para não revelar o resultado antes da hora.
 *
 * Uma rotina agendada procura as atividades vencidas. Cada atividade é fechada numa transação
 * própria, com a linha da atividade travada; cada resposta é contabilizada uma única vez
 * ({@code resposta.contabilizada_em}), então rodar de novo nunca soma duas vezes.
 */
@Service
@Slf4j
public class FechamentoAtividadesService {

    private final AtividadeRepository atividadeRepository;
    private final RespostaRepository respostaRepository;
    private final UsuarioRepository usuarioRepository;
    private final DesempenhoService desempenhoService;
    private final AuditoriaService auditoriaService;
    private final TransactionTemplate transacao;

    public FechamentoAtividadesService(AtividadeRepository atividadeRepository,
                                       RespostaRepository respostaRepository,
                                       UsuarioRepository usuarioRepository,
                                       DesempenhoService desempenhoService,
                                       AuditoriaService auditoriaService,
                                       PlatformTransactionManager transactionManager) {
        this.atividadeRepository = atividadeRepository;
        this.respostaRepository = respostaRepository;
        this.usuarioRepository = usuarioRepository;
        this.desempenhoService = desempenhoService;
        this.auditoriaService = auditoriaService;
        this.transacao = new TransactionTemplate(transactionManager);
    }

    /** Rotina agendada (padrão: a cada minuto). Devolve quantas atividades foram fechadas. */
    @Scheduled(fixedDelayString = "${maisedu.atividades.fechamento-intervalo-ms:60000}",
            initialDelayString = "${maisedu.atividades.fechamento-atraso-inicial-ms:20000}")
    public int fecharVencidas() {
        int fechadas = 0;
        for (Long atividadeId : atividadeRepository.findIdsVencidasNaoFechadas(LocalDateTime.now())) {
            try {
                Boolean fechou = transacao.execute(status -> fechar(atividadeId));
                if (Boolean.TRUE.equals(fechou)) {
                    fechadas++;
                }
            } catch (Exception ex) {
                // uma atividade com problema não impede as outras; a próxima execução tenta de novo
                log.warn("Falha ao fechar a atividade {}", atividadeId, ex);
            }
        }
        return fechadas;
    }

    /**
     * Fecha uma atividade vencida: contabiliza as respostas ainda não contabilizadas e marca a
     * atividade como fechada. Deve rodar dentro de uma transação. Devolve falso se não havia o que fechar.
     */
    public boolean fechar(Long atividadeId) {
        Atividade atividade = atividadeRepository.findByIdParaFechamento(atividadeId).orElse(null);
        LocalDateTime agora = LocalDateTime.now();
        if (atividade == null || atividade.isFechada() || !atividade.prazoEncerrado(agora)) {
            return false;
        }

        // Agrupa por aluno, em ordem de id: as linhas dos alunos são travadas sempre na mesma ordem,
        // o que evita impasse com um envio de prática adaptativa do mesmo aluno acontecendo ao mesmo tempo.
        Map<Long, List<Resposta>> porAluno = new TreeMap<>();
        for (Resposta resposta : respostaRepository.findNaoContabilizadasDaAtividade(atividadeId)) {
            Long alunoId = resposta.getAtividadeAlunoQuestao().getAtividadeAluno().getAluno().getId();
            porAluno.computeIfAbsent(alunoId, id -> new ArrayList<>()).add(resposta);
        }

        int contabilizadas = 0;
        for (Map.Entry<Long, List<Resposta>> entrada : porAluno.entrySet()) {
            Usuario aluno = usuarioRepository.findByIdParaAtualizacao(entrada.getKey()).orElse(null);
            if (aluno == null) {
                continue;
            }
            for (Resposta resposta : entrada.getValue()) {
                desempenhoService.registrarResposta(aluno, resposta.getAtividadeAlunoQuestao().getQuestao(),
                        resposta.acertou());
                resposta.marcarContabilizada(agora);
                contabilizadas++;
            }
        }
        atividade.fechar(agora);

        auditoriaService.registrar(null, "Sistema", null, AcaoAuditoria.FECHAMENTO_ATIVIDADE, "Atividade",
                atividade.getId(), true, contabilizadas + " resposta(s) de " + porAluno.size()
                        + " aluno(s) contabilizada(s) no desempenho");
        log.info("Atividade {} fechada: {} resposta(s) de {} aluno(s) contabilizada(s).", atividade.getId(),
                contabilizadas, porAluno.size());
        return true;
    }
}
