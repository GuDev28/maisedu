import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { gerarPraticaAdaptativa, painelDePratica } from "../services/alunoService";
import { BarraPercentual } from "../components/BarraPercentual";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { EstadoVazio } from "../components/EstadoVazio";
import { Mensagens } from "../components/Mensagens";
import { formatarPercentual, rotuloNivel } from "../components/rotulos";

/**
 * RF18 (aluno): tela "Praticar". No topo, a prática recomendada: o motor adaptativo escolhe os
 * tópicos mais fracos e o nível certo em cada um (sem histórico, uma prática diagnóstica). Abaixo, um
 * card por tópico do ano escolar do aluno, para ele praticar o tópico que quiser: a prática do tópico
 * tem poucas questões, no nível que ele ainda não domina. Em todas, as respostas são enviadas juntas,
 * no fim, e a correção aparece logo depois do envio.
 */
export default function Praticar() {
  const navigate = useNavigate();
  const [painel, setPainel] = useState(null);
  const [gerando, setGerando] = useState(null);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    painelDePratica()
      .then(setPainel)
      .catch((err) => setErro(err.message));
  }, []);

  /** Abre a prática em andamento ou gera uma nova ({@code topicoId} nulo: a recomendada). */
  async function praticar(topicoId, emAndamentoId) {
    setErro(null);
    if (emAndamentoId) {
      navigate(`/minhas-atividades/${emAndamentoId}`);
      return;
    }
    setGerando(topicoId ?? "recomendada");
    try {
      const pratica = await gerarPraticaAdaptativa(topicoId);
      navigate(`/minhas-atividades/${pratica.atividadeAlunoId}`);
    } catch (err) {
      setErro(err.message);
      setGerando(null);
    }
  }

  return (
    <>
      <Cabecalho titulo="Praticar">
        Treine no seu ritmo. A prática recomendada escolhe os seus pontos fracos; nos cards, você escolhe o tópico.
      </Cabecalho>
      <Mensagens erro={erro} />
      {!painel && !erro && <Carregando />}

      {painel && (
        <>
          <Recomendada
            recomendada={painel.recomendada}
            gerando={gerando === "recomendada"}
            aoPraticar={() => praticar(null, painel.recomendada.praticaEmAndamentoId)}
          />

          <h2 className="h5 mt-5 mb-3">Escolha um tópico</h2>
          {painel.topicos.length === 0 ? (
            <EstadoVazio titulo="Ainda não há questões aprovadas para o seu ano escolar">
              Quando os professores aprovarem questões no banco compartilhado, os tópicos aparecem aqui.
            </EstadoVazio>
          ) : (
            <div className="row g-3">
              {painel.topicos.map((topico) => (
                <div key={topico.topicoId} className="col-12 col-md-6 col-xl-4">
                  <CardTopico
                    topico={topico}
                    gerando={gerando === topico.topicoId}
                    aoPraticar={() => praticar(topico.topicoId, topico.praticaEmAndamentoId)}
                  />
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </>
  );
}

function Recomendada({ recomendada, gerando, aoPraticar }) {
  const emAndamento = Boolean(recomendada.praticaEmAndamentoId);
  return (
    <div className="card me-card-recomendado">
      <div className="card-body p-4 d-flex flex-wrap align-items-center gap-4">
        <div className="flex-grow-1" style={{ minWidth: "16rem" }}>
          <div className="small text-uppercase fw-semibold text-secondary mb-1">Recomendado para você</div>
          <h2 className="h4 mb-2">
            {emAndamento
              ? "Continue a sua prática"
              : recomendada.diagnostica
                ? "Prática diagnóstica"
                : "Prática dos seus pontos fracos"}
          </h2>
          <p className="mb-0 text-secondary">
            {emAndamento
              ? "Você tem uma prática recomendada em andamento. Termine e envie para ver a correção."
              : recomendada.diagnostica
                ? "Você ainda não respondeu nenhuma questão. Esta prática tem questões fáceis de vários tópicos; com as suas respostas, as próximas já vêm sob medida."
                : "Questões dos tópicos em que você mais erra, no nível que você ainda não domina:"}
          </p>
          {!emAndamento && recomendada.topicos.length > 0 && (
            <ul className="list-unstyled d-flex flex-wrap gap-2 mt-2 mb-0">
              {recomendada.topicos.map((topico) => (
                <li key={topico.topicoId} className="badge text-bg-light fw-normal">
                  {topico.topico} · {rotuloNivel(topico.nivelAlvo)} · {formatarPercentual(topico.percentual)}
                </li>
              ))}
            </ul>
          )}
        </div>
        <button type="button" className="btn btn-light btn-lg fw-semibold" onClick={aoPraticar} disabled={gerando}>
          {gerando ? "Montando..." : emAndamento ? "Continuar" : "Começar"}
        </button>
      </div>
    </div>
  );
}

function CardTopico({ topico, gerando, aoPraticar }) {
  const emAndamento = Boolean(topico.praticaEmAndamentoId);
  return (
    <div className="card h-100 me-card-topico">
      <div className="card-body d-flex flex-column">
        <div className="d-flex justify-content-between align-items-start gap-2">
          <h3 className="h6 mb-1">{topico.topico}</h3>
          {topico.recomendado && <span className="badge text-bg-warning">precisa de atenção</span>}
        </div>
        <div className="small text-secondary mb-3">
          {topico.totalRespostas === 0
            ? "Você ainda não respondeu questões deste tópico"
            : `${topico.totalRespostas} resposta(s) até agora`}
        </div>
        {topico.percentual !== null && topico.percentual !== undefined && (
          <div className="mb-3">
            <BarraPercentual valor={topico.percentual} />
          </div>
        )}
        <div className="d-flex flex-wrap gap-1 mb-3" aria-label="Situação por nível">
          {topico.niveis.map((nivel) => {
            const atual = nivel.nivelDificuldade === topico.nivelAtual;
            const classe = nivel.dominado ? "me-nivel me-nivel-dominado" : atual ? "me-nivel me-nivel-atual" : "me-nivel";
            return (
              <span key={nivel.nivelDificuldade} className={classe}>
                {nivel.dominado ? "✓ " : ""}
                {rotuloNivel(nivel.nivelDificuldade)}
                {atual && !nivel.dominado ? " · seu nível" : ""}
              </span>
            );
          })}
        </div>
        <div className="mt-auto d-flex justify-content-between align-items-center">
          <span className="small text-secondary">{topico.questoesDisponiveis} questões no banco</span>
          <button type="button" className={`btn btn-sm ${emAndamento ? "btn-primary" : "btn-outline-primary"}`}
                  onClick={aoPraticar} disabled={gerando}>
            {gerando ? "Montando..." : emAndamento ? "Continuar" : "Praticar"}
          </button>
        </div>
      </div>
    </div>
  );
}
