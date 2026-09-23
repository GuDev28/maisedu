import { useState } from "react";
import { avaliarQuestao } from "../services/avaliacaoQuestaoService";

const VOTO_APROVAR = "APROVAR";
const VOTO_REJEITAR = "REJEITAR";

export default function AvaliarQuestao() {
  const [questaoId, setQuestaoId] = useState("");
  const [professorId, setProfessorId] = useState("");
  const [voto, setVoto] = useState(VOTO_APROVAR);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);
  const [sucesso, setSucesso] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setErro(null);
    setSucesso(null);
    setCarregando(true);

    try {
      await avaliarQuestao(questaoId, professorId, voto);

      setSucesso(
        voto === VOTO_APROVAR
          ? "Questão aprovada com sucesso."
          : "Questão rejeitada com sucesso."
      );
      setProfessorId("");
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div style={{ maxWidth: 420, margin: "2rem auto" }}>
      <h2>Avaliar questão</h2>

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 12 }}>
          <label>
            ID da questão:{" "}
            <input
              type="number"
              value={questaoId}
              onChange={(e) => setQuestaoId(e.target.value)}
              required
            />
          </label>
        </div>

        <div style={{ marginBottom: 12 }}>
          <label>
            ID do professor:{" "}
            <input
              type="number"
              value={professorId}
              onChange={(e) => setProfessorId(e.target.value)}
              required
            />
          </label>
        </div>

        <div style={{ marginBottom: 12 }}>
          <label>
            Voto:{" "}
            <select value={voto} onChange={(e) => setVoto(e.target.value)}>
              <option value={VOTO_APROVAR}>Aprovar</option>
              <option value={VOTO_REJEITAR}>Rejeitar</option>
            </select>
          </label>
        </div>

        <button type="submit" disabled={carregando}>
          {carregando ? "Enviando..." : "Enviar avaliação"}
        </button>
      </form>

      {erro && <p style={{ color: "red" }}>{erro}</p>}
      {sucesso && <p style={{ color: "green" }}>{sucesso}</p>}
    </div>
  );
}
