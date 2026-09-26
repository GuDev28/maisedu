package br.com.maisedu.app.service;

import br.com.maisedu.app.model.AcaoAuditoria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class ResendEmailService {

    private final RestClient restClient;
    private final AuditoriaService auditoriaService;
    private final String remetente;
    private final boolean ativo;

    public ResendEmailService(
            @Value("${maisedu.resend.api-key:}") String apiKey,
            @Value("${maisedu.resend.remetente}") String remetente,
            AuditoriaService auditoriaService) {
        this.remetente = remetente;
        this.auditoriaService = auditoriaService;
        this.ativo = apiKey != null && !apiKey.isBlank();
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + (apiKey == null ? "" : apiKey))
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void enviarSenhaProvisoria(Long usuarioId, String usuarioNome, String destinatario, String senhaProvisoria) {
        if (!ativo) {
            log.warn("RESEND_API_KEY não configurada: e-mail de senha provisória para {} não foi enviado.", destinatario);
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
                            "subject", "Bem-vindo(a) ao MaisEdu — sua senha de acesso",
                            "html", construirHtmlSenhaProvisoria(usuarioNome, destinatario, senhaProvisoria)))
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

    private String construirHtmlSenhaProvisoria(String nome, String identificador, String senhaProvisoria) {
        return """
                <p>Olá, %s!</p>
                <p>Sua conta no MaisEdu foi criada. Use os dados abaixo no primeiro acesso:</p>
                <ul>
                    <li><strong>Login:</strong> %s</li>
                    <li><strong>Senha provisória:</strong> %s</li>
                </ul>
                <p>No primeiro acesso você será obrigado(a) a trocar essa senha e a aceitar
                os Termos de Uso e a Política de Privacidade do sistema.</p>
                <p>Se você não esperava este e-mail, ignore esta mensagem.</p>
                """.formatted(nome, identificador, senhaProvisoria);
    }
}
