import GerenciarTurmas from "./pages/GerenciarTurmas";
import CadastrarProfessor from "./pages/CadastrarProfessor";
import VincularProfessor from "./pages/VincularProfessor";
import GerenciarUsuarios from "./pages/GerenciarUsuarios";
import ConsultarAuditoria from "./pages/ConsultarAuditoria";
import DesempenhoTurma from "./pages/DesempenhoTurma";
import AtividadesProfessor from "./pages/AtividadesProfessor";
import CriarQuestao from "./pages/CriarQuestao";
import AvaliarQuestao from "./pages/AvaliarQuestao";
import CadastrarAluno from "./pages/CadastrarAluno";
import VincularAluno from "./pages/VincularAluno";
import MeuDesempenho from "./pages/MeuDesempenho";
import MinhasAtividades from "./pages/MinhasAtividades";

/**
 * Telas da área logada, agrupadas por perfil. A mesma lista monta as rotas (App.jsx) e o menu (Layout).
 * O menu só esconde o que o perfil não pode usar; o servidor confere o papel em toda requisição (RNF04).
 * O admin também tem o papel PROFESSOR, então vê os dois primeiros grupos.
 */
export const GRUPOS = [
  {
    titulo: "Gestão",
    papeis: ["ADMIN"],
    paginas: [
      { caminho: "/turmas", rotulo: "Turmas", componente: GerenciarTurmas },
      { caminho: "/professores/novo", rotulo: "Cadastrar professor", componente: CadastrarProfessor },
      { caminho: "/turmas/professores", rotulo: "Vincular professor", componente: VincularProfessor },
      { caminho: "/usuarios", rotulo: "Usuários (LGPD)", componente: GerenciarUsuarios },
      { caminho: "/auditoria", rotulo: "Auditoria", componente: ConsultarAuditoria },
    ],
  },
  {
    titulo: "Professor",
    papeis: ["PROFESSOR"],
    paginas: [
      { caminho: "/painel-turma", rotulo: "Painel de desempenho", componente: DesempenhoTurma },
      { caminho: "/atividades", rotulo: "Atividades", componente: AtividadesProfessor },
      { caminho: "/questoes/nova", rotulo: "Criar questão", componente: CriarQuestao },
      { caminho: "/questoes/avaliar", rotulo: "Avaliar questões", componente: AvaliarQuestao },
      { caminho: "/alunos/novo", rotulo: "Cadastrar aluno", componente: CadastrarAluno },
      { caminho: "/turmas/alunos", rotulo: "Vincular aluno", componente: VincularAluno },
      { caminho: "/meus-alunos", rotulo: "Meus alunos", componente: GerenciarUsuarios, somenteSemPapel: "ADMIN" },
    ],
  },
  {
    titulo: "Aluno",
    papeis: ["ALUNO"],
    paginas: [
      { caminho: "/meu-painel", rotulo: "Painel de desempenho", componente: MeuDesempenho },
      { caminho: "/minhas-atividades", rotulo: "Minhas atividades", componente: MinhasAtividades },
    ],
  },
];

/** Todas as páginas, cada uma com os papéis do seu grupo (usada para gerar as rotas). */
export const PAGINAS = [];
for (const grupo of GRUPOS) {
  for (const pagina of grupo.paginas) {
    PAGINAS.push({ ...pagina, papeis: grupo.papeis });
  }
}

/** O usuário pode abrir a página? (mesma regra do menu) */
export function podeAcessar(roles, pagina) {
  if (!pagina.papeis.some((papel) => roles.includes(papel))) {
    return false;
  }
  return !pagina.somenteSemPapel || !roles.includes(pagina.somenteSemPapel);
}

/** Grupos e páginas que aparecem no menu do usuário. */
export function gruposDoUsuario(roles) {
  const grupos = [];
  for (const grupo of GRUPOS) {
    const paginas = [];
    for (const pagina of grupo.paginas) {
      if (podeAcessar(roles, { ...pagina, papeis: grupo.papeis })) {
        paginas.push(pagina);
      }
    }
    if (paginas.length > 0) {
      grupos.push({ titulo: grupo.titulo, paginas });
    }
  }
  return grupos;
}

/** Tela inicial depois do login: o painel de desempenho (dashboard, RF19) do perfil; senão, a primeira tela. */
export function caminhoInicial(roles) {
  if (roles.includes("PROFESSOR")) {
    return "/painel-turma";
  }
  if (roles.includes("ALUNO")) {
    return "/meu-painel";
  }
  const grupos = gruposDoUsuario(roles);
  return grupos.length > 0 ? grupos[0].paginas[0].caminho : "/termos";
}
