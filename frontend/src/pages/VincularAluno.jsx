import { useState } from "react";
import { vincularAluno } from "../services/vinculoTurmaService";

export default function VincularAluno() {
  const [turmaId, setTurmaId] = useState("");
  const [alunoId, setAlunoId] = useState("");
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);
  const [sucesso, setSucesso] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setErro(null);
    setSucesso(null);
    setCarregando(true);

    try {
      const vinculo = await vincularAluno(turmaId, alunoId);
      setSucesso(`Aluno vinculado à turma com sucesso (id ${vinculo.id}).`);
      setAlunoId("");
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div style={{ maxWidth: 420, margin: "2rem auto" }}>
      <h2>Vincular aluno à turma</h2>

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 12 }}>
          <label>
            ID da turma:{" "}
            <input type="number" value={turmaId} onChange={(e) => setTurmaId(e.target.value)} required />
          </label>
        </div>

        <div style={{ marginBottom: 12 }}>
          <label>
            ID do aluno:{" "}
            <input type="number" value={alunoId} onChange={(e) => setAlunoId(e.target.value)} required />
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
