package br.com.maisedu.app.service;

import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.LogAuditoria;
import br.com.maisedu.app.repository.LogAuditoriaRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaService {

    private final LogAuditoriaRepository logAuditoriaRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Long usuarioId, String usuarioNome, String identificadorAcesso, AcaoAuditoria acao,
                           String entidade, Long entidadeId, boolean sucesso, String detalhes) {
        try {
            LogAuditoria registro = new LogAuditoria(
                    usuarioId, usuarioNome, identificadorAcesso, acao, entidade, entidadeId,
                    sucesso, detalhes, obterIp(), obterUserAgent());
            logAuditoriaRepository.save(registro);
        } catch (Exception ex) {
            log.warn("Falha ao gravar log de auditoria (ação={}, usuarioId={})", acao, usuarioId, ex);
        }
    }

    /** Ações de negócio (cadastro, vínculo, avaliação): sempre há um usuário autenticado. */
    public void registrar(UsuarioAutenticado ator, AcaoAuditoria acao, String entidade, Long entidadeId,
                           boolean sucesso, String detalhes) {
        registrar(ator.id(), ator.nome(), null, acao, entidade, entidadeId, sucesso, detalhes);
    }

    /** Mesma ideia, quando quem chama já tem o usuário carregado (ex.: dentro de AutenticacaoService), sem identificador de acesso a registrar. */
    public void registrar(Long usuarioId, String usuarioNome, AcaoAuditoria acao, String entidade, Long entidadeId,
                           boolean sucesso, String detalhes) {
        registrar(usuarioId, usuarioNome, null, acao, entidade, entidadeId, sucesso, detalhes);
    }

    @Transactional(readOnly = true)
    public Page<LogAuditoria> consultar(Long usuarioId, AcaoAuditoria acao, LocalDateTime de, LocalDateTime ate,
                                         Pageable pageable) {
        return logAuditoriaRepository.buscar(usuarioId, acao, de, ate, pageable);
    }

    private String obterIp() {
        HttpServletRequest request = requestAtual();
        if (request == null) {
            return null;
        }
        
        String encaminhadoPor = request.getHeader("X-Forwarded-For");
        if (encaminhadoPor != null && !encaminhadoPor.isBlank()) {
            return encaminhadoPor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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
}
