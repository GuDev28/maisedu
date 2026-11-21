package br.com.maisedu.app.service;

import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.StatusEmailAcesso;
import br.com.maisedu.app.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * E-mails de acesso: sem chave do Resend (só registra), com o Resend simulado (sucesso e falha), envio
 * depois do commit, minimização no e-mail do responsável e situação gravada no usuário.
 */
@ExtendWith(MockitoExtension.class)
class ResendEmailServiceTest {

    private static final Long PROFESSOR_ID = 7L;
    private static final Long ALUNO_ID = 20L;
    private static final String REMETENTE = "MaisEdu <nao-responda@maisedu.dev>";
    private static final String SEM_CHAVE = "RESEND_API_KEY não configurada";

    @Mock AuditoriaService auditoriaService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock PlatformTransactionManager transactionManager;

    private MockRestServiceServer resend;

    @AfterEach
    void limparSincronizacao() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private ResendEmailService semChave(boolean registrarNoLog) {
        return new ResendEmailService("", REMETENTE, registrarNoLog, "http://localhost:8081/", auditoriaService,
                usuarioRepository, transactionManager);
    }

    /** Serviço com chave, mas com o cliente HTTP ligado a um servidor simulado (nada sai para a internet). */
    private ResendEmailService comResendSimulado() {
        ResendEmailService service = new ResendEmailService("re_chave_teste", REMETENTE, false,
                "https://maisedu.dev/", auditoriaService, usuarioRepository, transactionManager);
        RestClient original = (RestClient) ReflectionTestUtils.getField(service, "restClient");
        RestClient.Builder builder = original.mutate();
        resend = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(service, "restClient", builder.build());
        return service;
    }

    private static String corpo(ClientHttpRequest requisicao) throws IOException {
        return ((MockClientHttpRequest) requisicao).getBodyAsString();
    }

    @Test
    void semChave_naoEnviaERegistraQueOEmailNaoEstaConfigurado() {
        semChave(false).enviarSenhaProvisoria(PROFESSOR_ID, "Paulo", "paulo@escola.test", "Senha12345");

        verify(auditoriaService).registrar(PROFESSOR_ID, "Paulo", null, AcaoAuditoria.ENVIO_EMAIL, "Usuario",
                PROFESSOR_ID, false, SEM_CHAVE);
        verify(usuarioRepository).registrarEnvioEmailAcesso(eq(PROFESSOR_ID), eq(StatusEmailAcesso.NAO_CONFIGURADO),
                any(LocalDateTime.class));
    }

    @Test
    void semChave_emDesenvolvimento_tambemRegistraANaoConfiguracao() {
        semChave(true).enviarNovaSenhaProvisoria(PROFESSOR_ID, "Paulo", "paulo@escola.test", "Senha12345");

        verify(auditoriaService).registrar(PROFESSOR_ID, "Paulo", null, AcaoAuditoria.ENVIO_EMAIL, "Usuario",
                PROFESSOR_ID, false, SEM_CHAVE);
        verify(usuarioRepository).registrarEnvioEmailAcesso(eq(PROFESSOR_ID), eq(StatusEmailAcesso.NAO_CONFIGURADO),
                any(LocalDateTime.class));
    }

    @Test
    void semChave_novaSenhaDoAluno_gravaASituacaoDoAluno() {
        semChave(false).enviarNovaSenhaAoResponsavel(ALUNO_ID, "Ana Souza", "2026001", "Escola Modelo",
                "responsavel@familia.test", "Senha12345");

        verify(auditoriaService).registrar(ALUNO_ID, "Ana Souza", null, AcaoAuditoria.ENVIO_EMAIL, "Usuario",
                ALUNO_ID, false, SEM_CHAVE);
        verify(usuarioRepository).registrarEnvioEmailAcesso(eq(ALUNO_ID), eq(StatusEmailAcesso.NAO_CONFIGURADO),
                any(LocalDateTime.class));
    }

    @Test
    void semUsuario_naoTentaGravarASituacao() {
        semChave(false).enviarSenhaProvisoria(null, "Convidado", "convidado@escola.test", "Senha12345");

        verify(auditoriaService).registrar(null, "Convidado", null, AcaoAuditoria.ENVIO_EMAIL, "Usuario", null,
                false, SEM_CHAVE);
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void falhaAoGravarASituacao_naoInterrompeQuemChamou() {
        when(usuarioRepository.registrarEnvioEmailAcesso(eq(PROFESSOR_ID), eq(StatusEmailAcesso.NAO_CONFIGURADO),
                any(LocalDateTime.class))).thenThrow(new IllegalStateException("banco fora"));

        assertDoesNotThrow(() ->
                semChave(false).enviarSenhaProvisoria(PROFESSOR_ID, "Paulo", "paulo@escola.test", "Senha12345"));
    }

    @Test
    void comTransacaoAberta_soEnviaDepoisDoCommit() {
        ResendEmailService service = semChave(false);
        TransactionSynchronizationManager.initSynchronization();

        service.enviarSenhaProvisoria(PROFESSOR_ID, "Paulo", "paulo@escola.test", "Senha12345");

        // nada acontece enquanto o cadastro não for confirmado
        verifyNoInteractions(auditoriaService, usuarioRepository);
        List<TransactionSynchronization> sincronizacoes = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, sincronizacoes.size());

        for (TransactionSynchronization sincronizacao : sincronizacoes) {
            sincronizacao.afterCommit();
        }

        verify(auditoriaService).registrar(PROFESSOR_ID, "Paulo", null, AcaoAuditoria.ENVIO_EMAIL, "Usuario",
                PROFESSOR_ID, false, SEM_CHAVE);
    }

    @Test
    void comChave_enviaPeloResendEMarcaComoEnviado() {
        ResendEmailService service = comResendSimulado();
        resend.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer re_chave_teste"))
                .andExpect(requisicao -> {
                    String corpo = corpo(requisicao);
                    assertTrue(corpo.contains("paulo@escola.test"));
                    assertTrue(corpo.contains("Senha12345"));
                    assertTrue(corpo.contains("nao-responda@maisedu.dev"));
                })
                .andRespond(withSuccess());

        service.enviarSenhaProvisoria(PROFESSOR_ID, "Paulo", "paulo@escola.test", "Senha12345");

        resend.verify();
        verify(auditoriaService).registrar(PROFESSOR_ID, "Paulo", null, AcaoAuditoria.ENVIO_EMAIL, "Usuario",
                PROFESSOR_ID, true, "Senha provisória enviada para paulo@escola.test");
        verify(usuarioRepository).registrarEnvioEmailAcesso(eq(PROFESSOR_ID), eq(StatusEmailAcesso.ENVIADO),
                any(LocalDateTime.class));
    }

    @Test
    void comChave_quandoOResendFalha_marcaComoFalhou() {
        ResendEmailService service = comResendSimulado();
        resend.expect(requestTo("https://api.resend.com/emails")).andRespond(withServerError());

        service.enviarSenhaProvisoria(PROFESSOR_ID, "Paulo", "paulo@escola.test", "Senha12345");

        resend.verify();
        verify(auditoriaService).registrar(eq(PROFESSOR_ID), eq("Paulo"), isNull(), eq(AcaoAuditoria.ENVIO_EMAIL),
                eq("Usuario"), eq(PROFESSOR_ID), eq(false), startsWith("Falha no envio"));
        verify(usuarioRepository).registrarEnvioEmailAcesso(eq(PROFESSOR_ID), eq(StatusEmailAcesso.FALHOU),
                any(LocalDateTime.class));
    }

    @Test
    void acessoAoResponsavel_levaSoOPrimeiroNomeEAAuditoriaNaoGuardaOEmail() {
        ResendEmailService service = comResendSimulado();
        resend.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(requisicao -> {
                    String corpo = corpo(requisicao);
                    assertTrue(corpo.contains("responsavel@familia.test"));
                    assertTrue(corpo.contains("Ana"));
                    assertFalse(corpo.contains("Souza"), "o e-mail ao responsável usa só o primeiro nome");
                    assertTrue(corpo.contains("2026001"));
                    assertTrue(corpo.contains("https://maisedu.dev/privacidade"));
                    assertFalse(corpo.contains("maisedu.dev//"), "a barra final do endereço é removida");
                })
                .andRespond(withSuccess());

        service.enviarAcessoAoResponsavel(ALUNO_ID, "Ana Souza", "2026001", "Escola Modelo",
                "responsavel@familia.test", "Senha12345");

        resend.verify();
        verify(auditoriaService).registrar(ALUNO_ID, "Ana Souza", null, AcaoAuditoria.ENVIO_EMAIL, "Usuario",
                ALUNO_ID, true, "Acesso do aluno enviado ao e-mail do responsável");
        verify(usuarioRepository).registrarEnvioEmailAcesso(eq(ALUNO_ID), eq(StatusEmailAcesso.ENVIADO),
                any(LocalDateTime.class));
    }
}
