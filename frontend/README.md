# MaisEdu — front-end

SPA em React 19 + Vite, com React Router 7 e Bootstrap 5.3.

```bash
npm install      # obrigatório depois de atualizar o projeto: atualiza o package-lock.json
npm run dev      # http://localhost:5173 (o Vite repassa /api para http://localhost:8080)
npm run build    # gera dist/ (é o que o Dockerfile entrega com nginx)
npm run lint     # oxlint
```

## Organização

| Pasta / arquivo | O que tem |
|---|---|
| `src/App.jsx` | Tabela de rotas e os "portões" da sessão (senha provisória, novo aceite dos termos) |
| `src/navegacao.js` | Menu lateral por perfil (mesmo formato do antigo `GRUPOS`, agora com rotas) |
| `src/components/Layout.jsx` | Menu lateral fixo no computador e gaveta no celular, com contador de questões esperando voto |
| `src/context/` | Sessão (`AuthContext`) e avisos/confirmações em modal (`InterfaceContext`) |
| `src/pages/` | Uma tela por rota; `pages/inicio/` tem a página Início de cada perfil |
| `src/services/` | Chamadas à API (`/api/...`) |
| `src/estilo.css` | Cores da marca sobre o Bootstrap e o que ele não tem pronto (menu, barra de percentual, gráfico) |

O Bootstrap é usado só pelo CSS: modal, gaveta do menu e menu suspenso são controlados pelo React,
sem o JavaScript do Bootstrap.
