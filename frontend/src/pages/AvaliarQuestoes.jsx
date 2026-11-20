import { useEffect, useState } from "react";
import { useInterface } from "../context/useInterface";
import { avaliarQuestao } from "../services/avaliacaoQuestaoService";
import { listarQuestoesPendentes } from "../services/questaoService";
import { EVENTO_FILA_ATUALIZADA } from "../components/Layout";
import { Autor } from "../components/Avatar";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { AlternativasComGabarito, EtiquetasQuestao } from "../components/EtiquetasQuestao";
import { EstadoVazio } from "../components/EstadoVazio";
import { Icone } from "../components/Icone";
import { Mensagens } from "../components/Mensagens";
import { rotuloNivel } from "../components/rotulos";

/** O que conferir antes de votar: o mesmo roteiro para todos os avaliadores. */
const ROTEIRO = [
  "O enunciado está claro e tem tudo o que o aluno precisa?",
  "A alternativa marcada como correta está mesmo certa?",
  "As outras alternativas estão erradas, sem duas certas?",
  "O nível e o ano escolar combinam com a questão?",
];

/** Começos de motivo mais comuns; o avaliador completa com o detalhe. */
const MOTIVOS_RAPIDOS = ["Gabarito errado", "Enunciado confuso", "Mais de uma alternativa certa", "Nível ou ano inadequado", "Erro de digitação"];

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
 * (de qualquer escola) e aprova ou rejeita; ao rejeitar, escreve o motivo para o autor. À esquerda
 * fica a fila; à direita, a questão aberta com o gabarito e o roteiro do que conferir.
 */
export default function AvaliarQuestoes() {
  const { avisar } = useInterface();
  const [pendentes, setPendentes] = useState(null);
  const [selecionada, setSelecionada] = useState(null);
  const [rejeitando, setRejeitando] = useState(false);
  const [motivo, setMotivo] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState(null);

  /** Mostra a fila e mantém aberta a questão atual, se ela ainda estiver lá (senão, a primeira). */
  function aplicarFila(lista) {
    setPendentes(lista);
    setSelecionada((atual) => (atual && lista.some((q) => q.id === atual.id) ? atual : lista[0] ?? null));
  }

  async function carregar() {
    try {
      aplicarFila(await listarQuestoesPendentes());
    } catch (err) {
      setErro(err.message);
    }
  }

  useEffect(() => {
    listarQuestoesPendentes()
      .then((lista) => {
        setPendentes(lista);
        setSelecionada(lista[0] ?? null);
      })
      .catch((err) => setErro(err.message));
  }, []);

  function selecionar(questao) {
    setSelecionada(questao);
    setRejeitando(false);
    setMotivo("");
    setErro(null);
  }

  function usarMotivo(texto) {
    setMotivo((atual) => (atual.trim() ? `${atual.trim()} ${texto}: ` : `${texto}: `));
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

  const posicao = selecionada && pendentes ? pendentes.findIndex((q) => q.id === selecionada.id) + 1 : 0;

  return (
    <>
      <Cabecalho titulo="Avaliar questões">
        Questões de outros professores esperando o seu voto. Uma questão entra no banco com 3 aprovações e sai com 3
        rejeições.
      </Cabecalho>
      {!pendentes && !erro && <Carregando />}
      {!pendentes && <Mensagens erro={erro} />}
      {pendentes?.length === 0 && (
        <EstadoVazio titulo="Nenhuma questão esperando o seu voto">
          Quando outro professor enviar uma questão para validação, ela aparece aqui.
        </EstadoVazio>
      )}
      {pendentes?.length > 0 && (
        <div className="row g-4">
          <div className="col-lg-5">
            <div className="me-fila-topo">
              <h2 className="h6 mb-0">Fila de validação</h2>
              <span className="badge rounded-pill text-bg-warning">
                {pendentes.length} {pendentes.length === 1 ? "questão" : "questões"}
              </span>
            </div>
            <div className="me-fila">
              {pendentes.map((questao, indice) => {
                const atual = selecionada?.id === questao.id;
                return (
                  <button
                    key={questao.id}
                    type="button"
                    className={`me-fila-item ${atual ? "atual" : ""}`}
                    aria-current={atual ? "true" : undefined}
                    onClick={() => selecionar(questao)}
                  >
                    <span className="me-fila-numero">{indice + 1}</span>
                    <span className="me-fila-corpo">
                      <span className="me-fila-enunciado">{questao.enunciado}</span>
                      <span className="me-fila-detalhe">
                        {questao.topico} · {rotuloNivel(questao.nivelDificuldade)}
                        {questao.autorInstituicao && ` · ${questao.autorInstituicao}`}
                      </span>
                    </span>
                    <Icone nome="avancar" tamanho={16} className="me-fila-seta" />
                  </button>
                );
              })}
            </div>
          </div>

          <div className="col-lg-7">
            {selecionada && (
              <article className="card me-avaliacao" aria-labelledby="enunciado-em-avaliacao">
                <div className="card-body p-4">
                  <div className="d-flex flex-wrap gap-2 justify-content-between align-items-center mb-3">
                    <EtiquetasQuestao questao={selecionada} />
                    <span className="small text-secondary">
                      Questão {posicao} de {pendentes.length}
                    </span>
                  </div>
                  <p id="enunciado-em-avaliacao" className="me-avaliacao-enunciado">
                    {selecionada.enunciado}
                  </p>
                  <AlternativasComGabarito alternativas={selecionada.alternativas} />

                  <div className="d-flex flex-wrap gap-3 justify-content-between align-items-center mt-4">
                    <Autor nome={selecionada.autorNome} escola={selecionada.autorInstituicao} prefixo="Enviada por" />
                    {selecionada.aprovacoes !== null && selecionada.aprovacoes !== undefined && (
                      <span className="small">
                        Placar: <strong className="text-success">{selecionada.aprovacoes}</strong> aprovação(ões) e{" "}
                        <strong className="text-danger">{selecionada.rejeicoes}</strong> rejeição(ões)
                      </span>
                    )}
                  </div>

                  <div className="me-roteiro mt-4">
                    <h3 className="me-roteiro-titulo">Antes de votar, confira</h3>
                    <ul>
                      {ROTEIRO.map((item) => (
                        <li key={item}>
                          <Icone nome="check" tamanho={16} />
                          {item}
                        </li>
                      ))}
                    </ul>
                  </div>
                </div>

                <div className="card-footer me-avaliacao-votos">
                  <Mensagens erro={erro} />
                  {rejeitando ? (
                    <form
                      onSubmit={(evento) => {
                        evento.preventDefault();
                        votar("REJEITAR");
                      }}
                    >
                      <label htmlFor="motivo" className="form-label fw-semibold">
                        Motivo da rejeição <span className="text-secondary fw-normal">(o autor lê, sem saber quem votou)</span>
                      </label>
                      <div className="d-flex flex-wrap gap-2 mb-2" role="group" aria-label="Começar o motivo com">
                        {MOTIVOS_RAPIDOS.map((texto) => (
                          <button key={texto} type="button" className="me-motivo-rapido" onClick={() => usarMotivo(texto)}>
                            {texto}
                          </button>
                        ))}
                      </div>
                      <textarea
                        id="motivo"
                        className="form-control mb-3"
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
                    <div className="me-votos">
                      <button
                        type="button"
                        className="btn btn-outline-danger me-voto"
                        disabled={enviando}
                        onClick={() => setRejeitando(true)}
                      >
                        <Icone nome="fechar" tamanho={20} />
                        Rejeitar
                      </button>
                      <button type="button" className="btn btn-success me-voto" disabled={enviando} onClick={() => votar("APROVAR")}>
                        <Icone nome="check" tamanho={20} />
                        {enviando ? "Enviando..." : "Aprovar"}
                      </button>
                    </div>
                  )}
                </div>
              </article>
            )}
          </div>
        </div>
      )}
    </>
  );
}
