import { useState } from "react";
import { AuthProvider, useAuth } from "./context/AuthContext";
import Login from "./pages/Login";
import TrocarSenha from "./pages/TrocarSenha";
import VincularTurma from "./pages/VincularTurma";
import AvaliarQuestao from "./pages/AvaliarQuestao";
import "./App.css";

const PAGINAS = {
  vincular: { label: "RN1 - Vincular Turma", componente: VincularTurma },
  avaliar: { label: "RN2 - Avaliar Questão", componente: AvaliarQuestao },
};

function AppAutenticado() {
  const { usuario, logout } = useAuth();
  const [paginaAtiva, setPaginaAtiva] = useState("vincular");
  const PaginaAtual = PAGINAS[paginaAtiva].componente;

  return (
    <div>
      <header style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
        <nav style={{ display: "flex", gap: "0.5rem" }}>
          {Object.entries(PAGINAS).map(([key, { label }]) => (
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

      <PaginaAtual />
    </div>
  );
}

function App() {
  const { usuario, carregandoSessao } = useAuth();

  if (carregandoSessao) {
    return <p style={{ textAlign: "center", marginTop: "4rem" }}>Carregando...</p>;
  }

  if (!usuario) {
    return <Login />;
  }

  if (usuario.senhaTemporaria) {
    return <TrocarSenha />;
  }

  return <AppAutenticado />;
}

export default function AppComProvider() {
  return (
    <AuthProvider>
      <App />
    </AuthProvider>
  );
}
