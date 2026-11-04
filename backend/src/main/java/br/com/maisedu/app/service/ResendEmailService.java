package br.com.maisedu.app.service;

import br.com.maisedu.app.model.AcaoAuditoria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class ResendEmailService {

    private final RestClient restClient;
    private final AuditoriaService auditoriaService;
    private final String remetente;
    private final boolean ativo;
    private final boolean registrarNoLogSemResend;

    public ResendEmailService(
            @Value("${maisedu.resend.api-key:}") String apiKey,
            @Value("${maisedu.resend.remetente}") String remetente,
            @Value("${maisedu.email.registrar-no-log-sem-resend:false}") boolean registrarNoLogSemResend,
            AuditoriaService auditoriaService) {
        this.remetente = remetente;
        this.auditoriaService = auditoriaService;
        this.ativo = apiKey != null && !apiKey.isBlank();
        this.registrarNoLogSemResend = registrarNoLogSemResend;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + (apiKey == null ? "" : apiKey))
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** E-mail de boas-vindas com a senha provisória de um professor/admin recém-cadastrado. */
    public void enviarSenhaProvisoria(Long usuarioId, String usuarioNome, String destinatario, String senhaProvisoria) {
        String assunto = "Bem-vindo(a) ao MaisEdu — sua senha de acesso";
        String html = construirHtml(usuarioNome, destinatario, senhaProvisoria,
                "Sua conta no MaisEdu foi criada. Use os dados abaixo no primeiro acesso:");
        enviarAposCommit(usuarioId, usuarioNome, destinatario, assunto, html, senhaProvisoria);
    }

    /** E-mail com a nova senha provisória gerada pela administração (redefinição de senha). */
    public void enviarNovaSenhaProvisoria(Long usuarioId, String usuarioNome, String destinatario, String senhaProvisoria) {
        String assunto = "MaisEdu — sua senha foi redefinida";
        String html = construirHtml(usuarioNome, destinatario, senhaProvisoria,
                "A administração da sua escola redefiniu a sua senha no MaisEdu. Use os dados abaixo no próximo acesso:");
        enviarAposCommit(usuarioId, usuarioNome, destinatario, assunto, html, senhaProvisoria);
    }

    /**
     * Se houver uma transação em andamento, o envio só acontece depois do commit: assim não sai
     * e-mail com senha de uma conta cujo cadastro acabou desfeito, e a chamada HTTP ao Resend não
     * segura a transação do banco aberta.
     */
    private void enviarAposCommit(Long usuarioId, String usuarioNome, String destinatario, String assunto,
                                  String html, String senhaProvisoria) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviarAgora(usuarioId, usuarioNome, destinatario, assunto, html, senhaProvisoria);
                }
            });
        } else {
            enviarAgora(usuarioId, usuarioNome, destinatario, assunto, html, senhaProvisoria);
        }
    }

    private void enviarAgora(Long usuarioId, String usuarioNome, String destinatario, String assunto,
                             String html, String senhaProvisoria) {
        if (!ativo) {
            if (registrarNoLogSemResend) {
                log.warn("""

                        [DEV] RESEND_API_KEY não configurada — e-mail NÃO enviado. Conteúdo:
                          para: {}
                          assunto: {}
                          senha provisória: {}
                        """, destinatario, assunto, senhaProvisoria);
            } else {
                log.warn("RESEND_API_KEY não configurada: e-mail para {} não foi enviado.", destinatario);
            }
            auditoriaService.registrar(usuarioId, usuarioNome, AcaoAuditoria.ENVIO_EMAIL,
                    "Usuario", usuarioId, false, "RESEND_API_KEY não configurada");
            return;
        }

        try {
            restClient.post()
                    .uri("/emails")
                    .body(Map.of(
                            "from", remetente,
                            "to", List.of(destinatario),
                            "subject", assunto,
                            "html", html))
                    .retrieve()
                    .toBodilessEntity();

            auditoriaService.registrar(usuarioId, usuarioNome, AcaoAuditoria.ENVIO_EMAIL,
                    "Usuario", usuarioId, true, "Senha provisória enviada para " + destinatario);
        } catch (Exception ex) {
            log.warn("Falha ao enviar e-mail via Resend para {}", destinatario, ex);
            auditoriaService.registrar(usuarioId, usuarioNome, AcaoAuditoria.ENVIO_EMAIL,
                    "Usuario", usuarioId, false, "Falha no envio: " + ex.getMessage());
        }
    }

    private String construirHtml(String nome, String identificador, String senhaProvisoria, String introducao) {
        return """
                <p>Olá, %s!</p>
                <p>%s</p>
                <ul>
                    <li><strong>Login:</strong> %s</li>
                    <li><strong>Senha provisória:</strong> %s</li>
                </ul>
                <p>No acesso você será obrigado(a) a trocar essa senha e a aceitar
                os Termos de Uso e a Política de Privacidade do sistema.</p>
                <p>Se você não esperava este e-mail, ignore esta mensagem.</p>
                """.formatted(HtmlUtils.htmlEscape(nome), introducao, HtmlUtils.htmlEscape(identificador),
                HtmlUtils.htmlEscape(senhaProvisoria));
    }
}
