import { useEffect, useState } from "react";
import { avaliarQuestao } from "../services/avaliacaoQuestaoService";
import { listarQuestoesPendentes } from "../services/questaoService";

const VOTO_APROVAR = "APROVAR";
const VOTO_REJEITAR = "REJEITAR";

function montarMensagem({ status, aprovacoes, rejeicoes, votosParaDecidir }) {
  const placar = `Placar: ${aprovacoes} aprovação(ões) e ${rejeicoes} rejeição(ões).`;

  if (status === "APROVADA") {
    return `Voto registrado. ${placar} A questão atingiu ${votosParaDecidir} aprovações e entrou no banco compartilhado.`;
  }
  if (status === "REJEITADA") {
    return `Voto registrado. ${placar} A questão atingiu ${votosParaDecidir} rejeições e foi rejeitada.`;
  }
  return `Voto registrado. ${placar} A questão continua pendente até ${votosParaDecidir} votos iguais.`;
}

export default function AvaliarQuestao() {
  const [pendentes, setPendentes] = useState([]);
  const [carregandoLista, setCarregandoLista] = useState(true);
  const [questaoSelecionada, setQuestaoSelecionada] = useState(null);
  const [voto, setVoto] = useState(VOTO_APROVAR);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);
  const [sucesso, setSucesso] = useState(null);

  async function carregarPendentes() {
    setCarregandoLista(true);
    try {
      const lista = await listarQuestoesPendentes();
      setPendentes(lista);
      setQuestaoSelecionada((atual) =>
        atual && lista.some((q) => q.id === atual.id) ? atual : lista[0] ?? null
      );
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregandoLista(false);
    }
  }

  useEffect(() => {
    carregarPendentes();
  }, []);

  async function handleSubmit(event) {
    event.preventDefault();
    setErro(null);
    setSucesso(null);

    if (!questaoSelecionada) {
      return;
    }

    setCarregando(true);
    try {
      const resultado = await avaliarQuestao(questaoSelecionada.id, voto);
      setSucesso(montarMensagem(resultado));
      await carregarPendentes();
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  if (carregandoLista) {
    return <p style={{ textAlign: "center" }}>Carregando questões pendentes...</p>;
  }

  return (
    <div style={{ maxWidth: 640, margin: "2rem auto" }}>
      <h2>Avaliar questão</h2>

      {pendentes.length === 0 ? (
        <p>Não há questões pendentes de avaliação no momento.</p>
      ) : (
        <div style={{ display: "flex", gap: "1.5rem" }}>
          <ul style={{ listStyle: "none", padding: 0, flex: 1 }}>
            {pendentes.map((questao) => (
              <li
                key={questao.id}
                onClick={() => setQuestaoSelecionada(questao)}
                style={{
                  border: "1px solid #ddd",
                  borderRadius: 4,
                  padding: 8,
                  marginBottom: 8,
                  cursor: "pointer",
                  background: questaoSelecionada?.id === questao.id ? "#eef" : "transparent",
                }}
              >
                <strong>#{questao.id}</strong> — {questao.enunciado}
                <br />
                <small>
                  {questao.topico} · {questao.nivelDificuldade} · autor: {questao.autorNome}
                </small>
              </li>
            ))}
          </ul>

          {questaoSelecionada && (
            <div style={{ flex: 1 }}>
              <h3>{questaoSelecionada.enunciado}</h3>
              <ol type="A">
                {questaoSelecionada.alternativas.map((alt) => (
                  <li key={alt.id} style={{ fontWeight: alt.correta ? "bold" : "normal" }}>
                    {alt.texto}
                    {alt.correta && " (marcada como correta pelo autor)"}
                  </li>
                ))}
              </ol>

              <form onSubmit={handleSubmit}>
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
            </div>
          )}
        </div>
      )}

      {erro && <p style={{ color: "red" }}>{erro}</p>}
      {sucesso && <p style={{ color: "green" }}>{sucesso}</p>}
    </div>
  );
}
