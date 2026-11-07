import { useEffect, useState } from "react";
import { avaliarQuestao } from "../services/avaliacaoQuestaoService";
import { listarQuestoesPendentes } from "../services/questaoService";
import { Mensagens } from "../components/Mensagens";
import { rotuloNivel } from "../components/rotulos";

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
    return <p className="pagina">Carregando questões pendentes...</p>;
  }

  return (
    <div className="pagina">
      <h2>Avaliar questões</h2>
      <p className="text-body-secondary">
        Questões de outros professores, de qualquer instituição, aguardando validação. Três votos iguais decidem
        a questão (máximo de cinco votos).
      </p>

      {pendentes.length === 0 ? (
        <p>Não há questões pendentes de avaliação no momento.</p>
      ) : (
        <div className="row g-4">
          <div className="col-lg-6">
            <div className="list-group">
              {pendentes.map((questao) => (
                <button type="button" key={questao.id} onClick={() => setQuestaoSelecionada(questao)}
                        aria-pressed={questaoSelecionada?.id === questao.id}
                        className={`list-group-item list-group-item-action${questaoSelecionada?.id === questao.id ? " active" : ""}`}>
                  <strong>#{questao.id}</strong> — {questao.enunciado}
                  <div className="small opacity-75">
                    {questao.topico} · {rotuloNivel(questao.nivelDificuldade)} · autor(a): {questao.autorNome}
                  </div>
                </button>
              ))}
            </div>
          </div>

          <div className="col-lg-6">
            {questaoSelecionada ? (
              <div className="cartao">
                <h3 className="h5">{questaoSelecionada.enunciado}</h3>
                <ol type="A" className="mb-3">
                  {questaoSelecionada.alternativas.map((alt) => (
                    <li key={alt.id} className={alt.correta ? "fw-bold text-success" : undefined}>
                      {alt.texto}
                      {alt.correta && " (marcada como correta pelo autor)"}
                    </li>
                  ))}
                </ol>

                <form onSubmit={handleSubmit}>
                  <div className="mb-3">
                    <label className="form-label" htmlFor="voto">Voto</label>
                    <select id="voto" className="form-select" value={voto} onChange={(e) => setVoto(e.target.value)}>
                      <option value={VOTO_APROVAR}>Aprovar</option>
                      <option value={VOTO_REJEITAR}>Rejeitar</option>
                    </select>
                  </div>
                  <button type="submit" className="btn btn-primary" disabled={carregando}>
                    {carregando ? "Enviando..." : "Enviar avaliação"}
                  </button>
                </form>
              </div>
            ) : (
              <p className="text-body-secondary">Selecione uma questão na lista para ver as alternativas e votar.</p>
            )}
          </div>
        </div>
      )}

      <Mensagens erro={erro} sucesso={sucesso} />
    </div>
  );
}
