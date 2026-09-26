# Integração com API externa — Resend

## O que é e por que foi escolhida

O [Resend](https://resend.com) é um serviço de envio de e-mail transacional (API REST). É usado no
MaisEdu para um único fluxo: **entregar a senha provisória a um professor ou administrador
recém-cadastrado**, sem que ela precise trafegar na resposta HTTP do cadastro.

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
| Disparo | `service/UsuarioService.java` (`cadastrarProfessor`) | Chama o envio logo após salvar o novo professor |
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

- **from**: por padrão usa `onboarding@resend.dev` — domínio de teste do próprio Resend, que funciona
  sem verificação de domínio próprio (suficiente para um projeto acadêmico). Pode ser trocado por um
  remetente com domínio verificado via `RESEND_REMETENTE` no `.env`, sem alterar código.
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
   permitindo ao admin perceber, pela tela de auditoria, que aquele professor não recebeu a senha por
   e-mail e precisa de outro canal (ex.: recadastro, ou uma futura funcionalidade de "esqueci minha
   senha", fora do escopo desta entrega).

Essa decisão segue o mesmo padrão já usado para a gravação de auditoria em si
(`AuditoriaService`): uma falha num recurso acessório (e-mail, log) não pode impedir a operação de
negócio principal (cadastrar o professor).

## Teste manual

1. Configurar `RESEND_API_KEY` no `.env` com uma key válida de uma conta Resend.
2. Subir a aplicação e, autenticado como admin, chamar `POST /professores` com um e-mail de destino
   real (ou o e-mail de teste da própria conta Resend, se o domínio de envio ainda não estiver
   verificado — o Resend restringe o destinatário a esse e-mail em contas não verificadas).
3. Conferir o recebimento do e-mail e, no banco, `select * from log_auditoria where acao = 'ENVIO_EMAIL' order by id desc;`.
