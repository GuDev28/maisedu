# Alterações desta versão

Resumo do que foi implementado a partir da análise do projeto, onde está no código, as decisões
tomadas onde a monografia deixava margem e o que foi (e o que ainda precisa ser) verificado.

## 1. Funcionalidades novas

| Item | O que foi feito | Onde |
|---|---|---|
| RF06 | Cadastro de turma (nome único na instituição), listagem com inativas, desativar/reativar | `TurmaService`, `TurmaController`, tela **Turmas** |
| RF13 | Tela de consulta da auditoria com filtros e paginação | `ConsultarAuditoria.jsx` |
| RF15 | Atividade com questões APROVADAS do banco compartilhado, na ordem escolhida; busca no banco por tópico, nível e ano | `Atividade`, `AtividadeService`, `GET /questoes/aprovadas`, tela **Atividades** |
| RF16 / RN03 / RN04 | Iniciar (snapshot das questões), reabrir com a mesma folha, responder uma única vez, correção automática, conclusão automática | `AtividadeAluno`, `AtividadeAlunoQuestao`, `Resposta`, `RespostaService`, tela **Minhas Atividades** |
| RF17 / RN05 | Desempenho acumulado por aluno + tópico + nível, na mesma transação da resposta | `DesempenhoTopico`, `DesempenhoService.registrarResposta` |
| RF18 / RN06 | Prática adaptativa (regra determinística, parâmetros configuráveis) | `MotorAdaptativo` (classe pura), `PraticaAdaptativaService` |
| RF19 | Painel do aluno, painel da turma e de cada aluno para o professor, evolução semanal | `DesempenhoService`, telas **Meu Desempenho** e **Desempenho da Turma** |
| Ano escolar | A questão passa a ter ano escolar (o Resumo e o objetivo (c) já falavam disso) | `Questao.anoEscolar`, migration V8, tela **Criar Questão** |
| Redefinição de senha | Admin redefine a senha de qualquer usuário da instituição (professor recebe por e-mail); professor redefine a dos alunos das suas turmas (senha exibida para repassar) | `GestaoUsuarioService.redefinirSenha`, tela **Usuários** / **Meus Alunos** |
| LGPD (art. 18) | Exportar dados (JSON), corrigir nome, desativar/reativar, anonimizar | `GestaoUsuarioService`, `GestaoUsuarioController` |
| Termos versionados | `maisedu.termos.versao-atual`; versão nova exige novo aceite antes de qualquer ação | `AutenticacaoService.aceitarTermos`, `JwtAuthenticationFilter`, tela **AceitarTermos** |
| Retenção | Rotina diária apaga logs com mais de 365 dias | `RetencaoAuditoriaService` |
| Demonstração | Perfil `dev` cria duas escolas, professores, turmas, alunos, 96 questões aprovadas e respostas | `SeedDemonstracao`, `GeradorQuestoesDemo` |
| Docker | `docker compose up --build` sobe banco, back-end e front-end (nginx com proxy `/api`) | `docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile` |

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

## 5. Como foi verificado

Neste ambiente o Maven Central estava bloqueado, então **não foi possível compilar com as bibliotecas reais nem rodar `./mvnw test`**. O que foi feito:

- **Checagem de tipos** de todo o código Java (main e test) com `javac` contra stubs das APIs de Spring/JPA/JUnit/Mockito e uma expansão do Lombok — zero erros.
- **34 testes unitários puros executados** (motor adaptativo, desempenho, snapshot, resposta, sessão/LGPD do usuário) — todos passaram.
- **Migrations V1–V8 aplicadas em PostgreSQL 16 real**, inclusive sobre dados no formato antigo; CHECKs, UNIQUEs e o trigger da RN04 exercitados com inserções inválidas; SQL de datas do seed testado.
- **Gerador de questões executado**: 96 questões, 5 alternativas distintas cada, gabaritos conferidos.
- **Front-end:** bundle com esbuild e navegação completa em Chromium headless contra uma API simulada (admin, professor e aluno), sem erros de execução; layout conferido também em largura de celular.

**Falta verificar na máquina de vocês:**

```bash
cd backend && ./mvnw test           # unitários com Mockito + integração com H2
docker compose up --build           # sobe tudo; o Hibernate valida o schema contra as migrations
```

Se algo falhar, os pontos mais prováveis são detalhes de API que os stubs não reproduzem (por exemplo, assinatura exata de algum método do Spring 7/Hibernate 7) — o restante da lógica já foi exercitado.

## 6. Arquivos (em relação ao zip original)

**Novos (95):**

- `.env.example`
- `backend/.dockerignore`
- `backend/Dockerfile`
- `backend/src/main/java/br/com/maisedu/app/config/AgendamentoConfig.java`
- `backend/src/main/java/br/com/maisedu/app/config/GeradorQuestoesDemo.java`
- `backend/src/main/java/br/com/maisedu/app/config/SeedDemonstracao.java`
- `backend/src/main/java/br/com/maisedu/app/controller/AlunoController.java`
- `backend/src/main/java/br/com/maisedu/app/controller/AtividadeController.java`
- `backend/src/main/java/br/com/maisedu/app/controller/DesempenhoController.java`
- `backend/src/main/java/br/com/maisedu/app/controller/GestaoUsuarioController.java`
- `backend/src/main/java/br/com/maisedu/app/dto/AceiteTermosRequest.java`
- `backend/src/main/java/br/com/maisedu/app/dto/AtividadeAlunoResumoResponse.java`
- `backend/src/main/java/br/com/maisedu/app/dto/AtividadeResumoResponse.java`
- `backend/src/main/java/br/com/maisedu/app/dto/CadastroTurmaRequest.java`
- `backend/src/main/java/br/com/maisedu/app/dto/CorrecaoNomeRequest.java`
- `backend/src/main/java/br/com/maisedu/app/dto/CriarAtividadeRequest.java`
- `backend/src/main/java/br/com/maisedu/app/dto/DadosPessoaisResponse.java`
- `backend/src/main/java/br/com/maisedu/app/dto/DesempenhoAlunoResponse.java`
- `backend/src/main/java/br/com/maisedu/app/dto/DesempenhoTurmaResponse.java`
- `backend/src/main/java/br/com/maisedu/app/dto/FolhaAtividadeResponse.java`
- `backend/src/main/java/br/com/maisedu/app/dto/PraticaAdaptativaResponse.java`
- `backend/src/main/java/br/com/maisedu/app/dto/ResponderQuestaoRequest.java`
- `backend/src/main/java/br/com/maisedu/app/dto/RespostaHistorico.java`
- `backend/src/main/java/br/com/maisedu/app/dto/RespostaResultadoResponse.java`
- `backend/src/main/java/br/com/maisedu/app/dto/ResultadoAtividadeResponse.java`
- `backend/src/main/java/br/com/maisedu/app/dto/SenhaRedefinidaResponse.java`
- `backend/src/main/java/br/com/maisedu/app/dto/TurmaGestaoResponse.java`
- `backend/src/main/java/br/com/maisedu/app/dto/UsuarioGestaoResponse.java`
- `backend/src/main/java/br/com/maisedu/app/exception/RecursoNaoEncontradoException.java`
- `backend/src/main/java/br/com/maisedu/app/model/Atividade.java`
- `backend/src/main/java/br/com/maisedu/app/model/AtividadeAluno.java`
- `backend/src/main/java/br/com/maisedu/app/model/AtividadeAlunoQuestao.java`
- `backend/src/main/java/br/com/maisedu/app/model/AtividadeQuestao.java`
- `backend/src/main/java/br/com/maisedu/app/model/DesempenhoTopico.java`
- `backend/src/main/java/br/com/maisedu/app/model/Resposta.java`
- `backend/src/main/java/br/com/maisedu/app/model/TipoAtividade.java`
- `backend/src/main/java/br/com/maisedu/app/repository/AlternativaRepository.java`
- `backend/src/main/java/br/com/maisedu/app/repository/AtividadeAlunoQuestaoRepository.java`
- `backend/src/main/java/br/com/maisedu/app/repository/AtividadeAlunoRepository.java`
- `backend/src/main/java/br/com/maisedu/app/repository/AtividadeRepository.java`
- `backend/src/main/java/br/com/maisedu/app/repository/DesempenhoTopicoRepository.java`
- `backend/src/main/java/br/com/maisedu/app/repository/RespostaRepository.java`
- `backend/src/main/java/br/com/maisedu/app/security/EstadoSessao.java`
- `backend/src/main/java/br/com/maisedu/app/service/AcessoTurma.java`
- `backend/src/main/java/br/com/maisedu/app/service/AtividadeService.java`
- `backend/src/main/java/br/com/maisedu/app/service/DesempenhoService.java`
- `backend/src/main/java/br/com/maisedu/app/service/GestaoUsuarioService.java`
- `backend/src/main/java/br/com/maisedu/app/service/Identificadores.java`
- `backend/src/main/java/br/com/maisedu/app/service/MotorAdaptativo.java`
- `backend/src/main/java/br/com/maisedu/app/service/PraticaAdaptativaService.java`
- `backend/src/main/java/br/com/maisedu/app/service/RespostaService.java`
- `backend/src/main/java/br/com/maisedu/app/service/RetencaoAuditoriaService.java`
- `backend/src/main/java/br/com/maisedu/app/service/TurmaService.java`
- `backend/src/main/resources/application-dev.properties`
- `backend/src/main/resources/db/migration/V7__auditoria_instituicao_sessao_lgpd.sql`
- `backend/src/main/resources/db/migration/V8__atividades_respostas_desempenho.sql`
- `backend/src/test/java/br/com/maisedu/app/controller/FluxoAtividadeIntegrationTest.java`
- `backend/src/test/java/br/com/maisedu/app/controller/SegurancaSessaoIntegrationTest.java`
- `backend/src/test/java/br/com/maisedu/app/model/AtividadeTest.java`
- `backend/src/test/java/br/com/maisedu/app/model/DesempenhoTopicoTest.java`
- `backend/src/test/java/br/com/maisedu/app/model/RespostaTest.java`
- `backend/src/test/java/br/com/maisedu/app/model/UsuarioSessaoELgpdTest.java`
- `backend/src/test/java/br/com/maisedu/app/service/AtividadeServiceTest.java`
- `backend/src/test/java/br/com/maisedu/app/service/EvolucaoDesempenhoTest.java`
- `backend/src/test/java/br/com/maisedu/app/service/GestaoUsuarioServiceTest.java`
- `backend/src/test/java/br/com/maisedu/app/service/MotorAdaptativoTest.java`
- `backend/src/test/java/br/com/maisedu/app/service/PraticaAdaptativaServiceTest.java`
- `backend/src/test/java/br/com/maisedu/app/service/RespostaServiceTest.java`
- `backend/src/test/java/br/com/maisedu/app/service/TurmaServiceTest.java`
- `docker-compose.yml`
- `docs/AJUSTES_MONOGRAFIA.md`
- `docs/ALTERACOES.md`
- `frontend/.dockerignore`
- `frontend/.env.example`
- `frontend/Dockerfile`
- `frontend/nginx.conf`
- `frontend/src/components/BarraPercentual.jsx`
- `frontend/src/components/GraficoEvolucao.jsx`
- `frontend/src/components/Mensagens.jsx`
- `frontend/src/components/PainelDesempenho.jsx`
- `frontend/src/components/rotulos.js`
- `frontend/src/pages/AceitarTermos.jsx`
- `frontend/src/pages/AtividadesProfessor.jsx`
- `frontend/src/pages/ConsultarAuditoria.jsx`
- `frontend/src/pages/DesempenhoTurma.jsx`
- `frontend/src/pages/GerenciarTurmas.jsx`
- `frontend/src/pages/GerenciarUsuarios.jsx`
- `frontend/src/pages/MeuDesempenho.jsx`
- `frontend/src/pages/MinhasAtividades.jsx`
- `frontend/src/services/alunoService.js`
- `frontend/src/services/atividadeService.js`
- `frontend/src/services/auditoriaService.js`
- `frontend/src/services/gestaoUsuarioService.js`
- `frontend/src/services/turmaService.js`
- `frontend/src/ui.css`

**Alterados (49):**

- `README.md`
- `backend/.env.example`
- `backend/src/main/java/br/com/maisedu/app/config/SeedInicial.java`
- `backend/src/main/java/br/com/maisedu/app/config/WebConfig.java`
- `backend/src/main/java/br/com/maisedu/app/controller/AuditoriaController.java`
- `backend/src/main/java/br/com/maisedu/app/controller/AuthController.java`
- `backend/src/main/java/br/com/maisedu/app/controller/QuestaoController.java`
- `backend/src/main/java/br/com/maisedu/app/controller/TurmaController.java`
- `backend/src/main/java/br/com/maisedu/app/dto/CadastroAlunoRequest.java`
- `backend/src/main/java/br/com/maisedu/app/dto/CadastroProfessorRequest.java`
- `backend/src/main/java/br/com/maisedu/app/dto/CriarQuestaoRequest.java`
- `backend/src/main/java/br/com/maisedu/app/dto/QuestaoResponse.java`
- `backend/src/main/java/br/com/maisedu/app/dto/UsuarioResumoResponse.java`
- `backend/src/main/java/br/com/maisedu/app/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/br/com/maisedu/app/model/AcaoAuditoria.java`
- `backend/src/main/java/br/com/maisedu/app/model/LogAuditoria.java`
- `backend/src/main/java/br/com/maisedu/app/model/Questao.java`
- `backend/src/main/java/br/com/maisedu/app/model/Usuario.java`
- `backend/src/main/java/br/com/maisedu/app/repository/AlunoTurmaRepository.java`
- `backend/src/main/java/br/com/maisedu/app/repository/LogAuditoriaRepository.java`
- `backend/src/main/java/br/com/maisedu/app/repository/ProfessorTurmaRepository.java`
- `backend/src/main/java/br/com/maisedu/app/repository/QuestaoRepository.java`
- `backend/src/main/java/br/com/maisedu/app/repository/TurmaRepository.java`
- `backend/src/main/java/br/com/maisedu/app/repository/UsuarioRepository.java`
- `backend/src/main/java/br/com/maisedu/app/security/JwtAuthenticationFilter.java`
- `backend/src/main/java/br/com/maisedu/app/security/JwtService.java`
- `backend/src/main/java/br/com/maisedu/app/security/UsuarioAutenticado.java`
- `backend/src/main/java/br/com/maisedu/app/service/AuditoriaService.java`
- `backend/src/main/java/br/com/maisedu/app/service/AutenticacaoService.java`
- `backend/src/main/java/br/com/maisedu/app/service/QuestaoService.java`
- `backend/src/main/java/br/com/maisedu/app/service/ResendEmailService.java`
- `backend/src/main/java/br/com/maisedu/app/service/UsuarioService.java`
- `backend/src/main/resources/application.properties`
- `backend/src/test/java/br/com/maisedu/app/controller/IntegracaoTestBase.java`
- `backend/src/test/java/br/com/maisedu/app/controller/QuestaoControllerIntegrationTest.java`
- `backend/src/test/java/br/com/maisedu/app/service/AutenticacaoServiceTest.java`
- `backend/src/test/java/br/com/maisedu/app/service/QuestaoServiceTest.java`
- `docs/integracao-resend.md`
- `frontend/src/App.jsx`
- `frontend/src/context/AuthContext.jsx`
- `frontend/src/main.jsx`
- `frontend/src/pages/CriarQuestao.jsx`
- `frontend/src/pages/Login.jsx`
- `frontend/src/pages/PoliticaPrivacidade.jsx`
- `frontend/src/pages/TermosUso.jsx`
- `frontend/src/services/authService.js`
- `frontend/src/services/http.js`
- `frontend/src/services/questaoService.js`
- `frontend/vite.config.js`

**Removidos:** nenhum.
