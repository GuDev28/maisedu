package br.com.maisedu.app.service;

import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.repository.LogAuditoriaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Retenção dos registros de auditoria (LGPD, princípio da necessidade): uma rotina diária apaga os
 * registros mais antigos que maisedu.auditoria.retencao-dias (padrão: 365 dias). O prazo também
 * aparece na Política de Privacidade.
 */
@Service
@Slf4j
public class RetencaoAuditoriaService {

    private final LogAuditoriaRepository logAuditoriaRepository;
    private final AuditoriaService auditoriaService;
    private final int retencaoDias;

    public RetencaoAuditoriaService(LogAuditoriaRepository logAuditoriaRepository,
                                    AuditoriaService auditoriaService,
                                    @Value("${maisedu.auditoria.retencao-dias:365}") int retencaoDias) {
        this.logAuditoriaRepository = logAuditoriaRepository;
        this.auditoriaService = auditoriaService;
        this.retencaoDias = retencaoDias;
    }

    @Scheduled(cron = "${maisedu.auditoria.limpeza-cron:0 30 3 * * *}")
    @Transactional
    public int excluirRegistrosVencidos() {
        LocalDateTime limite = LocalDateTime.now().minusDays(retencaoDias);
        int excluidos = logAuditoriaRepository.excluirAnterioresA(limite);
        if (excluidos > 0) {
            log.info("Retenção de auditoria: {} registro(s) anteriores a {} excluído(s).", excluidos, limite);
            auditoriaService.registrar(null, "Sistema", null, AcaoAuditoria.LIMPEZA_AUDITORIA, "LogAuditoria", null,
                    true, excluidos + " registro(s) anteriores a " + limite.toLocalDate() + " excluído(s)");
        }
        return excluidos;
    }
}
