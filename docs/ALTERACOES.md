# Alterações desta versão (entrega de 09/11)

Funcionalidades entregues, onde estão no código, correções, mudanças no banco e as decisões técnicas
tomadas onde a especificação da monografia deixava margem.

## 1. Funcionalidades novas

| Item | O que foi feito | Onde |
|---|---|---|
| RF06 | Cadastro de turma (nome único na instituição), listagem com inativas, desativar/reativar | `TurmaService`, `TurmaController`, tela **Turmas** |
| RF13 | Tela de consulta da auditoria com filtros e paginação | `ConsultarAuditoria.jsx` |
| RF15 | Atividade com questões APROVADAS do banco compartilhado, na ordem escolhida; busca no banco por tópico, nível e ano | `Atividade`, `AtividadeService`, `GET /questoes/aprovadas`, tela **Atividades** |
| RF16 / RN03 / RN04 | Iniciar (snapshot das questões), reabrir com a mesma folha, responder uma única vez, correção automática, conclusão automática | `AtividadeAluno`, `AtividadeAlunoQuestao`, `Resposta`, `RespostaService`, tela **Minhas Atividades** |
| RF17 / RN05 | Desempenho acumulado por aluno + tópico + nível, na mesma transação da resposta | `DesempenhoTopico`, `DesempenhoService.registrarResposta` |
| RF18 / RN06 | Prática adaptativa (regra determinística, parâmetros configuráveis) | `MotorAdaptativo` (classe pura), `PraticaAdaptativaService` |
| RF19 | Painel de desempenho (dashboard) do aluno e da turma: indicadores em destaque (alunos, atividades, respostas, acerto, tópico mais fraco, níveis dominados), gráfico de barras de acerto por tópico, gráfico de evolução semanal e detalhe por tópico e nível; o painel é a tela inicial de professores, administradores e alunos | `DesempenhoService.consultarTurma`, `DesempenhoTurmaResponse`, `GraficoBarras`, `GraficoEvolucao`, `CartaoNumero`, telas **Painel de Desempenho** |
| Ano escolar | A questão passa a ter ano escolar (o Resumo e o objetivo (c) já falavam disso) | `Questao.anoEscolar`, migration V8, tela **Criar Questão** |
| Redefinição de senha | Admin redefine a senha de qualquer usuário da instituição (professor recebe por e-mail); professor redefine a dos alunos das suas turmas (senha exibida para repassar) | `GestaoUsuarioService.redefinirSenha`, tela **Usuários** / **Meus Alunos** |
| LGPD (art. 18) | Exportar dados (JSON), corrigir nome, desativar/reativar, anonimizar | `GestaoUsuarioService`, `GestaoUsuarioController` |
| Termos versionados | `maisedu.termos.versao-atual`; versão nova exige novo aceite antes de qualquer ação | `AutenticacaoService.aceitarTermos`, `JwtAuthenticationFilter`, tela **AceitarTermos** |
| Retenção | Rotina diária apaga logs com mais de 365 dias | `RetencaoAuditoriaService` |
| Demonstração | Perfil `dev` cria duas escolas, professores, turmas, alunos, 96 questões aprovadas e respostas | `SeedDemonstracao`, `GeradorQuestoesDemo` |
| Docker | `docker compose up --build` sobe banco, back-end e front-end (nginx com proxy `/api`) | `docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile` |

| Navegação com React Router | Cada tela tem a sua URL; rotas públicas (login, Termos, Política), etapas obrigatórias (troca de senha, novo aceite) e área logada protegida por perfil; botão voltar e F5 funcionam; página "não encontrada" | `App.jsx`, `navegacao.js`, `Layout.jsx`, `PaginaLegal.jsx` |
| Interface com Bootstrap 5 | Barra superior, menu lateral por perfil (recolhível no celular), formulários, tabelas, alertas, listas de seleção e cartões no padrão do Bootstrap; telas de acesso centralizadas | `main.jsx`, `index.css`, `ui.css` e telas em `pages/` |
| Validações no front | Mesmas regras do back-end antes do envio: login do aluno (3 a 50 caracteres, sem acento), nova senha com 8+ caracteres e diferente da atual (RF02, CA03), nome sem espaços em branco; mensagens de erro e sucesso padronizadas | `CadastrarAluno`, `CadastrarProfessor`, `TrocarSenha`, `Mensagens` |

## 2. Correções

| Problema | Correção |
|---|---|
| Admin via auditoria de outras instituições | `log_auditoria.instituicao_id` (V7, com preenchimento dos registros antigos) e consulta sempre filtrada pela instituição do admin |
| Senha do professor se perdia se o e-mail falhasse | Redefinição/reenvio pelo admin; e-mail enviado só depois do commit; no perfil `dev`, conteúdo no log |
| Logout/desativação não derrubavam o token (valia por 120 min) | `usuario.versao_sessao`: o token carrega a versão e o filtro confere no banco a cada requisição |
| "Conta desativada" e e-mail inexistente revelavam a conta | Conta desativada só é informada a quem acerta a senha; identificador inexistente compara com um hash fictício (mesmo tempo de resposta) |
| E-mail/login diferenciavam maiúsculas | Normalização (trim + minúsculas) no cadastro e no login; CHECKs no banco; login de aluno sem "@" |
| Corrida em cadastro virava HTTP 500 | Handler para `DataIntegrityViolationException` (409) e para os demais erros, sempre com `{"mensagem": ...}` |
| Falha ao gravar log podia desfazer a operação de negócio | O `try/catch` da auditoria agora fica fora da transação independente (`TransactionTemplate` REQUIRES_NEW) |
| IP da auditoria vinha de `X-Forwarded-For` (forjável) | Usa `getRemoteAddr()`; atrás do nginx, `server.forward-headers-strategy=native` |
| `localhost` fixo (banco, CORS, front) | `DB_URL`/`DB_USERNAME`, `CORS_ORIGENS`, front sempre em `/api` |
| `.env` não era lido e tinha `RESEND_REMETENT` | `spring.config.import=optional:file:.env[.properties]` e nome corrigido |
| `AlternativaResponse` expõe a correta | Tela do aluno usa `FolhaAtividadeResponse`, que só mostra o gabarito depois da resposta |
| Front não reagia ao 401 | Evento global de sessão expirada → volta ao login com aviso |
| Consulta da auditoria dava erro 500 no PostgreSQL | O filtro `:de is null` deixava o parâmetro de data sem tipo no PostgreSQL (o H2 dos testes aceitava); o período agora vai sempre preenchido pelo `AuditoriaService` e a consulta só compara as datas (`AuditoriaServiceConsultaTest`) |
| Política/Termos prometiam o que o sistema não fazia | Textos reescritos conforme o funcionamento real (seção 4 da Política: Resend como transferência internacional e autoria no banco compartilhado) |

## 3. Banco de dados

- **V7** — `log_auditoria.instituicao_id`; normalização de e-mail/login; `usuario.versao_sessao`, `termos_versao`, `anonimizado_em`.
- **V8** — `questao.ano_escolar`; CHECKs de status e ano; tabelas `atividade`, `atividade_questao`, `atividade_aluno`, `atividade_aluno_questao`, `resposta`, `desempenho_topico`; trigger que garante no banco que a alternativa da resposta pertence à questão entregue (RN04).

## 4. Decisões tomadas onde a monografia deixava margem

1. **Prática em andamento é retomada.** Se o aluno já tem uma prática adaptativa não concluída, pedir outra devolve a mesma (evita acumular práticas e "trocar" uma prática difícil).
2. **Seleção das questões (RN06, passo c).** Em cada tópico: primeiro o nível-alvo (inéditas antes das já respondidas), depois os níveis adjacentes (FÁCIL→MÉDIO; MÉDIO→FÁCIL, DIFÍCIL; DIFÍCIL→MÉDIO). Os tópicos são intercalados a partir do mais fraco (10 vagas em 3 tópicos = 4/3/3). Vagas que um tópico não preencher ficam para os outros. Se houver menos de 10 questões compatíveis, a prática sai menor; com zero, não é gerada (CA03).
3. **Empate no ranking dos tópicos:** menor percentual; empate → tópico com mais respostas; depois o id.
4. **Admin e turmas:** o admin vê resultados e desempenho de qualquer turma da instituição, mas só cria atividade em turma em que está vinculado como professor (RF15 diz "professor que leciona na turma").
5. **Logout encerra todas as sessões do usuário** (todos os dispositivos), porque a revogação é por versão.
6. **Anonimização** não se aplica a administradores (a instituição não pode ficar sem gestor) e mantém respostas/desempenho sem identificação.
7. **Bloqueio por tentativas** continua informando o tempo restante antes de conferir a senha (CA03 do RF01).
8. **Ano escolar** é obrigatório para questões novas e nulo nas criadas antes da V8.
9. **Painel da turma usa o histórico completo de cada aluno** (RN05): os indicadores, o acerto por tópico e
   nível e a evolução semanal somam os registros de desempenho e as respostas dos alunos vinculados, em
   qualquer atividade ou prática. Assim o painel da turma e o painel individual mostram os mesmos números
   que o motor adaptativo usa (RF19, CA03).
11. **Bootstrap só com CSS.** O menu do celular abre e fecha pelo estado do React, sem o JavaScript do
    Bootstrap, o que evita dependências extras e conflitos com o React.
12. **Termos e Política em nova aba nas telas de formulário** (troca de senha e novo aceite), para que o
    usuário consulte os documentos sem perder o que já digitou.
10. **Gráficos sem biblioteca externa.** Barras e linha são componentes próprios (HTML/CSS e SVG), o que
    mantém o front com só duas dependências de execução (React e React DOM).

## 5. Como verificar

```bash
cd backend && ./mvnw test        # testes unitários (JUnit 5 + Mockito) e de integração (Spring Boot Test + H2)
docker compose up --build        # sobe banco, back-end e front-end; o Hibernate valida o schema contra as migrations
```

- Os testes unitários cobrem as regras RN01 a RN06, autenticação, sessão, LGPD, o motor adaptativo e o
  painel da turma (`DesempenhoServiceTest`).
- Os testes de integração exercitam os fluxos HTTP completos (login, questões e quórum, turma → atividade →
  respostas → desempenho → painel → prática adaptativa, revogação de sessão, isolamento da auditoria).
- Ressalva: a integração usa H2 com o Flyway desligado; as restrições específicas do PostgreSQL (EXCLUDE,
  trigger da RN04, CHECKs) são conferidas ao subir com `docker compose`, sobre as migrations reais.
- Com o perfil `dev`, o cenário de demonstração (README) traz respostas em semanas diferentes, o que
  preenche os gráficos do painel.
