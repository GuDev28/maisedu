import { useState } from "react";
import { AuthProvider, useAuth } from "./context/AuthContext";
import Login from "./pages/Login";
import TrocarSenha from "./pages/TrocarSenha";
import VincularTurma from "./pages/VincularTurma";
import AvaliarQuestao from "./pages/AvaliarQuestao";
import CriarQuestao from "./pages/CriarQuestao";
import CadastrarUsuario from "./pages/CadastrarUsuario";
import PoliticaPrivacidade from "./pages/PoliticaPrivacidade";
import TermosUso from "./pages/TermosUso";
import { RodapeLegal, linkStyle } from "./components/RodapeLegal";
import "./App.css";

const PAGINAS = {
  cadastrar: { label: "Cadastrar Usuário", componente: CadastrarUsuario, roles: ["ADMIN", "PROFESSOR"] },
  vincular: { label: "RN1 - Vincular Turma", componente: VincularTurma, roles: ["ADMIN", "PROFESSOR"] },
  criar: { label: "Criar Questão", componente: CriarQuestao, roles: ["PROFESSOR"] },
  avaliar: { label: "RN2 - Avaliar Questão", componente: AvaliarQuestao, roles: ["PROFESSOR"] },
};

function AppAutenticado({ onAbrirPaginaLegal }) {
  const { usuario, logout } = useAuth();
  const paginasVisiveis = Object.entries(PAGINAS).filter(([, pagina]) =>
    pagina.roles.some((role) => usuario.roles.includes(role))
  );
  const [paginaAtiva, setPaginaAtiva] = useState(paginasVisiveis[0]?.[0] ?? null);
  const PaginaAtual = paginaAtiva ? PAGINAS[paginaAtiva].componente : null;

  return (
    <div>
      <header style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
        <nav style={{ display: "flex", gap: "0.5rem" }}>
          {paginasVisiveis.map(([key, { label }]) => (
            <button
              key={key}
              onClick={() => setPaginaAtiva(key)}
              disabled={key === paginaAtiva}
            >
              {label}
            </button>
          ))}
        </nav>
        <div>
          <span style={{ marginRight: 12 }}>
            {usuario.nome} ({usuario.roles.join(", ")})
          </span>
          <button onClick={logout}>Sair</button>
        </div>
      </header>

      {PaginaAtual ? (
        <PaginaAtual />
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

  return <AppAutenticado onAbrirPaginaLegal={setPaginaLegal} />;
}

export default function AppComProvider() {
  return (
    <AuthProvider>
      <App />
    </AuthProvider>
  );
}
