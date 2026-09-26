import { useEffect, useState } from "react";
import {
  listarTurmasParaVinculo,
  listarAlunosDisponiveis,
  vincularAluno,
} from "../services/vinculoTurmaService";

export default function VincularAluno() {
  const [turmas, setTurmas] = useState([]);
  const [turmaId, setTurmaId] = useState("");
  const [alunos, setAlunos] = useState([]);
  const [alunoSelecionado, setAlunoSelecionado] = useState(null);
  const [carregandoTurmas, setCarregandoTurmas] = useState(true);
  const [carregandoAlunos, setCarregandoAlunos] = useState(false);
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

  async function carregarAlunos(id) {
    if (!id) {
      setAlunos([]);
      return;
    }
    setCarregandoAlunos(true);
    setAlunoSelecionado(null);
    try {
      const lista = await listarAlunosDisponiveis(id);
      setAlunos(lista);
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregandoAlunos(false);
    }
  }

  useEffect(() => {
    carregarAlunos(turmaId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [turmaId]);

  async function handleSubmit(event) {
    event.preventDefault();
    setErro(null);
    setSucesso(null);

    if (!alunoSelecionado) {
      return;
    }

    setCarregando(true);
    try {
      await vincularAluno(turmaId, alunoSelecionado.id);
      setSucesso(`${alunoSelecionado.nome} vinculado(a) à turma com sucesso.`);
      await carregarAlunos(turmaId);
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
      <h2>Vincular aluno à turma</h2>

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

          {carregandoAlunos ? (
            <p>Carregando alunos disponíveis...</p>
          ) : alunos.length === 0 ? (
            <p>Não há alunos disponíveis para vincular a esta turma.</p>
          ) : (
            <form onSubmit={handleSubmit}>
              <ul style={{ listStyle: "none", padding: 0 }}>
                {alunos.map((aluno) => (
                  <li
                    key={aluno.id}
                    onClick={() => setAlunoSelecionado(aluno)}
                    style={{
                      border: "1px solid #ddd",
                      borderRadius: 4,
                      padding: 8,
                      marginBottom: 8,
                      cursor: "pointer",
                      background: alunoSelecionado?.id === aluno.id ? "#eef" : "transparent",
                    }}
                  >
                    <strong>{aluno.nome}</strong> — <small>{aluno.identificadorDeAcesso}</small>
                  </li>
                ))}
              </ul>

              <button type="submit" disabled={carregando || !alunoSelecionado}>
                {carregando ? "Vinculando..." : "Vincular aluno selecionado"}
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
