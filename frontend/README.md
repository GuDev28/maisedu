# MaisEdu — front-end

SPA em React 19 + Vite, com React Router 7 (navegação) e Bootstrap 5 (interface). Visão geral do projeto, requisitos e como subir tudo com Docker: veja o
[README principal](../README.md).

## Executar em desenvolvimento

Com o back-end rodando na porta 8080:

```bash
npm install
npm run dev        # http://localhost:5173
```

O front chama a API sempre em `/api`. Em desenvolvimento, o Vite repassa `/api` para
`http://localhost:8080` (outro endereço: variável `VITE_API_PROXY`); no Docker, quem faz isso é o nginx
(`nginx.conf`). Assim front e API ficam na mesma origem e o cookie de sessão `SameSite=Strict` funciona.

## Outros comandos

```bash
npm run build      # gera dist/ (usado pelo Dockerfile)
npm run lint       # oxlint
```

## Organização

```
src/
├── App.jsx            # rotas (React Router): públicas, primeiro acesso e área logada por perfil
├── navegacao.js       # telas de cada perfil: gera as rotas e o menu lateral
├── context/           # sessão do usuário (AuthContext)
├── services/          # uma função por endpoint da API (http.js trata erros e sessão expirada)
├── pages/             # uma tela por funcionalidade (RF01–RF19 e LGPD)
└── components/        # layout (barra + menu), painel de desempenho, gráficos, big numbers, mensagens
```

Os gráficos do painel de desempenho (RF19) são componentes próprios em SVG/CSS (`GraficoEvolucao`,
`GraficoBarras`), sem biblioteca de gráficos.

## Rotas

| Rota | Quem acessa |
|---|---|
| `/login`, `/termos`, `/privacidade` | Qualquer pessoa (os documentos legais abrem sem login, RF14) |
| `/trocar-senha`, `/aceitar-termos` | Usuário com senha provisória ou termos pendentes (RF02) |
| `/turmas`, `/professores/novo`, `/turmas/professores`, `/usuarios`, `/auditoria` | Administrador |
| `/painel-turma`, `/atividades`, `/questoes/nova`, `/questoes/avaliar`, `/alunos/novo`, `/turmas/alunos` | Professor (o admin também) |
| `/meus-alunos` | Professor sem papel de admin (o admin usa `/usuarios`) |
| `/meu-painel`, `/minhas-atividades` | Aluno |

Quem abre uma rota de outro perfil volta para a tela inicial; sem sessão, vai para o login e, depois de
entrar, retorna à página que tentou abrir. As rotas só organizam a navegação: a API confere o papel em toda
requisição (RNF04). Em produção, o nginx devolve o `index.html` para qualquer caminho (`try_files`), então
as URLs funcionam ao recarregar a página ou abrir um link direto.
