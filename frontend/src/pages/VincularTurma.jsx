import { useState } from "react";
import { vincularAluno, vincularProfessor } from "../services/vinculoTurmaService";

const TIPO_ALUNO = "ALUNO";
const TIPO_PROFESSOR = "PROFESSOR";

export default function VincularTurma() {
  const [tipo, setTipo] = useState(TIPO_ALUNO);
  const [turmaId, setTurmaId] = useState("");
  const [usuarioId, setUsuarioId] = useState("");
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);
  const [sucesso, setSucesso] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setErro(null);
    setSucesso(null);
    setCarregando(true);

    try {
      const vinculo =
        tipo === TIPO_ALUNO
          ? await vincularAluno(turmaId, usuarioId)
          : await vincularProfessor(turmaId, usuarioId);

      setSucesso(
        `Vínculo criado com sucesso (id ${vinculo.id}).`
      );
      setUsuarioId("");
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div style={{ maxWidth: 420, margin: "2rem auto" }}>
      <h2>Vincular usuário à turma</h2>

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 12 }}>
          <label>
            Papel:{" "}
            <select value={tipo} onChange={(e) => setTipo(e.target.value)}>
              <option value={TIPO_ALUNO}>Aluno</option>
              <option value={TIPO_PROFESSOR}>Professor</option>
            </select>
          </label>
        </div>

        <div style={{ marginBottom: 12 }}>
          <label>
            ID da turma:{" "}
            <input
              type="number"
              value={turmaId}
              onChange={(e) => setTurmaId(e.target.value)}
              required
            />
          </label>
        </div>

        <div style={{ marginBottom: 12 }}>
          <label>
            ID do usuário:{" "}
            <input
              type="number"
              value={usuarioId}
              onChange={(e) => setUsuarioId(e.target.value)}
              required
            />
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
