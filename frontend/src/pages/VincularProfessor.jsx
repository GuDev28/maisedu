import { useEffect, useState } from "react";
import {
  listarTurmasParaVinculo,
  listarProfessoresDisponiveis,
  vincularProfessor,
} from "../services/vinculoTurmaService";

export default function VincularProfessor() {
  const [turmas, setTurmas] = useState([]);
  const [turmaId, setTurmaId] = useState("");
  const [professores, setProfessores] = useState([]);
  const [professorSelecionado, setProfessorSelecionado] = useState(null);
  const [carregandoTurmas, setCarregandoTurmas] = useState(true);
  const [carregandoProfessores, setCarregandoProfessores] = useState(false);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);
  const [sucesso, setSucesso] = useState(null);

  useEffect(() => {
    async function carregarTurmas() {
      setCarregandoTurmas(true);
      try {
        const lista = await listarTurmasParaVinculo();
        setTurmas(lista);
        setTurmaId((atual) => atual || (lista[0] ? String(lista[0].id) : ""));
      } catch (err) {
        setErro(err.message);
      } finally {
        setCarregandoTurmas(false);
      }
    }
    carregarTurmas();
  }, []);

  async function carregarProfessores(id) {
    if (!id) {
      setProfessores([]);
      return;
    }
    setCarregandoProfessores(true);
    setProfessorSelecionado(null);
    try {
      const lista = await listarProfessoresDisponiveis(id);
      setProfessores(lista);
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregandoProfessores(false);
    }
  }

  useEffect(() => {
    carregarProfessores(turmaId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [turmaId]);

  async function handleSubmit(event) {
    event.preventDefault();
    setErro(null);
    setSucesso(null);

    if (!professorSelecionado) {
      return;
    }

    setCarregando(true);
    try {
      await vincularProfessor(turmaId, professorSelecionado.id);
      setSucesso(`${professorSelecionado.nome} vinculado(a) à turma com sucesso.`);
      await carregarProfessores(turmaId);
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  if (carregandoTurmas) {
    return <p style={{ textAlign: "center" }}>Carregando turmas...</p>;
  }

  return (
    <div style={{ maxWidth: 480, margin: "2rem auto" }}>
      <h2>Vincular professor à turma</h2>

      {turmas.length === 0 ? (
        <p>Nenhuma turma disponível para vínculo no momento.</p>
      ) : (
        <>
          <div style={{ marginBottom: 16 }}>
            <label>
              Turma:{" "}
              <select value={turmaId} onChange={(e) => setTurmaId(e.target.value)}>
                {turmas.map((turma) => (
                  <option key={turma.id} value={turma.id}>
                    {turma.nome} ({turma.anoEscolar})
                  </option>
                ))}
              </select>
            </label>
          </div>

          {carregandoProfessores ? (
            <p>Carregando professores disponíveis...</p>
          ) : professores.length === 0 ? (
            <p>Não há professores disponíveis para vincular a esta turma.</p>
          ) : (
            <form onSubmit={handleSubmit}>
              <ul style={{ listStyle: "none", padding: 0 }}>
                {professores.map((professor) => (
                  <li
                    key={professor.id}
                    onClick={() => setProfessorSelecionado(professor)}
                    style={{
                      border: "1px solid #ddd",
                      borderRadius: 4,
                      padding: 8,
                      marginBottom: 8,
                      cursor: "pointer",
                      background: professorSelecionado?.id === professor.id ? "#eef" : "transparent",
                    }}
                  >
                    <strong>{professor.nome}</strong> — <small>{professor.identificadorDeAcesso}</small>
                  </li>
                ))}
              </ul>

              <button type="submit" disabled={carregando || !professorSelecionado}>
                {carregando ? "Vinculando..." : "Vincular professor selecionado"}
              </button>
            </form>
          )}
        </>
      )}

      {erro && <p style={{ color: "red" }}>{erro}</p>}
      {sucesso && <p style={{ color: "green" }}>{sucesso}</p>}
    </div>
  );
}
