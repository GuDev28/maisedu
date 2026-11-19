package br.com.maisedu.app.service;

import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.StatusEmailAcesso;
import br.com.maisedu.app.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;


/**
 * E-mails transacionais via Resend: só senha provisória (cadastro e redefinição). Professores e
 * administradores recebem no próprio e-mail; o aluno não tem e-mail, e o acesso dele vai para o e-mail
 * do responsável, com o mínimo de dados do aluno (primeiro nome e login).
 *
 * Depois de cada tentativa, a situação do envio (enviado, falhou ou e-mail não configurado) fica
 * gravada no usuário, para o professor e o admin perceberem quando o acesso não chegou.
 */
@Service
@Slf4j
public class ResendEmailService {

    private final RestClient restClient;
    private final AuditoriaService auditoriaService;
    private final UsuarioRepository usuarioRepository;
    /** O envio acontece depois do commit do cadastro: a situação é gravada numa transação própria. */
    private final TransactionTemplate transacaoIndependente;
    private final String remetente;
    private final String enderecoSistema;
    private final boolean ativo;
    private final boolean registrarNoLogSemResend;

    public ResendEmailService(
            @Value("${maisedu.resend.api-key:}") String apiKey,
            @Value("${maisedu.resend.remetente}") String remetente,
            @Value("${maisedu.email.registrar-no-log-sem-resend:false}") boolean registrarNoLogSemResend,
            @Value("${maisedu.app-url:http://localhost:8081}") String enderecoSistema,
            AuditoriaService auditoriaService,
            UsuarioRepository usuarioRepository,
            PlatformTransactionManager transactionManager) {
        this.remetente = remetente;
        this.enderecoSistema = enderecoSistema.endsWith("/")
                ? enderecoSistema.substring(0, enderecoSistema.length() - 1)
                : enderecoSistema;
        this.auditoriaService = auditoriaService;
        this.usuarioRepository = usuarioRepository;
        this.transacaoIndependente = new TransactionTemplate(transactionManager);
        this.transacaoIndependente.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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
        enviarAposCommit(usuarioId, usuarioNome, destinatario, assunto, html, senhaProvisoria,
                "Senha provisória enviada para " + destinatario);
    }

    /** E-mail com a nova senha provisória gerada pela administração (redefinição de senha). */
    public void enviarNovaSenhaProvisoria(Long usuarioId, String usuarioNome, String destinatario, String senhaProvisoria) {
        String assunto = "MaisEdu — sua senha foi redefinida";
        String html = construirHtml(usuarioNome, destinatario, senhaProvisoria,
                "A administração da sua escola redefiniu a sua senha no MaisEdu. Use os dados abaixo no próximo acesso:");
        enviarAposCommit(usuarioId, usuarioNome, destinatario, assunto, html, senhaProvisoria,
                "Senha provisória enviada para " + destinatario);
    }

    /** Acesso de um aluno recém-cadastrado, enviado ao e-mail do responsável. */
    public void enviarAcessoAoResponsavel(Long alunoId, String nomeAluno, String login, String escola,
                                          String emailResponsavel, String senhaProvisoria) {
        String primeiroNome = primeiroNome(nomeAluno);
        String assunto = "MaisEdu — acesso de " + primeiroNome;
        String html = construirHtmlResponsavel(primeiroNome, escola, login, senhaProvisoria,
                "%s cadastrou %s no MaisEdu, a plataforma de prática de Matemática usada nas aulas."
                        .formatted(HtmlUtils.htmlEscape(escola), HtmlUtils.htmlEscape(primeiroNome)));
        enviarAposCommit(alunoId, nomeAluno, emailResponsavel, assunto, html, senhaProvisoria,
                "Acesso do aluno enviado ao e-mail do responsável");
    }

    /** Senha nova de um aluno (gerada pelo professor ou pelo admin), enviada ao e-mail do responsável. */
    public void enviarNovaSenhaAoResponsavel(Long alunoId, String nomeAluno, String login, String escola,
                                             String emailResponsavel, String senhaProvisoria) {
        String primeiroNome = primeiroNome(nomeAluno);
        String assunto = "MaisEdu — nova senha de " + primeiroNome;
        String html = construirHtmlResponsavel(primeiroNome, escola, login, senhaProvisoria,
                "%s gerou uma senha nova para %s no MaisEdu (por exemplo, porque a anterior foi esquecida)."
                        .formatted(HtmlUtils.htmlEscape(escola), HtmlUtils.htmlEscape(primeiroNome)));
        enviarAposCommit(alunoId, nomeAluno, emailResponsavel, assunto, html, senhaProvisoria,
                "Senha nova do aluno enviada ao e-mail do responsável");
    }
    /**
     * Se houver uma transação em andamento, o envio só acontece depois do commit: assim não sai
     * e-mail com senha de uma conta cujo cadastro acabou desfeito, e a chamada HTTP ao Resend não
     * segura a transação do banco aberta.
     */
    private void enviarAposCommit(Long usuarioId, String usuarioNome, String destinatario, String assunto,
                                  String html, String senhaProvisoria, String detalheSucesso) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviarAgora(usuarioId, usuarioNome, destinatario, assunto, html, senhaProvisoria, detalheSucesso);
                }
            });
        } else {
            enviarAgora(usuarioId, usuarioNome, destinatario, assunto, html, senhaProvisoria, detalheSucesso);
        }
    }

    /**
     * {@code detalheSucesso} vai para a auditoria. No e-mail do responsável ele não traz o endereço:
     * a auditoria registra que o envio aconteceu, sem copiar o dado de contato.
     */
    private void enviarAgora(Long usuarioId, String usuarioNome, String destinatario, String assunto,
                             String html, String senhaProvisoria, String detalheSucesso) {
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
            auditoriaService.registrar(usuarioId, usuarioNome, null, AcaoAuditoria.ENVIO_EMAIL,
                    "Usuario", usuarioId, false, "RESEND_API_KEY não configurada");
            registrarSituacao(usuarioId, StatusEmailAcesso.NAO_CONFIGURADO);
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

            auditoriaService.registrar(usuarioId, usuarioNome, null, AcaoAuditoria.ENVIO_EMAIL,
                    "Usuario", usuarioId, true, detalheSucesso);
            registrarSituacao(usuarioId, StatusEmailAcesso.ENVIADO);
        } catch (Exception ex) {
            log.warn("Falha ao enviar e-mail via Resend para {}", destinatario, ex);
            auditoriaService.registrar(usuarioId, usuarioNome, null, AcaoAuditoria.ENVIO_EMAIL,
                    "Usuario", usuarioId, false, "Falha no envio: " + ex.getMessage());
            registrarSituacao(usuarioId, StatusEmailAcesso.FALHOU);
        }
    }

    /** Grava a situação do envio sem nunca interromper quem chamou (o cadastro já foi confirmado). */
    private void registrarSituacao(Long usuarioId, StatusEmailAcesso situacao) {
        if (usuarioId == null) {
            return;
        }
        try {
            transacaoIndependente.executeWithoutResult(status ->
                    usuarioRepository.registrarEnvioEmailAcesso(usuarioId, situacao, LocalDateTime.now()));
        } catch (Exception ex) {
            log.warn("Não foi possível gravar a situação do e-mail de acesso (usuarioId={})", usuarioId, ex);
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

    private String construirHtmlResponsavel(String primeiroNome, String escola, String login, String senhaProvisoria,
                                            String introducao) {
        return """
                <p>Olá!</p>
                <p>%s</p>
                <p>Dados para %s entrar em <a href="%s">%s</a>:</p>
                <ul>
                    <li><strong>Login:</strong> %s</li>
                    <li><strong>Senha provisória:</strong> %s</li>
                </ul>
                <p>No primeiro acesso, o(a) estudante cria uma senha própria e lê os Termos de Uso e a
                Política de Privacidade.</p>
                <p>Você recebeu este e-mail porque a escola informou este endereço como o do(a) responsável.
                Ele é usado só para enviar o acesso e as senhas novas do(a) estudante. Como os dados são
                tratados: <a href="%s/privacidade">Política de Privacidade</a>. Dúvidas ou pedidos sobre os
                dados: fale com %s.</p>
                <p>Se você não é responsável por este(a) estudante, ignore esta mensagem e avise a escola.</p>
                """.formatted(introducao, HtmlUtils.htmlEscape(primeiroNome), enderecoSistema, enderecoSistema,
                HtmlUtils.htmlEscape(login), HtmlUtils.htmlEscape(senhaProvisoria), enderecoSistema,
                HtmlUtils.htmlEscape(escola));
    }

    /** Minimização: o e-mail ao responsável usa só o primeiro nome do aluno. */
    private static String primeiroNome(String nomeCompleto) {
        String nome = nomeCompleto == null ? "" : nomeCompleto.strip();
        int espaco = nome.indexOf(' ');
        return espaco < 0 ? nome : nome.substring(0, espaco);
    }
}
