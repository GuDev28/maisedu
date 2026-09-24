import { useState } from "react";
import VincularTurma from "./pages/VincularTurma";
import AvaliarQuestao from "./pages/AvaliarQuestao";
import "./App.css";

const PAGINAS = {
  vincular: { label: "RN1 - Vincular Turma", componente: VincularTurma },
  avaliar: { label: "RN2 - Avaliar Questão", componente: AvaliarQuestao },
};

function App() {
  const [paginaAtiva, setPaginaAtiva] = useState("vincular");
  const PaginaAtual = PAGINAS[paginaAtiva].componente;

  return (
    <div>
      <nav style={{ display: "flex", gap: "0.5rem", marginBottom: "1rem" }}>
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

      <PaginaAtual />
    </div>
  );
}

export default App;