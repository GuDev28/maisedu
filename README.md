# MaisEdu

Plataforma web de **aprendizagem adaptativa de Matemática** para o Ensino Fundamental II (6º ao 9º ano), com **banco de questões compartilhado** entre instituições de ensino cadastradas.

> Projeto Final de Curso (PFC) — Bacharelado em Engenharia de Software, Universidade de Mogi das Cruzes (UMC).

## Sobre o projeto

O MaisEdu permite que professores criem questões de Matemática por tópico, nível de dificuldade e ano escolar, submetam-nas a uma **validação colaborativa por pares** (quórum de 3 votos em até 5) e, uma vez aprovadas, as usem — junto com as de outras escolas — em atividades para as suas turmas. As respostas dos alunos alimentam o **desempenho por tópico e nível**, que o **motor de prática adaptativa** usa para montar exercícios direcionados às maiores dificuldades de cada aluno.

## Funcionalidades

| Requisito | Funcionalidade | Perfil |
|---|---|---|
| RF01–RF03 | Login por cookie HttpOnly, troca da senha provisória com aceite dos termos, logout que invalida a sessão no servidor | Todos |
| RF04, RF05 | Cadastro de professores (senha por e-mail via Resend) e de alunos (sem e-mail, com login gerado pelo sistema e acesso enviado ao e-mail do responsável), já podendo escolher a turma; a tela avisa quando o e-mail de acesso não foi entregue | Admin, Professor |
| RF06 | Cadastro e desativação de turmas | Admin |
| RF07, RF08 | Tela da Turma: professores e alunos, vincular e desvincular (RN01) | Admin, Professor |
| RF09–RF11 | Questões com 5 alternativas (rascunho editável), envio para validação e votação por quórum com motivo da rejeição e placar para o autor (RN02) | Professor |
| RF12, RF13 | Trilha de auditoria isolada por instituição, com tela de consulta (filtro de usuário com busca no servidor) | Admin |
| RF14 | Termos de Uso e Política de Privacidade, versionados (nova versão exige novo aceite) | Todos |
| RF15 | Banco de questões compartilhado (com escola de origem) e atividades com as questões aprovadas e prazo de entrega (alterável antes de terminar); resultados com o erro mais comum de cada questão | Professor |
| RF16 | Resolução com snapshot das questões (RN03): o aluno marca e troca alternativas e envia tudo de uma vez, só com todas respondidas (RN04); a correção aparece depois do prazo (na prática, logo após o envio) | Aluno |
| RF17 | Desempenho acumulado por tópico e nível (RN05), contado uma única vez: na prática, no envio; na atividade, no fechamento automático do prazo | Sistema |
| RF18 | Tela Praticar: prática recomendada pelo motor (RN06, com diagnóstica para quem não respondeu nada) e cards por tópico para praticar o tópico escolhido | Aluno |
| RF19 | Painel do aluno e da turma (mapa de calor aluno × tópico, evolução semanal, efeito da prática) e painel da escola para o administrador, só com números agregados (grupos com menos de 3 alunos aparecem como "poucos dados") | Aluno, Professor, Admin |
| LGPD | Lista de usuários paginada com busca; redefinição de senha, correção, desativação, exportação (JSON) e anonimização de usuários; retenção automática dos logs | Admin |
| LGPD | Minha conta: dados, troca de senha, aceite dos termos e "baixar meus dados" pelo próprio titular | Todos |
| — | Página Início por perfil, menu lateral, "Como funciona" | Todos |

## Arquitetura

![Diagrama de arquitetura do MaisEdu](docs/diagrams/arquitetura.svg)

- **Front-end**: SPA em React + Vite, com React Router (uma rota por tela, o Voltar do navegador funciona) e Bootstrap 5.3. Menu lateral por perfil (gaveta no celular). Chama a API sempre em `/api` (proxy do Vite em desenvolvimento, nginx no Docker), então front e API ficam na mesma origem.
- **Back-end**: API REST em Spring Boot, em camadas Controller → Service → Repository. A regra de acesso à turma fica num lugar só (`AcessoTurma`). O motor adaptativo (`MotorAdaptativo`) é uma classe pura, sem Spring, testada isoladamente, e um único bean dele serve a prática e o painel de desempenho.
- **Autenticação**: Spring Security + JWT em cookie HttpOnly/SameSite=Strict, RBAC por papéis. A cada requisição o filtro confere no banco se a conta está ativa e se a sessão não foi encerrada.
- **Persistência**: PostgreSQL via Spring Data JPA / Hibernate; esquema evoluído só por migrations Flyway (`V1` a `V11`). Modelo de dados na seção [Modelo de dados](#modelo-de-dados).
- **Serviço externo**: Resend, para os e-mails de senha provisória: de professores e administradores para o próprio e-mail; de alunos, para o e-mail do responsável (enviados só depois do commit). O resultado do envio (enviado, falhou, envio desativado) fica gravado no usuário para a tela avisar.
- **Tarefas agendadas**: o fechamento das atividades vencidas (a cada minuto: contabiliza o desempenho das respostas enviadas e libera a correção) e a retenção da auditoria (diária). Os horários usam o fuso do servidor; o compose define `TZ=America/Sao_Paulo`.

## Modelo de dados

![Diagrama entidade-relacionamento do MaisEdu](docs/diagrams/er_diagram.svg)

As tabelas estão agrupadas em quatro áreas: **instituição e acesso** (usuários, papéis e auditoria), **turmas** (RN01), **banco de questões** (RN02) e **atividades e desempenho** (RN03 a RN06). As relações usam a notação pé de galinha; o círculo marca as chaves estrangeiras opcionais.

## Como executar

### Opção 1 — Docker (um comando)

Requisito: Docker com Compose.

```bash
cp .env.example .env        # opcional: ajuste JWT_SECRET, RESEND_API_KEY etc.
docker compose up --build
```

- Sistema: **http://localhost:8081**
- API direta (Postman/Insomnia): http://localhost:8080

Por padrão roda no perfil **dev**, que popula um banco vazio com o cenário de demonstração (abaixo). Para zerar o banco: `docker compose down -v`.

### Opção 2 — Desenvolvimento local

Requisitos: Java 25, Node 22+, PostgreSQL com um banco `maisedu`.

```bash
# back-end
cd backend
cp .env.example .env          # preencha DB_PASSWORD e JWT_SECRET (o Spring lê este arquivo)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# front-end (outro terminal)
cd frontend
npm install                    # na primeira vez e sempre que o package.json mudar
npm run dev                    # http://localhost:5173 (o Vite repassa /api para a porta 8080)
```

### Cenário de demonstração (perfil `dev`)

Criado só quando o banco está vazio, numa única carga em lote (cerca de 30 segundos na primeira subida). Todos os dados são fictícios. Senha de todas as contas: **`MaisEdu@2026`**.

São duas escolas com 1000 alunos no total, em turmas do 6º ao 9º ano com 20 a 30 alunos cada (tamanho sorteado):

| Escola | Alunos | Turmas |
|---|---|---|
| Escola Estadual Monteiro Lobato (fictícia) | 560 | 22 |
| Colégio Municipal Cecília Meireles (fictício) | 440 | 18 |

| Conta | Perfil |
|---|---|
| `ana@maisedu.dev` | **Professora vitrine** (Monteiro Lobato): leciona no 7º ano A, 7º ano B e 8º ano A, com atividades vencidas e corrigidas e atividades abertas com entregas; questões dela aprovadas, pendentes, uma rejeitada (com os motivos dos colegas) e dois rascunhos; 10 questões de colegas esperando o voto dela na fila de avaliação |
| `a700001` | **Aluna vitrine** (7º ano A, turma da Ana): atividades corrigidas, uma não entregue, uma enviada aguardando a correção, uma em andamento com respostas marcadas e uma a fazer; práticas recomendadas e por tópico já feitas, uma prática por tópico em andamento, níveis dominados e um tópico fraco |
| `admin.a@maisedu.dev` | Administradora da Monteiro Lobato |
| `bruno@maisedu.dev`, `prof01.a@maisedu.dev` … `prof04.a@maisedu.dev` | Professores da Monteiro Lobato |
| `admin.b@maisedu.dev` | Administrador do Cecília Meireles |
| `carla@maisedu.dev`, `prof01.b@maisedu.dev` … `prof04.b@maisedu.dev` | Professores do Cecília Meireles |
| `diego@maisedu.dev` | Professor sem turma (Cecília Meireles), para demonstrar o vínculo com turma |
| `elisa@maisedu.dev`, `a700099` | Primeiro acesso: senha provisória **`Provisoria1`** (`a700099` está no 7º ano A) |

Os administradores têm só o papel de administrador: montam a escola (turmas, professores, alunos e vínculos) e acompanham o painel da escola e a auditoria, mas não lecionam, não criam questões nem votam na validação entre pares, que fica com os professores. O sistema ainda aceita uma conta com os dois papéis, como exceção (um coordenador que também dá aula).

Os demais alunos têm login gerado (ex.: `a482913`); o quadro "MaisEdu — cenário de demonstração" no log do back-end (`docker compose logs backend`) lista alguns de cada escola. Cada aluno tem como e-mail do responsável `responsavel.<login>@maisedu.dev` (endereço fictício). Sem `RESEND_API_KEY`, o conteúdo dos e-mails (inclusive a senha provisória enviada ao responsável) aparece no log do back-end.

O que vem junto:

- **Banco compartilhado**: cerca de 230 questões aprovadas pelo quórum de 3 votos (8 tópicos × 3 níveis, gabarito calculado), com autores e avaliadores das duas escolas; 15 pendentes com 0 a 2 votos; rejeitadas com o motivo; rascunhos.
- **Atividades**: em cada turma, 8 semanas com 6 atividades vencidas (já fechadas e contabilizadas) e uma aberta; a turma da aluna vitrine tem mais duas abertas. Cada aluno simulado tem um perfil de acerto e um tópico mais difícil, participa conforme o engajamento (alguns só marcam e não enviam), faz práticas adaptativas e melhora nos tópicos praticados, o que aparece no "efeito da prática" do painel do professor.
- **Auditoria**: logins da última semana (painel do administrador), criação das atividades, votos nas questões e geração das práticas.

A carga usa sementes fixas: cada recriação do banco (`docker compose down -v`) gera os mesmos alunos, turmas, questões e atividades, com as datas acompanhando o dia da subida.

> Os dados são simulados: servem para ver os painéis funcionando, não como evidência de resultado pedagógico.

## Testes

```bash
cd backend
./mvnw test
```

- **Unitários** (JUnit 5 + Mockito): entidades e regras de negócio — RN01 a RN06, autenticação, LGPD, motor adaptativo.
- **Integração** (`@SpringBootTest` + H2): fluxos HTTP reais — login, questões, quórum, fluxo completo turma → atividade com prazo → marcação e envio → fechamento no prazo → correção e desempenho → prática adaptativa (recomendada e por tópico), painel da escola com minimização, lista paginada de usuários, revogação de sessão, isolamento da auditoria e novo aceite dos termos.

> Os testes de integração usam H2 com o Flyway desligado; as restrições específicas do PostgreSQL das migrations (EXCLUDE, trigger da RN04, CHECKs) não são exercitadas por eles.

## Estrutura do repositório

```
maisedu/
├── backend/            # API REST (Spring Boot) — Dockerfile próprio
│   └── src/main/resources/db/migration/   # V1..V11 (Flyway)
├── frontend/           # SPA (React + Vite) — Dockerfile + nginx.conf
├── docs/
│   ├── diagrams/       # arquitetura e diagrama entidade-relacionamento (PNG + fonte .dot)
│   ├── integracao-resend.md
│   ├── ALTERACOES.md   # o que mudou nesta versão e como foi verificado
│   └── AJUSTES_MONOGRAFIA.md
├── docker-compose.yml
└── .env.example
```

## Variáveis de ambiente (back-end)

| Variável | Uso |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexão com o PostgreSQL |
| `JWT_SECRET` | Segredo do JWT (mínimo 32 bytes; a aplicação não sobe com menos) |
| `JWT_COOKIE_SECURE` | `true` atrás de HTTPS |
| `RESEND_API_KEY`, `RESEND_REMETENTE` | E-mail transacional. Com `onboarding@resend.dev`, o Resend só entrega para o e-mail dono da conta; para outros destinatários, verifique um domínio |
| `APP_URL` | Endereço do sistema usado nos links dos e-mails (padrão `http://localhost:8081`) |
| `CORS_ORIGENS` | Só se o front chamar a API em outra origem |
| `FORWARD_HEADERS_STRATEGY` | `native` atrás de proxy reverso (o compose já define) |
| `SPRING_PROFILES_ACTIVE` | `dev` (cenário de demonstração, com duas escolas e 1000 alunos simulados); outro valor, como `prod`, cria só a instituição e o admin iniciais |
| `TZ` | Fuso do servidor, usado nos prazos e no fechamento das atividades (padrão do compose: `America/Sao_Paulo`) |

Parâmetros de negócio (em `application.properties`): versão vigente dos termos (3), retenção da auditoria (365 dias), parâmetros do motor adaptativo (3 tópicos, 10 questões, 5 respostas, 70%; 5 questões na prática de um tópico) e intervalo do fechamento das atividades vencidas (60 s).

## Equipe

- Gustavo André dos Passos Chelucci
- Zacarias Alves Lins

## Licença

Projeto acadêmico desenvolvido para fins de Projeto Final de Curso (PFC) da UMC.
