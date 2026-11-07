import { useEffect, useState } from "react";
import {
  listarTurmasParaVinculo,
  listarAlunosDisponiveis,
  vincularAluno,
} from "../services/vinculoTurmaService";
import { Mensagens } from "../components/Mensagens";
import { rotuloAno } from "../components/rotulos";

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
    return <p className="pagina">Carregando turmas...</p>;
  }

  return (
    <div className="pagina pagina-estreita">
      <h2>Vincular aluno à turma</h2>

      {turmas.length === 0 ? (
        <p>Nenhuma turma disponível para vínculo no momento.</p>
      ) : (
        <>
          <div className="mb-3">
            <label className="form-label w-100">
              Turma
              <select className="form-select mt-1" value={turmaId} onChange={(e) => setTurmaId(e.target.value)}>
                {turmas.map((turma) => (
                  <option key={turma.id} value={turma.id}>
                    {turma.nome} ({rotuloAno(turma.anoEscolar)})
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
              <div className="list-group mb-3">
                {alunos.map((aluno) => (
                  <button type="button" key={aluno.id} onClick={() => setAlunoSelecionado(aluno)}
                          aria-pressed={alunoSelecionado?.id === aluno.id}
                          className={`list-group-item list-group-item-action${alunoSelecionado?.id === aluno.id ? " active" : ""}`}>
                    <strong>{aluno.nome}</strong> — <small>{aluno.identificadorDeAcesso}</small>
                  </button>
                ))}
              </div>

              <button className="btn btn-primary" type="submit" disabled={carregando || !alunoSelecionado}>
                {carregando ? "Vinculando..." : "Vincular aluno selecionado"}
              </button>
            </form>
          )}
        </>
      )}

      <Mensagens erro={erro} sucesso={sucesso} />
    </div>
  );
}
