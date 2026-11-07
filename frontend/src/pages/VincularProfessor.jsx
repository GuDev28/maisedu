import { useEffect, useState } from "react";
import {
  listarTurmasParaVinculo,
  listarProfessoresDisponiveis,
  vincularProfessor,
} from "../services/vinculoTurmaService";
import { Mensagens } from "../components/Mensagens";
import { rotuloAno } from "../components/rotulos";

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
    return <p className="pagina">Carregando turmas...</p>;
  }

  return (
    <div className="pagina pagina-estreita">
      <h2>Vincular professor à turma</h2>

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

          {carregandoProfessores ? (
            <p>Carregando professores disponíveis...</p>
          ) : professores.length === 0 ? (
            <p>Não há professores disponíveis para vincular a esta turma.</p>
          ) : (
            <form onSubmit={handleSubmit}>
              <div className="list-group mb-3">
                {professores.map((professor) => (
                  <button type="button" key={professor.id} onClick={() => setProfessorSelecionado(professor)}
                          aria-pressed={professorSelecionado?.id === professor.id}
                          className={`list-group-item list-group-item-action${professorSelecionado?.id === professor.id ? " active" : ""}`}>
                    <strong>{professor.nome}</strong> — <small>{professor.identificadorDeAcesso}</small>
                  </button>
                ))}
              </div>

              <button className="btn btn-primary" type="submit" disabled={carregando || !professorSelecionado}>
                {carregando ? "Vinculando..." : "Vincular professor selecionado"}
              </button>
            </form>
          )}
        </>
      )}

      <Mensagens erro={erro} sucesso={sucesso} />
    </div>
  );
}
