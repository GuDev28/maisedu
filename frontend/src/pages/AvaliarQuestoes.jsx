import { useEffect, useState } from "react";
import { useInterface } from "../context/InterfaceContext";
import { avaliarQuestao } from "../services/avaliacaoQuestaoService";
import { listarQuestoesPendentes } from "../services/questaoService";
import { EVENTO_FILA_ATUALIZADA } from "../components/Layout";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { AlternativasComGabarito, EtiquetasQuestao } from "../components/EtiquetasQuestao";
import { EstadoVazio } from "../components/EstadoVazio";
import { Mensagens } from "../components/Mensagens";

function mensagemDoResultado({ status, aprovacoes, rejeicoes, votosParaDecidir }) {
  const placar = `${aprovacoes} aprovação(ões) e ${rejeicoes} rejeição(ões).`;
  if (status === "APROVADA") {
    return `Voto registrado. Com ${placar} a questão foi aprovada e entrou no banco compartilhado.`;
  }
  if (status === "REJEITADA") {
    return `Voto registrado. Com ${placar} a questão foi rejeitada; o autor vai ler os motivos.`;
  }
  return `Voto registrado: ${placar} Ela continua em validação até ${votosParaDecidir} votos iguais.`;
}

/**
 * RN02: validação colaborativa. O professor vê as questões de outros autores que ainda não votou
 * (de qualquer escola) e aprova ou rejeita; ao rejeitar, escreve o motivo para o autor.
 */
export default function AvaliarQuestoes() {
  const { avisar } = useInterface();
  const [pendentes, setPendentes] = useState(null);
  const [selecionada, setSelecionada] = useState(null);
  const [rejeitando, setRejeitando] = useState(false);
  const [motivo, setMotivo] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState(null);

  async function carregar() {
    try {
      const lista = await listarQuestoesPendentes();
      setPendentes(lista);
      setSelecionada((atual) => (atual && lista.some((q) => q.id === atual.id) ? atual : lista[0] ?? null));
    } catch (err) {
      setErro(err.message);
    }
  }

  useEffect(() => {
    carregar();
  }, []);

  function selecionar(questao) {
    setSelecionada(questao);
    setRejeitando(false);
    setMotivo("");
    setErro(null);
  }

  async function votar(voto) {
    setErro(null);
    setEnviando(true);
    try {
      const resultado = await avaliarQuestao(selecionada.id, voto, voto === "REJEITAR" ? motivo : null);
      avisar(mensagemDoResultado(resultado), resultado.status === "REJEITADA" ? "secondary" : "success");
      setRejeitando(false);
      setMotivo("");
      window.dispatchEvent(new Event(EVENTO_FILA_ATUALIZADA));
      await carregar();
    } catch (err) {
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <>
      <Cabecalho titulo="Avaliar questões">
        Questões de outros professores esperando o seu voto. Uma questão entra no banco com 3 aprovações e sai com 3
        rejeições.
      </Cabecalho>
      {!pendentes && !erro && <Carregando />}
      {pendentes?.length === 0 && (
        <EstadoVazio titulo="Nenhuma questão esperando o seu voto">
          Quando outro professor enviar uma questão para validação, ela aparece aqui.
        </EstadoVazio>
      )}
      {pendentes?.length > 0 && (
        <div className="row g-4">
          <div className="col-lg-5">
            <div className="list-group">
              {pendentes.map((questao) => (
                <button
                  key={questao.id}
                  type="button"
                  className={`list-group-item list-group-item-action ${selecionada?.id === questao.id ? "active-subtle border-primary" : ""}`}
                  aria-current={selecionada?.id === questao.id ? "true" : undefined}
                  onClick={() => selecionar(questao)}
                >
                  <div className="text-truncate">{questao.enunciado}</div>
                  <div className="small text-secondary">
                    {questao.topico} · {questao.autorInstituicao ?? questao.autorNome}
                  </div>
                </button>
              ))}
            </div>
          </div>
          <div className="col-lg-7">
            {selecionada && (
              <div className="card">
                <div className="card-body p-4">
                  <EtiquetasQuestao questao={selecionada} />
                  <p className="fs-5 mt-3">{selecionada.enunciado}</p>
                  <AlternativasComGabarito alternativas={selecionada.alternativas} />
                  <p className="small text-secondary mt-3 mb-1">
                    Autor(a): {selecionada.autorNome}
                    {selecionada.autorInstituicao && ` · ${selecionada.autorInstituicao}`}
                  </p>
                  {selecionada.aprovacoes !== null && selecionada.aprovacoes !== undefined && (
                    <p className="small mb-0">
                      Placar até agora: <strong className="text-success">{selecionada.aprovacoes}</strong> aprovação(ões) e{" "}
                      <strong className="text-danger">{selecionada.rejeicoes}</strong> rejeição(ões).
                    </p>
                  )}
                </div>
                <div className="card-footer bg-white">
                  <Mensagens erro={erro} />
                  {rejeitando ? (
                    <form
                      onSubmit={(evento) => {
                        evento.preventDefault();
                        votar("REJEITAR");
                      }}
                    >
                      <label htmlFor="motivo" className="form-label">
                        Motivo da rejeição <span className="text-secondary">(o autor lê, sem saber quem votou)</span>
                      </label>
                      <textarea
                        id="motivo"
                        className="form-control mb-2"
                        rows={3}
                        maxLength={500}
                        value={motivo}
                        onChange={(e) => setMotivo(e.target.value)}
                        placeholder="Ex.: a alternativa marcada como correta está errada; o certo é 56."
                        autoFocus
                        required
                      />
                      <div className="d-flex gap-2 justify-content-end">
                        <button type="button" className="btn btn-outline-secondary" onClick={() => setRejeitando(false)}>
                          Cancelar
                        </button>
                        <button type="submit" className="btn btn-danger" disabled={enviando || !motivo.trim()}>
                          {enviando ? "Enviando..." : "Rejeitar questão"}
                        </button>
                      </div>
                    </form>
                  ) : (
                    <div className="d-flex gap-2 justify-content-end">
                      <button type="button" className="btn btn-outline-danger" disabled={enviando} onClick={() => setRejeitando(true)}>
                        Rejeitar
                      </button>
                      <button type="button" className="btn btn-success" disabled={enviando} onClick={() => votar("APROVAR")}>
                        {enviando ? "Enviando..." : "Aprovar"}
                      </button>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}
