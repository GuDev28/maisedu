# Alterações

## Entrega com prazo, prática por tópico, painéis e nova paleta (23 de novembro de 2026)

### Antes de rodar

Nenhuma dependência nova (nem no Maven nem no npm). A migration `V11` roda sozinha. A versão dos
termos subiu para **3**: no próximo login todos leem e aceitam os novos Termos e a nova Política.
O compose passa a definir `TZ=America/Sao_Paulo` (os prazos e o fechamento das atividades usam o
fuso do servidor). Para os painéis com volume, suba com `SPRING_PROFILES_ACTIVE=dev,demo-volume`
num banco vazio (`docker compose down -v` antes).

### Como o aluno responde agora

| Item | O que mudou | Onde |
|---|---|---|
| Marcar e enviar tudo de uma vez | Clicar numa alternativa só a marca (pode trocar à vontade, a marcação fica no servidor). No fim da folha, "Enviar respostas" manda todas juntas, uma única vez, e só com todas as questões respondidas; o back-end recusa envio incompleto, repetido ou depois do prazo. Saiu o "Confirmar resposta" por questão | `RespostaService.enviar`, `POST /aluno/folhas/{id}/envio`, `pages/FolhaAtividade.jsx` |
| Prazo da atividade | O professor define o prazo ao criar (padrão sugerido: 7 dias, 23h59) e pode alterá-lo enquanto não termina. Depois do prazo não dá para começar nem enviar; quem não enviou fica "Não entregue" e as marcações não contam | `Atividade.prazo`, `PUT /atividades/{id}/prazo`, `pages/Atividades.jsx` |
| Correção só depois do prazo | Antes do prazo, o aluno vê o que enviou, sem acertos nem gabarito (nem na lista, nem na folha, nem no "baixar meus dados"). O professor já vê os acertos de quem entregou | `AtividadeAluno.correcaoLiberada`, `AtividadeService.montarFolha`, `GestaoUsuarioService` (exportação com `acertou` nulo) |
| Desempenho contado uma vez | Na prática, no envio. Na atividade, quando o prazo termina: uma tarefa agendada (a cada minuto) fecha as atividades vencidas, soma as respostas enviadas ao desempenho e marca cada resposta como contabilizada (`resposta.contabilizada_em`), então nada é contado duas vezes nem antes da hora. O fechamento vai para a auditoria como ação do sistema | `FechamentoAtividadesService`, `Resposta.marcarContabilizada` |
| Prática no mesmo fluxo | Marcar, enviar tudo e ver a correção logo depois do envio (sem prazo) | `RespostaService`, `pages/FolhaAtividade.jsx` |

### Praticar (cards por tópico)

| Item | O que mudou | Onde |
|---|---|---|
| Tela Praticar | No topo, a prática recomendada pelo motor (tópicos mais fracos e nível-alvo; diagnóstica para quem não respondeu nada). Abaixo, um card por tópico do ano do aluno, com acerto, níveis dominados e o nível atual; o clique gera uma prática curta (5 questões) daquele tópico, no nível que ele ainda não domina. Prática em andamento aparece como "Continuar" | `GET /aluno/praticas`, `POST /aluno/praticas-adaptativas?topicoId=`, `PraticaAdaptativaService`, `MotorAdaptativo.planejarTopico`, `pages/Praticar.jsx` |
| Filtro de ano na prática | A prática recomendada usa questões do ano escolar do aluno; se um tópico não tiver questões do ano, usa as do tópico (para não travar a prática) | `PraticaAdaptativaService.gerarRecomendada` |

### Painéis (com minimização)

| Item | O que mudou | Onde |
|---|---|---|
| Desempenho da turma (professor) | Mapa de calor aluno × tópico (escala divergente centrada em 70%, número em cada célula, asterisco com menos de 3 respostas), evolução semanal da turma (com dica ao passar o mouse e tabela) e "efeito da prática": acerto nas atividades antes e depois da 1ª prática de cada tópico, com o aviso de que é uma comparação descritiva | `DesempenhoService.consultarTurma`, `pages/Desempenho.jsx`, `components/GraficoEvolucao.jsx`, `components/GraficoHaltere.jsx`, `components/MapaDeCalor.jsx` |
| Painel da escola (admin) | Mapa ano escolar × tópico, comparativo entre turmas (acerto médio e alunos ativos em 14 dias), engajamento das últimas 8 semanas (uma medida por vez, mesmo eixo) e números do banco compartilhado. Só agregados, sem nomes; grupo com menos de 3 alunos aparece como "poucos dados", sem percentual | `PainelService.desempenhoDaEscola`, `GET /painel/escola`, `pages/PainelEscola.jsx`, menu Gestão e Início do admin |
| Resultados da atividade | Situação de cada aluno (entregue, em andamento, não iniciada, não entregue) e análise por questão: % de acerto e a alternativa errada mais escolhida | `AtividadeService.resultados`, `pages/Atividades.jsx` |

### Cadastro, e-mail e listas

| Item | O que mudou | Onde |
|---|---|---|
| Login do aluno gerado | O professor informa só o nome e o e-mail do responsável; o sistema gera um login `a` + 6 dígitos (sem nome nem matrícula, para não identificar o aluno) e mostra ao professor depois do cadastro. Alunos do seed mantêm os logins fixos | `GeradorLogin`, `UsuarioService.cadastrarAluno`, `pages/Turma.jsx` |
| Situação do e-mail de acesso | O resultado do envio (enviado, falhou, envio desativado) fica no usuário. O cadastro, a "Nova senha" e a correção do e-mail do responsável avisam quando o e-mail não saiu; nas listas, aparece um selo para quem ainda não fez o primeiro acesso e cujo e-mail falhou | `Usuario.emailAcessoStatus`, `ResendEmailService.registrarSituacao`, `components/SeloEmailAcesso.jsx` |
| Usuários paginados | `GET /usuarios` agora é paginado e com busca no servidor (nome, login, e-mail); a tela tem paginação e a auditoria busca o usuário enquanto se digita, em vez de carregar a escola inteira num select | `GestaoUsuarioService.listar`, `FiltroUsuarios`, `pages/Usuarios.jsx`, `pages/Auditoria.jsx` |
| Ordem dos anos | Painel da escola ordena por ano escolar (6º ao 9º), e não pelo nome do enum | `PainelService` |

### Banco (`V11`)

`atividade.prazo` (atividades existentes recebem criação + 7 dias) e `atividade.fechada_em`;
`resposta.contabilizada_em` (respostas existentes já contadas recebem a data da resposta);
`atividade_aluno.topico_id` (só em prática adaptativa, com CHECK); `usuario.email_acesso_status`
(CHECK com os três valores) e `email_acesso_em`; índices parciais para as atividades a fechar e as
práticas abertas.

### Cenário de demonstração

- Perfil `dev`: atividades com prazo já encerrado e corrigidas, uma atividade ainda no prazo
  ("Porcentagem e média", com entregas, marcações não enviadas e alunos sem começar) e práticas
  feitas antes de uma atividade posterior, para o "efeito da prática" aparecer.
- Perfil novo `demo-volume`: escola fictícia com 30 turmas, 12 professores e 1000 alunos simulados,
  com respostas em várias semanas, práticas e uma atividade aberta por turma (inserção em lote com
  JDBC). Os dados são simulados: servem para mostrar os painéis, não como resultado pedagógico.

### Visual

Paleta nova: azul principal `#195ABE` (botões e destaques), azul escuro `#0F3E87` (menu lateral),
azul claro `#3B82F6` (gráficos), azul suave `#BEDBFE` (fundos de destaque), fundo `#F8FAFC`, cards
brancos com borda `#E2E8F0`, texto `#0F172A`/`#64748B` e cores de estado (sucesso, atenção, erro e
informação) com um tom escuro para texto, para manter o contraste. Logo nova (PNG recolorido para o
azul principal, 512 e 192 px) e favicon em SVG.

### Textos

Termos de Uso e Política de Privacidade na **versão 3** (entrega única e prazo, correção depois do
prazo, login gerado, prática por tópico, painel agregado do admin, situação do e-mail de acesso),
Como funciona, Login e Início de cada perfil.

### Como foi verificado

O ambiente continua sem acesso ao Maven Central e ao registro npm:

- **Java**: `main` e `test` compilados com `javac` contra stubs das bibliotecas, sem nenhum erro de
  tipo, de assinatura ou de nome.
- **Testes executados aqui**: os testes de unidade puros (entidades, motor adaptativo, gerador de
  login, efeito da prática e evolução da turma) rodaram num executor mínimo e passaram. Os testes com
  Mockito e os de integração (H2) foram atualizados e revisados, mas não rodaram aqui.
- **Banco**: V1 a V11 aplicadas num PostgreSQL 16 zerado; a carga do `demo-volume` rodou de ponta a
  ponta nesse banco, respeitando todas as restrições e gatilhos.
- **Front-end**: empacotado com esbuild, sem erro de importação; as telas novas (Praticar, Atividades
  com prazo e resultados, Desempenho da turma e Painel da escola) foram abertas no Chromium com a API
  simulada, conferindo layout, dicas ao passar o mouse e console sem erros.

**Falta rodar com as bibliotecas reais**: `./mvnw test`, `npm install && npm run build`,
`npm run lint` e `docker compose up --build`.


## Ajustes depois da revisão (16 de novembro de 2026)

### Antes de rodar

Nada novo para instalar. A migration `V10` roda sozinha. A versão dos termos subiu para **2**: no
próximo login, todos (inclusive os usuários do seed) leem e aceitam a nova Política e os novos Termos.
Em produção, defina `APP_URL` com o endereço público (vai nos links dos e-mails).

| Item | O que mudou | Onde |
|---|---|---|
| Auditoria (erro ao abrir) | No PostgreSQL, a consulta com `:de is null` recebia o parâmetro de data nulo sem tipo e o banco respondia "could not determine data type of parameter", e a tela mostrava "Erro inesperado". O filtro agora é montado com `Specification`: cada condição só entra quando o filtro foi preenchido. O H2 dos testes não reproduzia o erro; há um teste de integração novo cobrindo a tela sem filtros | `repository/FiltroAuditoria.java`, `LogAuditoriaRepository`, `AuditoriaService` |
| E-mail do responsável | O cadastro do aluno pede o e-mail do responsável (obrigatório). O login e a senha provisória vão para esse e-mail, e não aparecem mais para o professor. "Nova senha" também envia para o responsável. O professor da turma (e o admin) corrige o e-mail e pode, no mesmo passo, mandar uma senha nova. Aluno antigo sem e-mail do responsável continua recebendo a senha pela tela, como antes | `Usuario.emailResponsavel`, `UsuarioService`, `GestaoUsuarioService`, `ResendEmailService`, `PUT /usuarios/{id}/email-responsavel`, `pages/Turma.jsx`, `components/EmailDoResponsavel.jsx` |
| E-mail ao responsável (conteúdo) | Só o primeiro nome do aluno, o login, a senha provisória, o nome da escola e links para o sistema e para a Política. A auditoria registra o envio, mas não o endereço | `ResendEmailService` |
| Marcar antes de responder | Clicar numa alternativa só a marca; a resposta vale em "Confirmar resposta". A marcação fica no servidor (o aluno sai e volta sem perder), pode ser trocada quantas vezes quiser e é apagada ao confirmar. A navegação mostra as questões marcadas e não confirmadas. A resposta continua única (RN04) e a ordem das respostas confirmadas, que o motor adaptativo usa, não muda | `AtividadeAlunoQuestao.alternativaMarcada`, `RespostaService.marcar`, `PUT /aluno/folhas/{id}/questoes/{id}/marcacao`, `pages/FolhaAtividade.jsx` |
| Banco (`V10`) | `usuario.email_responsavel`; `atividade_aluno_questao.alternativa_marcada_id` e `marcada_em`, com gatilho que recusa alternativa de outra questão | `db/migration/V10__email_responsavel_e_marcacao.sql` |
| Política de Privacidade e Termos (versão 2) | Novo dado (e-mail do responsável), finalidade e base legal, o que o Resend passa a receber, correção, eliminação e retenção; Termos descrevem o cadastro com e-mail do responsável e a confirmação da resposta | `pages/PoliticaPrivacidade.jsx`, `pages/TermosUso.jsx`, `maisedu.termos.versao-atual=2` |
| Textos | Login, Como funciona e Minha conta (aluno vê o e-mail do responsável cadastrado) | `pages/Login.jsx`, `pages/ComoFunciona.jsx`, `pages/MinhaConta.jsx` |
| Configuração | `APP_URL` (links dos e-mails) | `application.properties`, `.env.example`, `docker-compose.yml` |

Testes novos: auditoria sem filtros e com filtro de ação e período; cadastro e senha nova com envio
ao responsável; correção do e-mail (professor da turma, professor de outra turma, professor sem esse
campo); anonimização apaga o e-mail do responsável; marcar, trocar, confirmar, marcação apagada e
recusa de marcação em questão já respondida.

## Versão para a apresentação do PFC (novembro de 2026)

Implementa a análise do projeto feita antes da apresentação: pendências, simplificações de fluxo e
de código, telas novas e o menu lateral com React Router e Bootstrap. Ficaram como trabalhos
futuros, como na análise: motor estatístico (BKT/TRI), testes com PostgreSQL real
(Testcontainers), prazos de entrega e de votação e importação de alunos por CSV.

### Antes de rodar: `npm install` no front-end

O `package.json` ganhou `react-router-dom` e `bootstrap`. O `package-lock.json` não pôde ser
atualizado aqui (sem acesso ao registro npm), e o `Dockerfile` usa `npm ci`, que exige os dois em
sincronia. Rode uma vez e faça commit do lock:

```bash
cd frontend && npm install && cd ..
git add frontend/package-lock.json
```

Depois: `cd backend && ./mvnw test` e `docker compose down -v && docker compose up --build`.

### Telas e navegação

| Item | O que mudou | Onde |
|---|---|---|
| Menu lateral | Seções por perfil, item atual destacado, contador de questões esperando voto, gaveta no celular, rodapé com Minha conta, Como funciona, Termos, Privacidade e Sair | `navegacao.js`, `components/Layout.jsx` |
| React Router | Uma rota por tela; Termos e Privacidade em `/termos` e `/privacidade` (o Voltar do navegador funciona) | `App.jsx`, `main.jsx` |
| Bootstrap 5.3 | Todas as telas; as 7 telas antigas com estilo inline foram refeitas; modal e avisos no lugar de `window.confirm`/`prompt` | `estilo.css`, `context/InterfaceContext.jsx` |
| Início | Admin: números da escola e alertas (contas bloqueadas, 1º acesso pendente). Professor: questões esperando voto e, por turma, tópico mais fraco e alunos que não começaram. Aluno: acerto geral, tópico que precisa de atenção, próxima atividade e "Praticar agora" | `pages/inicio/`, `GET /painel/instituicao` |
| Turma | Tela única com abas Pessoas, Atividades e Desempenho. Em Pessoas: cadastrar já vinculando, vincular quem já tem conta, tirar da turma e gerar senha nova. Substitui Cadastrar aluno, Cadastrar professor, Vincular aluno, Vincular professor e Meus alunos | `pages/Turma.jsx` |
| Banco de questões | Tela própria, com autor, escola de origem e contagem por tópico | `pages/BancoQuestoes.jsx` |
| Minhas questões | Placar ("2 de 3 aprovações"), motivos da rejeição, editar e excluir rascunho, filtro por status | `pages/MinhasQuestoes.jsx` |
| Criar/editar questão | Botões "Salvar rascunho" e "Salvar e enviar para validação" | `pages/FormQuestao.jsx` |
| Avaliar questões | Botões Aprovar e Rejeitar (com motivo obrigatório) e placar parcial | `pages/AvaliarQuestoes.jsx` |
| Folha da atividade | Uma questão por vez, barra de progresso, navegação entre questões e tela final com acertos por tópico; usa o retorno do POST da resposta (uma requisição a menos por clique) | `pages/FolhaAtividade.jsx` |
| Praticar agora | Item próprio no menu; mostra os tópicos e níveis escolhidos pelo motor antes de começar | `pages/Praticar.jsx` |
| Minha conta | Dados, escola, papéis, alterar senha, versão e data do aceite, baixar meus dados (JSON) | `pages/MinhaConta.jsx`, `GET /auth/conta`, `GET /auth/meus-dados` |
| Como funciona | Fluxo por perfil, com links para as telas | `pages/ComoFunciona.jsx` |
| Auditoria | Ações com nome em português e lista vinda do back-end; filtro por nome do usuário em vez do id | `pages/Auditoria.jsx`, `GET /auditoria/acoes` |
| Rótulos | Ano, nível, status, situação e ações da auditoria sem códigos crus | `components/rotulos.js` |
| Identidade | Logo, cor própria (verde-azulado), favicon, título "MaisEdu", `lang="pt-BR"`; arquivos do template do Vite apagados | `components/Logo.jsx`, `index.html`, `public/favicon.svg` |

### Back-end

| Item | O que mudou | Onde |
|---|---|---|
| Prática diagnóstica | Aluno sem nenhuma resposta recebe questões fáceis de até 3 tópicos do ano da turma (antes, "Praticar agora" dava erro) | `MotorAdaptativo.planejarDiagnostico`, `PraticaAdaptativaService` |
| Desvincular | `DELETE /turmas/{id}/alunos/{alunoId}` (admin ou professor da turma) e `/professores/{id}` (só admin), com auditoria; histórico do aluno é mantido | `VinculoTurmaService`, `VinculoTurmaController` |
| Membros da turma | `GET /turmas/{id}/membros` | `VinculoTurmaService.listarMembros` |
| Cadastro com turma | `turmaId` opcional em `POST /alunos` e `POST /professores`: cria e vincula na mesma transação | `UsuarioService` |
| Rascunho | `PUT` e `DELETE /questoes/{id}`, só do autor e só em rascunho | `QuestaoService`, `Questao.editar` |
| Motivo da rejeição | Campo `comentario` (obrigatório ao rejeitar); o autor vê os motivos sem saber quem votou; a fila do avaliador mostra o placar parcial mas não os motivos | `AvaliacaoQuestao`, migration **V9**, `QuestaoResponse` |
| Placar e escola | `QuestaoResponse` com aprovações, rejeições, motivos e escola do autor | `QuestaoService.comPlacar` |
| Troca voluntária de senha | Em Minha conta, sem novo aceite; no primeiro acesso ou com termos novos, o aceite continua obrigatório | `AutenticacaoService.trocarSenha` |
| Mensagem | "use Minha conta → Alterar senha" (antes citava uma opção que não existia) | `GestaoUsuarioService.redefinirSenha` |
| Sessão | `GET /auth/me` traz o nome da escola para o menu | `UsuarioResumoResponse` |

### Simplificações de código

| Antes | Agora |
|---|---|
| Regra "admin da instituição ou professor da turma" em 4 lugares | `AcessoTurma` com `buscarComAcesso`, `buscarComoAdmin` e `buscarComoProfessorDaTurma` |
| "Nível dominado" calculado em dois lugares e parâmetros lidos duas vezes com `@Value` | Um bean `MotorAdaptativo` (`AdaptativoConfig`); `DesempenhoService` usa `motor.dominado`; `DesempenhoTopico.dominado` removido |
| Services recebendo ora `Long id`, ora `UsuarioAutenticado` | `UsuarioAutenticado` em questões, avaliação e cadastro; `AuditoriaService` com 2 sobrecargas de `registrar` em vez de 3 |
| `setStatus` na questão a partir do service | `Questao.apurarVotos(aprovacoes, rejeicoes)`; `@Setter` removido |
| Consultas dentro de laços (N+1) | Fila de avaliação com `not exists`; contagens de atividades e placares agregados no banco (`PlacarAtividade`) |
| Contagem de acertos repetida | `Placar` |
| Turma/usuário inexistente devolvia 400 | 404 (`RecursoNaoEncontradoException`) |
| JSON de erro escrito à mão em dois lugares | `RespostaErroJson` |
| `stream()` em `Questao.definirAlternativas` | `for`, como no resto do projeto |
| `UsuarioRepository.findByEmail`/`findByLogin` sem uso | Removidos; `findByTurmaIdComProfessor` passou a ser usado |

### Documentos

- Termos de Uso e Política de Privacidade: professor pode tirar aluno da turma; rascunho editável;
  motivo da rejeição (lido pelo autor, sem identificar quem votou); "baixar meus dados" em Minha
  conta. Nesta etapa ficaram na versão 1; depois subiram para a versão 2 (16/11, e-mail do
  responsável) e para a versão 3 (23/11, entrega com prazo e login gerado), cada uma exigindo novo
  aceite.
- `docs/diagrams/er_diagram.png`: agora é o modelo de dados de verdade, gerado do banco criado pelas
  migrations V1 a V9 (fonte em `er_diagram.dot`).
- `docs/diagrams/arquitetura.png`: inclui o nginx com proxy `/api`, os três contêineres e o Resend
  (fonte em `arquitetura.dot`).

### Como foi verificado

O ambiente onde as mudanças foram feitas não tem acesso ao Maven Central nem ao registro npm, então:

- **Java**: compilação com `javac` contra os fontes do projeto, sem as bibliotecas (Spring, Lombok).
  Conferido: nenhum erro de sintaxe; nenhum método, construtor ou campo do projeto chamado com nome
  ou número de argumentos errado (em `main` e em `test`); getters/setters do Lombok usados só onde a
  classe tem a anotação e o campo. Os construtores gerados pelo `@RequiredArgsConstructor` usados
  nos testes foram conferidos à mão, na ordem dos campos.
- **Migrations**: V1 a V9 aplicadas em sequência num PostgreSQL 16 zerado, sem erro.
- **Front-end**: ESLint (variáveis não definidas ou sem uso) sem apontamentos; empacotado com esbuild e
  aberto no Chromium (Playwright) com a API simulada, nos três perfis, em todas as telas, no computador
  e no celular; nenhum erro no console.
- **Testes**: atualizados para as assinaturas novas, e testes novos para prática diagnóstica,
  quórum na entidade, edição e exclusão de rascunho, motivo da rejeição, placar, desvincular,
  membros da turma, cadastro já vinculando, troca voluntária de senha, Minha conta, Início do admin
  e lista de atividades do aluno com placar agregado.

**Falta rodar com as bibliotecas reais**: `./mvnw test`, `npm run build`, `npm run lint` e
`docker compose up --build`.

---

## Versão anterior (funcionalidades obrigatórias)

Resumo do que foi implementado a partir da análise do projeto, onde está no código, as decisões
tomadas onde a monografia deixava margem e o que foi (e o que ainda precisa ser) verificado.

### 1. Funcionalidades novas

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

### 2. Correções

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

### 3. Banco de dados

- **V7** — `log_auditoria.instituicao_id`; normalização de e-mail/login; `usuario.versao_sessao`, `termos_versao`, `anonimizado_em`.
- **V8** — `questao.ano_escolar`; CHECKs de status e ano; tabelas `atividade`, `atividade_questao`, `atividade_aluno`, `atividade_aluno_questao`, `resposta`, `desempenho_topico`; trigger que garante no banco que a alternativa da resposta pertence à questão entregue (RN04).

### 4. Decisões tomadas onde a monografia deixava margem

1. **Prática em andamento é retomada.** Se o aluno já tem uma prática adaptativa não concluída, pedir outra devolve a mesma (evita acumular práticas e "trocar" uma prática difícil).
2. **Seleção das questões (RN06, passo c).** Em cada tópico: primeiro o nível-alvo (inéditas antes das já respondidas), depois os níveis adjacentes (FÁCIL→MÉDIO; MÉDIO→FÁCIL, DIFÍCIL; DIFÍCIL→MÉDIO). Os tópicos são intercalados a partir do mais fraco (10 vagas em 3 tópicos = 4/3/3). Vagas que um tópico não preencher ficam para os outros. Se houver menos de 10 questões compatíveis, a prática sai menor; com zero, não é gerada (CA03).
3. **Empate no ranking dos tópicos:** menor percentual; empate → tópico com mais respostas; depois o id.
4. **Admin e turmas:** o admin vê resultados e desempenho de qualquer turma da instituição, mas só cria atividade em turma em que está vinculado como professor (RF15 diz "professor que leciona na turma").
5. **Logout encerra todas as sessões do usuário** (todos os dispositivos), porque a revogação é por versão.
6. **Anonimização** não se aplica a administradores (a instituição não pode ficar sem gestor) e mantém respostas/desempenho sem identificação.
7. **Bloqueio por tentativas** continua informando o tempo restante antes de conferir a senha (CA03 do RF01).
8. **Ano escolar** é obrigatório para questões novas e nulo nas criadas antes da V8.

### 5. Como foi verificado

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

### 6. Arquivos (em relação ao zip original)

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
