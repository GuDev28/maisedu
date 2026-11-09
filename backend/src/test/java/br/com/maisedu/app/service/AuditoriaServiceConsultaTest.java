package br.com.maisedu.app.service;

import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.repository.LogAuditoriaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

/**
 * RF13: a consulta da auditoria sempre envia o período preenchido ao repositório. No PostgreSQL, um
 * parâmetro de data em ":de is null" fica sem tipo e a consulta falha (o H2 dos testes aceita).
 */
@ExtendWith(MockitoExtension.class)
class AuditoriaServiceConsultaTest {

    @Mock LogAuditoriaRepository logAuditoriaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock PlatformTransactionManager transactionManager;

    private AuditoriaService service;
    private final Pageable pagina = PageRequest.of(0, 20);

    @BeforeEach
    void setUp() {
        service = new AuditoriaService(logAuditoriaRepository, usuarioRepository, transactionManager);
    }

    @Test
    void rf13_semPeriodoInformadoUsaLimitesAbertos() {
        service.consultar(1L, null, null, null, null, pagina);

        verify(logAuditoriaRepository).buscar(1L, null, null, AuditoriaService.INICIO_DO_PERIODO,
                AuditoriaService.FIM_DO_PERIODO, pagina);
    }

    @Test
    void rf13_periodoInformadoEhRepassadoSemAlteracao() {
        LocalDateTime de = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime ate = LocalDateTime.of(2026, 11, 9, 23, 59);

        service.consultar(1L, 7L, AcaoAuditoria.LOGIN, de, ate, pagina);

        verify(logAuditoriaRepository).buscar(1L, 7L, AcaoAuditoria.LOGIN, de, ate, pagina);
    }
}
