package br.com.maisedu.app.service;

import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.LogAuditoria;
import br.com.maisedu.app.repository.LogAuditoriaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AuditoriaService {

    /** Limites usados quando o admin não informa o início ou o fim do período na consulta (RF13). */
    static final LocalDateTime INICIO_DO_PERIODO = LocalDateTime.of(2000, 1, 1, 0, 0);
    static final LocalDateTime FIM_DO_PERIODO = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private final LogAuditoriaRepository logAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Cada registro é gravado numa transação própria (REQUIRES_NEW), aberta e fechada aqui dentro.
     * O try/catch fica FORA dessa transação: se a gravação falhar, a falha só vai para o log da
     * aplicação e nunca marca a transação de negócio do chamador para rollback.
     */
    private final TransactionTemplate transacaoIndependente;

    public AuditoriaService(LogAuditoriaRepository logAuditoriaRepository, UsuarioRepository usuarioRepository,
                            PlatformTransactionManager transactionManager) {
        this.logAuditoriaRepository = logAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.transacaoIndependente = new TransactionTemplate(transactionManager);
        this.transacaoIndependente.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void registrar(Long usuarioId, String usuarioNome, String identificadorAcesso, AcaoAuditoria acao,
                           String entidade, Long entidadeId, boolean sucesso, String detalhes) {
        gravar(usuarioId, usuarioNome, null, identificadorAcesso, acao, entidade, entidadeId, sucesso, detalhes);
    }

    /** Ações de negócio (cadastro, vínculo, avaliação): sempre há um usuário autenticado. */
    public void registrar(UsuarioAutenticado ator, AcaoAuditoria acao, String entidade, Long entidadeId,
                           boolean sucesso, String detalhes) {
        gravar(ator.id(), ator.nome(), ator.instituicaoId(), null, acao, entidade, entidadeId, sucesso, detalhes);
    }

    /** Mesma ideia, quando quem chama já tem o usuário carregado (ex.: dentro de AutenticacaoService), sem identificador de acesso a registrar. */
    public void registrar(Long usuarioId, String usuarioNome, AcaoAuditoria acao, String entidade, Long entidadeId,
                           boolean sucesso, String detalhes) {
        gravar(usuarioId, usuarioNome, null, null, acao, entidade, entidadeId, sucesso, detalhes);
    }

    /**
     * Grava o registro sem nunca interromper a operação de negócio. Quando a instituição não é
     * informada, ela é descoberta a partir do usuário, para que cada admin veja só os registros
     * da própria instituição.
     */
    private void gravar(Long usuarioId, String usuarioNome, Long instituicaoId, String identificadorAcesso,
                        AcaoAuditoria acao, String entidade, Long entidadeId, boolean sucesso, String detalhes) {
        String ip = obterIp();
        String userAgent = limitar(obterUserAgent(), 500);
        try {
            transacaoIndependente.executeWithoutResult(status -> {
                Long instituicao = instituicaoId;
                if (instituicao == null && usuarioId != null) {
                    instituicao = usuarioRepository.findInstituicaoIdById(usuarioId).orElse(null);
                }
                logAuditoriaRepository.save(new LogAuditoria(
                        usuarioId, usuarioNome, instituicao, identificadorAcesso, acao, entidade, entidadeId,
                        sucesso, limitar(detalhes, 500), ip, userAgent));
            });
        } catch (Exception ex) {
            log.warn("Falha ao gravar log de auditoria (ação={}, usuarioId={})", acao, usuarioId, ex);
        }
    }

    @Transactional(readOnly = true)
    public Page<LogAuditoria> consultar(Long instituicaoId, Long usuarioId, AcaoAuditoria acao,
                                        LocalDateTime de, LocalDateTime ate, Pageable pageable) {
        // Sem data informada, o período fica aberto naquele lado (ver LogAuditoriaRepository.buscar).
        LocalDateTime inicio = de != null ? de : INICIO_DO_PERIODO;
        LocalDateTime fim = ate != null ? ate : FIM_DO_PERIODO;
        return logAuditoriaRepository.buscar(instituicaoId, usuarioId, acao, inicio, fim, pageable);
    }

    /**
     * IP do cliente. Quando a aplicação roda atrás de um proxy reverso confiável, o Tomcat
     * (server.forward-headers-strategy=native) já substitui o remoteAddr pelo IP real; o cabeçalho
     * X-Forwarded-For não é lido diretamente porque qualquer cliente pode forjá-lo.
     */
    private String obterIp() {
        HttpServletRequest request = requestAtual();
        return request == null ? null : request.getRemoteAddr();
    }

    private String obterUserAgent() {
        HttpServletRequest request = requestAtual();
        return request == null ? null : request.getHeader("User-Agent");
    }

    private HttpServletRequest requestAtual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private static String limitar(String texto, int tamanho) {
        if (texto == null || texto.length() <= tamanho) {
            return texto;
        }
        return texto.substring(0, tamanho);
    }
}
