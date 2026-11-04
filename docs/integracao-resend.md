# Integração com API externa — Resend

## O que é e por que foi escolhida

O [Resend](https://resend.com) é um serviço de envio de e-mail transacional (API REST). É usado no
MaisEdu para entregar a senha provisória a professores e administradores, sem que ela precise
trafegar na resposta HTTP, em dois fluxos:

- **cadastro** de um professor (`POST /professores`);
- **redefinição de senha** feita pelo admin (`POST /usuarios/{id}/redefinir-senha`), que também serve
  para reenviar o acesso quando o primeiro e-mail não chegou.

Antes desta integração, `POST /professores` devolvia a senha provisória em texto puro no corpo da
resposta (`UsuarioCadastradoResponse.senhaProvisoria`), e quem cadastrou repassava manualmente ao
professor. Isso funcionava, mas expunha a senha num lugar desnecessário (logs de proxy, histórico do
navegador de quem fez a chamada, etc.). Com o Resend, a senha só existe em texto puro no e-mail
recebido pelo próprio professor — a resposta HTTP não a traz mais.

O aluno **não é afetado por esta integração**: por decisão de minimização de dados (LGPD), o aluno não
tem e-mail cadastrado no sistema, então a senha provisória dele continua sendo devolvida na resposta
do cadastro (`POST /alunos`) para o professor repassar fora do sistema.

## Onde a integração vive no código

| Peça | Arquivo | Papel |
|---|---|---|
| Cliente HTTP | `service/ResendEmailService.java` | Monta a chamada REST ao Resend e trata sucesso/falha |
| Disparo | `service/UsuarioService.java` (`cadastrarProfessor`) e `service/GestaoUsuarioService.java` (`redefinirSenha`) | Pedem o envio dentro da transação; o envio real acontece **depois do commit** |
| Configuração | `application.properties` + `.env` | `maisedu.resend.api-key`, `maisedu.resend.remetente` |
| Auditoria | `AcaoAuditoria.ENVIO_EMAIL` | Cada tentativa de envio (sucesso ou falha) vira uma linha em `log_auditoria` |

Não foi adicionado nenhum SDK do Resend ao `pom.xml`: a chamada é feita com o `RestClient` do próprio
Spring (`org.springframework.web.client.RestClient`, disponível via `spring-boot-starter-webmvc`), que
já é suficiente para uma chamada REST simples e evita mais uma dependência externa no projeto.

## Chamada feita

```
POST https://api.resend.com/emails
Authorization: Bearer ${RESEND_API_KEY}
Content-Type: application/json

{
  "from": "MaisEdu <onboarding@resend.dev>",
  "to": ["professor@escola.test"],
  "subject": "Bem-vindo(a) ao MaisEdu — sua senha de acesso",
  "html": "<p>Olá, ...</p>..."
}
```

- **from**: por padrão usa `onboarding@resend.dev` — domínio de teste do próprio Resend. **Atenção:**
  com esse remetente o Resend só entrega para o e-mail dono da conta Resend; qualquer outro
  destinatário recebe erro 403 ("You can only send testing emails to your own email address").
  Para enviar a professores reais, verifique um domínio em <https://resend.com/domains> e defina
  `RESEND_REMETENTE` com um endereço desse domínio, sem alterar código.
- **to**: sempre um único destinatário — o e-mail do professor recém-cadastrado.
- Referência oficial do endpoint: <https://resend.com/docs/api-reference/emails/send-email>.

## Configuração (`.env`)

```
RESEND_API_KEY=re_xxx...        # obrigatória para o envio real acontecer
RESEND_REMETENTE=                # opcional; sem preencher, usa onboarding@resend.dev
```

## Comportamento em caso de falha

O envio de e-mail **nunca derruba o cadastro do professor**. Se `RESEND_API_KEY` não estiver
configurada, ou a chamada ao Resend falhar (rede, credencial inválida, limite de taxa etc.):

1. O professor é cadastrado normalmente no banco.
2. Um `log.warn(...)` é emitido no log da aplicação.
3. Uma linha malsucedida (`sucesso = false`) é gravada em `log_auditoria` com `acao = ENVIO_EMAIL`,
   visível ao admin na tela de Auditoria.
4. O admin usa **Usuários → Nova senha** para gerar outra senha provisória e reenviar o e-mail
   (as sessões antigas da conta são encerradas).

No perfil `dev` (`maisedu.email.registrar-no-log-sem-resend=true`), sem `RESEND_API_KEY` o conteúdo do
e-mail, inclusive a senha provisória, é escrito no log — só para demonstração local.

## Envio depois do commit

`ResendEmailService` registra o envio como `TransactionSynchronization.afterCommit()` quando há
transação ativa. Assim, (a) não sai e-mail com senha de um cadastro que acabou desfeito por rollback
e (b) a chamada HTTP ao Resend não mantém a transação do banco aberta. A auditoria do envio usa uma
transação própria, como as demais.

Essa decisão segue o mesmo padrão já usado para a gravação de auditoria em si
(`AuditoriaService`): uma falha num recurso acessório (e-mail, log) não pode impedir a operação de
negócio principal (cadastrar o professor).

## Teste manual

1. Configurar `RESEND_API_KEY` no `.env` com uma key válida de uma conta Resend.
2. Subir a aplicação e, autenticado como admin, chamar `POST /professores` com um e-mail de destino
   real (ou o e-mail de teste da própria conta Resend, se o domínio de envio ainda não estiver
   verificado — o Resend restringe o destinatário a esse e-mail em contas não verificadas).
3. Conferir o recebimento do e-mail e, no banco, `select * from log_auditoria where acao = 'ENVIO_EMAIL' order by id desc;`.
