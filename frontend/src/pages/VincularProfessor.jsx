import { useState } from "react";
import { vincularProfessor } from "../services/vinculoTurmaService";

export default function VincularProfessor() {
  const [turmaId, setTurmaId] = useState("");
  const [professorId, setProfessorId] = useState("");
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);
  const [sucesso, setSucesso] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setErro(null);
    setSucesso(null);
    setCarregando(true);

    try {
      const vinculo = await vincularProfessor(turmaId, professorId);
      setSucesso(`Professor vinculado à turma com sucesso (id ${vinculo.id}).`);
      setProfessorId("");
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div style={{ maxWidth: 420, margin: "2rem auto" }}>
      <h2>Vincular professor à turma</h2>

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 12 }}>
          <label>
            ID da turma:{" "}
            <input type="number" value={turmaId} onChange={(e) => setTurmaId(e.target.value)} required />
          </label>
        </div>

        <div style={{ marginBottom: 12 }}>
          <label>
            ID do professor:{" "}
            <input type="number" value={professorId} onChange={(e) => setProfessorId(e.target.value)} required />
          </label>
        </div>

        <button type="submit" disabled={carregando}>
          {carregando ? "Vinculando..." : "Vincular"}
        </button>
      </form>

      {erro && <p style={{ color: "red" }}>{erro}</p>}
      {sucesso && <p style={{ color: "green" }}>{sucesso}</p>}
    </div>
  );
}
