import { useEffect, useState } from "react";
import { cadastrarTurma, desativarTurma, listarTurmasParaGestao, reativarTurma } from "../services/turmaService";
import { ANOS_ESCOLARES, formatarDataHora, rotuloAno } from "../components/rotulos";
import { Mensagens } from "../components/Mensagens";

/** RF06: o administrador cadastra as turmas da instituição e pode desativá-las. */
export default function GerenciarTurmas() {
  const [turmas, setTurmas] = useState([]);
  const [nome, setNome] = useState("");
  const [anoEscolar, setAnoEscolar] = useState(ANOS_ESCOLARES[0].valor);
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState(null);
  const [sucesso, setSucesso] = useState(null);

  async function carregar() {
    try {
      setTurmas(await listarTurmasParaGestao());
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    carregar();
  }, []);

  async function handleSubmit(event) {
    event.preventDefault();
    setErro(null);
    setSucesso(null);
    setSalvando(true);
    try {
      const turma = await cadastrarTurma(nome, anoEscolar);
      setSucesso(`Turma "${turma.nome}" cadastrada. Agora vincule os professores em "Vincular Professor à Turma".`);
      setNome("");
      await carregar();
    } catch (err) {
      setErro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  async function alternar(turma) {
    setErro(null);
    setSucesso(null);
    try {
      if (turma.ativo) {
        await desativarTurma(turma.id);
        setSucesso(`Turma "${turma.nome}" desativada: não aceita novos vínculos nem atividades.`);
      } else {
        await reativarTurma(turma.id);
        setSucesso(`Turma "${turma.nome}" reativada.`);
      }
      await carregar();
    } catch (err) {
      setErro(err.message);
    }
  }

  return (
    <div className="pagina">
      <h2>Turmas</h2>

      <form onSubmit={handleSubmit} className="cartao">
        <div className="campos">
          <label>
            Nome da turma
            <input value={nome} onChange={(e) => setNome(e.target.value)} placeholder="Ex.: 7º ano A" required
                   maxLength={100} />
          </label>
          <label>
            Ano escolar
            <select value={anoEscolar} onChange={(e) => setAnoEscolar(e.target.value)}>
              {ANOS_ESCOLARES.map((ano) => (
                <option key={ano.valor} value={ano.valor}>{ano.rotulo}</option>
              ))}
            </select>
          </label>
          <button type="submit" disabled={salvando}>{salvando ? "Salvando..." : "Cadastrar turma"}</button>
        </div>
      </form>

      <Mensagens erro={erro} sucesso={sucesso} />

      {carregando ? (
        <p>Carregando...</p>
      ) : turmas.length === 0 ? (
        <p>Nenhuma turma cadastrada ainda.</p>
      ) : (
        <table className="tabela">
          <thead>
            <tr>
              <th>Turma</th>
              <th>Ano</th>
              <th>Situação</th>
              <th>Criada em</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {turmas.map((turma) => (
              <tr key={turma.id}>
                <td>{turma.nome}</td>
                <td>{rotuloAno(turma.anoEscolar)}</td>
                <td>{turma.ativo ? "Ativa" : <span className="etiqueta etiqueta-neutra">Inativa</span>}</td>
                <td>{formatarDataHora(turma.criadoEm)}</td>
                <td>
                  <button type="button" onClick={() => alternar(turma)}>
                    {turma.ativo ? "Desativar" : "Reativar"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
