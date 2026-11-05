# Ajustes na monografia para refletir esta versão

O arquivo .docx não foi alterado. Abaixo, por seção, o que mudar para o texto bater com o código.
Trechos entre aspas podem ser colados (revisem o tom e a formatação ABNT).

## Resumo e Abstract

- Trocar a última frase ("Até o presente momento, estão implementados... estão especificados para as
  próximas etapas") por: "Estão implementados a autenticação com controle de acesso por perfil, o
  cadastro de turmas e usuários e o vínculo às turmas, o banco de questões com validação colaborativa
  por quórum, as atividades com correção automática, o cálculo do desempenho por tópico e nível, a
  prática adaptativa, o acompanhamento do desempenho, a trilha de auditoria e os controles de
  privacidade exigidos pela LGPD." (e o equivalente no Abstract).

## 1.3.6 Containerização, versionamento e testes

- O Docker agora existe: "A aplicação é executada com Docker Compose em três contêineres — PostgreSQL,
  back-end e front-end servido por nginx, que também encaminha as chamadas `/api` ao back-end —, e o
  ambiente completo sobe com um único comando."
- Testes: citar testes unitários (JUnit/Mockito) **e** de integração (Spring Boot Test com H2).

## 1.4 Arquitetura da solução

- Acrescentar: o front-end acessa a API pela mesma origem (`/api`), o que permite o cookie
  `SameSite=Strict` e dispensa CORS; o e-mail do Resend é enviado **depois do commit** da transação.
- Atualizar a Figura 1 se ela mostrar o front chamando a API diretamente.

## 1.5.1 Atores e regras de negócio

- "A instituição e o seu primeiro administrador são registrados diretamente no banco" → "são criados
  na primeira inicialização da aplicação, com senha provisória registrada no log (classe SeedInicial)".
- Quadro 1: RN03, RN04, RN05 e RN06 → **Implementada**.

## 1.5.2 Requisitos funcionais

- Quadro 2: RF06, RF15, RF16, RF17, RF18 e RF19 → **Implementado**. RF13: remover "a tela de consulta está prevista".
- Quadro 3 (RF01): nas regras, "Contas desativadas não podem se autenticar; a mensagem específica só é
  exibida a quem informou a senha correta". Pós-condição: sessão de 120 minutos **revogável**.
- Quadro 5 (RF03): regra nova — "O servidor invalida o token no logout (versão da sessão), de modo que um
  token copiado antes deixa de ser aceito." CA01 continua igual.
- Quadro 11 (RF09): acrescentar o **ano escolar** no fluxo; trocar "Exatamente uma alternativa é correta
  (regra garantida também por restrição no banco de dados)" por "Exatamente uma alternativa é correta
  (validada na entidade); o banco impede mais de uma correta e posições repetidas".
- Quadro 14 (RF12): incluir as ações novas (CADASTRO_TURMA, ALTERACAO_TURMA, CRIACAO_ATIVIDADE,
  INICIO_ATIVIDADE, GERACAO_PRATICA_ADAPTATIVA, REDEFINICAO_SENHA, CORRECAO_DADOS_USUARIO,
  DESATIVACAO_USUARIO, REATIVACAO_USUARIO, EXPORTACAO_DADOS_USUARIO, ANONIMIZACAO_USUARIO,
  LIMPEZA_AUDITORIA) e a regra "cada registro guarda a instituição de quem executou a ação".
- Quadro 15 (RF13): regra nova — "O administrador só consulta os registros da própria instituição."
- Quadro 20 (RF18): detalhar o passo 4 conforme `docs/ALTERACOES.md` §4, item 2, e a regra "se houver
  prática adaptativa não concluída, ela é retomada em vez de gerar outra".
- Novos quadros sugeridos (mesmo modelo):
  - **RF20 – Redefinir senha provisória** (Administrador, Professor): admin para qualquer usuário da
    instituição; professor só para alunos das turmas em que leciona; gera senha provisória, desbloqueia
    a conta e encerra as sessões; professor/admin recebe por e-mail, aluno recebe pelo professor.
  - **RF21 – Atender direitos do titular** (Administrador): exportar dados em JSON, corrigir nome,
    desativar/reativar e anonimizar (exceto administradores).
  - **RF22 – Aceitar nova versão dos termos** (Todos): quando a versão vigente muda, o acesso fica
    restrito até o novo aceite, registrado com data, hora e versão.

## 1.5.3 Requisitos não funcionais (Quadro 22)

- **RNF02**: "Sessão com token JWT assinado, válido por 120 minutos e revogável: a cada requisição o
  servidor confere, por consulta à chave primária, se a conta está ativa e se a versão da sessão do
  token é a atual." (o sistema deixa de ser 100% stateless — vale citar como trade-off consciente).
- **RNF05**: acrescentar "isolada por instituição e mantida por 365 dias, com exclusão automática".
- **RNF06**: acrescentar "ferramentas de atendimento aos direitos do titular (RF21)".
- **RNF07**: acrescentar as restrições da V8 (resposta única por questão entregue, alternativa da
  questão entregue por trigger, contadores de desempenho coerentes, tipo de atividade).
- **RNF10**: "Atendido" — RN01 já tinha testes; hoje há testes unitários de RN01 a RN06 e testes de
  integração dos fluxos HTTP. Ressalva: a integração usa H2 com Flyway desligado.
- **RNF11**: URL e usuário do banco também vêm de variáveis de ambiente.
- **RNF15**: "Atendido" depois que vocês rodarem `docker compose up --build`.

## 1.5.4 Diagrama de classes

- Figura 2: `Usuario` ganha `versaoSessao`, `termosVersao`, `anonimizadoEm` e as operações
  `redefinirSenhaProvisoria`, `encerrarSessoes`, `desativar`, `reativar`, `aceitarTermos(versao)`,
  `termosPendentes(versao)`, `anonimizar`.
- Figura 3: `Questao` ganha `anoEscolar`.
- Figura 4: remover o estereótipo «planejada»; `Resposta.acertou()` e `DesempenhoTopico.registrarResposta()`
  existem como descritos; `AtividadeAluno` tem as fábricas `regular(...)` e `adaptativa(...)`.
- Figura 5: acrescentar `TurmaService`, `GestaoUsuarioService`, `AtividadeService`, `RespostaService`,
  `DesempenhoService`, `PraticaAdaptativaService`, `MotorAdaptativo` (sem dependências de Spring),
  `AcessoTurma` e `RetencaoAuditoriaService`; `LogAuditoria` ganha `instituicaoId`.

## 1.5.5 BPMN

- Figura 6: "Cadastrar turma" deixa de ser planejada.
- Figura 8: deixa de ser planejada; acrescentar o desvio "há prática em andamento? → retomar".

## 1.6 Controle de acesso, auditoria e proteção de dados

- 1.6.2: acrescentar a revogação de sessão, a normalização do identificador (minúsculas) e a resposta
  em tempo constante para identificador inexistente.
- 1.6.3: auditoria isolada por instituição, retenção de 365 dias e o detalhe de que a falha ao gravar o
  log não afeta a operação (o tratamento de erro fica fora da transação independente).
- 1.6.4: citar o **Enunciado CD/ANPD nº 1/2023** como base para tratar dados de crianças e adolescentes
  pelas hipóteses dos arts. 7º e 11, prevalecendo o melhor interesse (art. 14); explicar que o aceite
  do aluno é ciência, não base legal; descrever as ferramentas do RF21, a retenção e a transferência
  internacional ao Resend (art. 33). Mencionar a cláusula de autoria/licença das questões nos Termos.

## 1.7 Banco de questões colaborativo

- 1.7.1: questão classificada por tópico, nível **e ano escolar**.

## 1.8 Prática adaptativa e acompanhamento de desempenho

- Trocar "serão implementadas nas próximas etapas" pelo presente.
- 1.8.3: incluir a ordem de seleção, a intercalação entre tópicos, a redistribuição de vagas e a
  retomada de prática em andamento. Vale citar que o motor é uma classe sem dependências de framework,
  coberta por testes unitários que reproduzem os critérios de aceite do RF18.

## Capítulos seguintes (sugestão para Resultados)

O perfil `dev` gera um cenário reprodutível (duas escolas, 96 questões, respostas em semanas
diferentes). Dá para usar como base de uma avaliação por simulação: alunos sintéticos com domínio
conhecido por tópico, medindo se o motor prioriza os tópicos certos e em quantas práticas o aluno
"domina" cada nível. Somado a um teste de usabilidade com professores (questionário SUS), isso
sustenta o capítulo de resultados mesmo sem uso em escola real.

## Referências a acrescentar

- BRASIL. Autoridade Nacional de Proteção de Dados. **Enunciado CD/ANPD nº 1, de 2023**: tratamento
  de dados pessoais de crianças e adolescentes. Brasília, DF: ANPD, 2023. (Conferir a data exata e o
  link oficial no site da ANPD em gov.br antes de inserir.)
- RESEND. **Testing with resend.dev domain / 403 error**. Disponível em:
  https://resend.com/docs/knowledge-base/403-error-resend-dev-domain. (Se citarem a restrição do
  remetente de teste.)
