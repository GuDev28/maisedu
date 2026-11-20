package br.com.maisedu.app.service;

import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.repository.LogAuditoriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Retenção da auditoria (LGPD, necessidade): apaga o que passou do prazo e registra a limpeza. */
@ExtendWith(MockitoExtension.class)
class RetencaoAuditoriaServiceTest {

    private static final int RETENCAO_DIAS = 365;

    @Mock LogAuditoriaRepository logAuditoriaRepository;
    @Mock AuditoriaService auditoriaService;

    @Test
    void excluir_apagaOsRegistrosAnterioresAoPrazoERegistraALimpeza() {
        RetencaoAuditoriaService service =
                new RetencaoAuditoriaService(logAuditoriaRepository, auditoriaService, RETENCAO_DIAS);
        when(logAuditoriaRepository.excluirAnterioresA(any(LocalDateTime.class))).thenReturn(4);

        LocalDateTime antes = LocalDateTime.now().minusDays(RETENCAO_DIAS);
        int excluidos = service.excluirRegistrosVencidos();
        LocalDateTime depois = LocalDateTime.now().minusDays(RETENCAO_DIAS);

        assertEquals(4, excluidos);
        ArgumentCaptor<LocalDateTime> limite = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(logAuditoriaRepository).excluirAnterioresA(limite.capture());
        assertFalse(limite.getValue().isBefore(antes), "o limite deve ser hoje menos o prazo de retenção");
        assertFalse(limite.getValue().isAfter(depois));
        verify(auditoriaService).registrar(isNull(), eq("Sistema"), isNull(), eq(AcaoAuditoria.LIMPEZA_AUDITORIA),
                eq("LogAuditoria"), isNull(), eq(true), contains("4 registro(s)"));
    }

    @Test
    void excluir_semNadaVencido_naoRegistraNada() {
        RetencaoAuditoriaService service =
                new RetencaoAuditoriaService(logAuditoriaRepository, auditoriaService, RETENCAO_DIAS);
        when(logAuditoriaRepository.excluirAnterioresA(any(LocalDateTime.class))).thenReturn(0);

        assertEquals(0, service.excluirRegistrosVencidos());

        verifyNoInteractions(auditoriaService);
    }
}
