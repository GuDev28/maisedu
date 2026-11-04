import { useState } from "react";
import { AuthProvider, useAuth } from "./context/AuthContext";
import Login from "./pages/Login";
import TrocarSenha from "./pages/TrocarSenha";
import AceitarTermos from "./pages/AceitarTermos";
import CadastrarProfessor from "./pages/CadastrarProfessor";
import CadastrarAluno from "./pages/CadastrarAluno";
import VincularAluno from "./pages/VincularAluno";
import VincularProfessor from "./pages/VincularProfessor";
import AvaliarQuestao from "./pages/AvaliarQuestao";
import CriarQuestao from "./pages/CriarQuestao";
import GerenciarTurmas from "./pages/GerenciarTurmas";
import GerenciarUsuarios from "./pages/GerenciarUsuarios";
import ConsultarAuditoria from "./pages/ConsultarAuditoria";
import AtividadesProfessor from "./pages/AtividadesProfessor";
import DesempenhoTurma from "./pages/DesempenhoTurma";
import MinhasAtividades from "./pages/MinhasAtividades";
import MeuDesempenho from "./pages/MeuDesempenho";
import PoliticaPrivacidade from "./pages/PoliticaPrivacidade";
import TermosUso from "./pages/TermosUso";
import { RodapeLegal, linkStyle } from "./components/RodapeLegal";
import "./App.css";

/**
 * Telas por perfil. Sem React Router: a navegação troca a tela ativa no estado (decisão do grupo).
 * O admin também tem o papel PROFESSOR, então vê os dois grupos. O servidor confere o papel em
 * toda requisição; o menu só esconde o que o perfil não pode usar.
 */
const GRUPOS = [
  {
    titulo: "Gestão",
    roles: ["ADMIN"],
    paginas: {
      turmas: { label: "Turmas", componente: GerenciarTurmas },
      cadastrarProfessor: { label: "Cadastrar Professor", componente: CadastrarProfessor },
      vincularProfessor: { label: "Vincular Professor à Turma", componente: VincularProfessor },
      usuarios: { label: "Usuários (LGPD)", componente: GerenciarUsuarios },
      auditoria: { label: "Auditoria", componente: ConsultarAuditoria },
    },
  },
  {
    titulo: "Professor",
    roles: ["PROFESSOR"],
    paginas: {
      atividades: { label: "Atividades", componente: AtividadesProfessor },
      desempenhoTurma: { label: "Desempenho da Turma", componente: DesempenhoTurma },
      criar: { label: "Criar Questão", componente: CriarQuestao },
      avaliar: { label: "Avaliar Questão", componente: AvaliarQuestao },
      cadastrarAluno: { label: "Cadastrar Aluno", componente: CadastrarAluno },
      vincularAluno: { label: "Vincular Aluno à Turma", componente: VincularAluno },
      meusAlunos: { label: "Meus Alunos", componente: GerenciarUsuarios, somenteSemRole: "ADMIN" },
    },
  },
  {
    titulo: "Aluno",
    roles: ["ALUNO"],
    paginas: {
      minhasAtividades: { label: "Minhas Atividades", componente: MinhasAtividades },
      meuDesempenho: { label: "Meu Desempenho", componente: MeuDesempenho },
    },
  },
];

function gruposDoUsuario(roles) {
  const grupos = [];
  for (const grupo of GRUPOS) {
    if (!grupo.roles.some((role) => roles.includes(role))) {
      continue;
    }
    const paginas = Object.entries(grupo.paginas).filter(
      ([, pagina]) => !pagina.somenteSemRole || !roles.includes(pagina.somenteSemRole)
    );
    grupos.push({ titulo: grupo.titulo, paginas });
  }
  return grupos;
}

function AppAutenticado({ onAbrirPaginaLegal }) {
  const { usuario, logout } = useAuth();
  const grupos = gruposDoUsuario(usuario.roles);
  const todas = grupos.flatMap((grupo) => grupo.paginas);
  const [paginaAtiva, setPaginaAtiva] = useState(todas[0]?.[0] ?? null);
  const entrada = todas.find(([chave]) => chave === paginaAtiva);
  const PaginaAtual = entrada ? entrada[1].componente : null;

  return (
    <div>
      <header className="cabecalho">
        <nav className="nav-grupos" aria-label="Menu principal">
          {grupos.map((grupo) => (
            <div key={grupo.titulo} className="nav-grupo">
              {grupos.length > 1 && <span className="nav-grupo-titulo">{grupo.titulo}</span>}
              {grupo.paginas.map(([chave, { label }]) => (
                <button
                  key={chave}
                  onClick={() => setPaginaAtiva(chave)}
                  disabled={chave === paginaAtiva}
                  aria-current={chave === paginaAtiva ? "page" : undefined}
                >
                  {label}
                </button>
              ))}
            </div>
          ))}
        </nav>
        <div style={{ whiteSpace: "nowrap" }}>
          <span style={{ marginRight: 12 }}>{usuario.nome}</span>
          <button onClick={logout}>Sair</button>
        </div>
      </header>

      {PaginaAtual ? (
        <PaginaAtual key={paginaAtiva} />
      ) : (
        <p>Nenhuma funcionalidade disponível para o seu perfil ainda.</p>
      )}

      <RodapeLegal onAbrirPaginaLegal={onAbrirPaginaLegal} />
    </div>
  );
}

function App() {
  const { usuario, carregandoSessao } = useAuth();
  const [paginaLegal, setPaginaLegal] = useState(null);

  if (paginaLegal) {
    const PaginaLegal = paginaLegal === "termos" ? TermosUso : PoliticaPrivacidade;
    return (
      <div>
        <div style={{ textAlign: "center", marginTop: "1rem" }}>
          <button type="button" style={linkStyle} onClick={() => setPaginaLegal(null)}>
            ← Voltar
          </button>
        </div>
        <PaginaLegal />
      </div>
    );
  }

  if (carregandoSessao) {
    return <p style={{ textAlign: "center", marginTop: "4rem" }}>Carregando...</p>;
  }

  if (!usuario) {
    return <Login onAbrirPaginaLegal={setPaginaLegal} />;
  }

  if (usuario.senhaTemporaria) {
    return <TrocarSenha onAbrirPaginaLegal={setPaginaLegal} />;
  }

  if (usuario.termosPendentes) {
    return <AceitarTermos onAbrirPaginaLegal={setPaginaLegal} />;
  }

  return <AppAutenticado onAbrirPaginaLegal={setPaginaLegal} />;
}

export default function AppComProvider() {
  return (
    <AuthProvider>
      <App />
    </AuthProvider>
  );
}
