package br.com.maisedu.app.service;

import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.LogAuditoria;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.repository.LogAuditoriaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Registro de auditoria: dados do ator, instituição, IP/navegador, limites de tamanho e falha sem derrubar o negócio. */
@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock LogAuditoriaRepository logAuditoriaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock PlatformTransactionManager transactionManager;

    private AuditoriaService service;
    private UsuarioAutenticado professor;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
        service = new AuditoriaService(logAuditoriaRepository, usuarioRepository, transactionManager);
        professor = new UsuarioAutenticado(7L, "Paulo Professor", 3L, EnumSet.of(RoleNome.PROFESSOR), false);
    }

    @AfterEach
    void limparRequisicao() {
        RequestContextHolder.resetRequestAttributes();
    }

    private LogAuditoria registroGravado() {
        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(logAuditoriaRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void registrar_comUsuarioAutenticado_usaOsDadosDaSessao() {
        service.registrar(professor, AcaoAuditoria.CADASTRO_TURMA, "Turma", 10L, true, "Turma criada");

        LogAuditoria registro = registroGravado();
        assertEquals(7L, registro.getUsuarioId());
        assertEquals("Paulo Professor", registro.getUsuarioNome());
        assertEquals(3L, registro.getInstituicaoId());
        assertEquals(AcaoAuditoria.CADASTRO_TURMA, registro.getAcao());
        assertEquals("Turma", registro.getEntidade());
        assertEquals(10L, registro.getEntidadeId());
        assertTrue(registro.isSucesso());
        assertEquals("Turma criada", registro.getDetalhes());
        assertNull(registro.getIdentificadorAcesso());
        assertNull(registro.getIp(), "fora de uma requisição HTTP não há IP");
        assertNull(registro.getUserAgent());
        assertNotNull(registro.getCriadoEm());
        // a instituição veio da sessão: não precisa consultar o usuário
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void registrar_semSessao_descobreAInstituicaoPeloUsuario() {
        when(usuarioRepository.findInstituicaoIdById(20L)).thenReturn(Optional.of(3L));

        service.registrar(20L, "Ana Aluna", "2026001", AcaoAuditoria.LOGIN, "Usuario", 20L, true, null);

        LogAuditoria registro = registroGravado();
        assertEquals(3L, registro.getInstituicaoId());
        assertEquals("2026001", registro.getIdentificadorAcesso());
        assertEquals(AcaoAuditoria.LOGIN, registro.getAcao());
    }

    @Test
    void registrar_semUsuario_ficaSemInstituicao() {
        service.registrar(null, "Sistema", null, AcaoAuditoria.LIMPEZA_AUDITORIA, "LogAuditoria", null, true,
                "3 registro(s) excluído(s)");

        LogAuditoria registro = registroGravado();
        assertNull(registro.getUsuarioId());
        assertNull(registro.getInstituicaoId());
        assertEquals("Sistema", registro.getUsuarioNome());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void registrar_dentroDeUmaRequisicao_guardaIpENavegadorRespeitandoOsLimites() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("User-Agent", "N".repeat(600));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        service.registrar(professor, AcaoAuditoria.LOGOUT, "Usuario", 7L, true, "d".repeat(700));

        LogAuditoria registro = registroGravado();
        assertEquals("10.0.0.5", registro.getIp());
        assertEquals(500, registro.getUserAgent().length());
        assertEquals(500, registro.getDetalhes().length());
    }

    @Test
    void registrar_quandoAGravacaoFalha_naoInterrompeQuemChamou() {
        when(logAuditoriaRepository.save(any(LogAuditoria.class))).thenThrow(new IllegalStateException("banco fora"));

        assertDoesNotThrow(() ->
                service.registrar(professor, AcaoAuditoria.CADASTRO_ALUNO, "Usuario", 20L, true, null));
    }

    @Test
    void consultar_delegaAoRepositorioComOFiltroEAPagina() {
        Pageable pagina = PageRequest.of(0, 20);
        Page<LogAuditoria> esperada = new PageImpl<>(List.of());
        when(logAuditoriaRepository.findAll(ArgumentMatchers.<Specification<LogAuditoria>>any(), eq(pagina)))
                .thenReturn(esperada);

        Page<LogAuditoria> resultado = service.consultar(3L, null, AcaoAuditoria.LOGIN, null, null, pagina);

        assertSame(esperada, resultado);
    }
}
