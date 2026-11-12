import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { abrirFolha, marcarAlternativa, responderQuestao } from "../services/alunoService";
import { BarraPercentual } from "../components/BarraPercentual";
import { Carregando } from "../components/Carregando";
import { Mensagens } from "../components/Mensagens";
import { rotuloNivel } from "../components/rotulos";

/** Primeira questão ainda sem resposta (ou a última, se todas foram respondidas). */
function primeiraPendente(questoes) {
  const indice = questoes.findIndex((questao) => !questao.resposta);
  return indice === -1 ? questoes.length - 1 : indice;
}

/** Acertos por tópico, para a tela final. */
function resumoPorTopico(questoes) {
  const porTopico = new Map();
  for (const questao of questoes) {
    const atual = porTopico.get(questao.topico) ?? { topico: questao.topico, total: 0, acertos: 0 };
    atual.total += 1;
    if (questao.resposta?.acertou) {
      atual.acertos += 1;
    }
    porTopico.set(questao.topico, atual);
  }
  return [...porTopico.values()];
}

/**
 * Folha da atividade (ou da prática): uma questão por vez, na ordem fixada ao iniciar (RN03).
 * Clicar numa alternativa só a marca: a marcação fica guardada no servidor (volta se o aluno sair e
 * entrar de novo) e pode ser trocada. A resposta, única (RN04), só é gravada em "Confirmar resposta",
 * e a correção aparece na hora, com o retorno do próprio POST. No fim, os acertos por tópico.
 */
export default function FolhaAtividade() {
  const { atividadeAlunoId } = useParams();
  const [folha, setFolha] = useState(null);
  const [atual, setAtual] = useState(0);
  const [enviando, setEnviando] = useState(false);
  const [verResumo, setVerResumo] = useState(false);
  const [erro, setErro] = useState(null);
  const [avisoMarcacao, setAvisoMarcacao] = useState(null);
  // As marcações vão ao servidor uma de cada vez, na ordem dos cliques: assim a última sempre vale.
  const filaMarcacoes = useRef(Promise.resolve());

  useEffect(() => {
    abrirFolha(atividadeAlunoId)
      .then((aberta) => {
        setFolha(aberta);
        setAtual(primeiraPendente(aberta.questoes));
        setVerResumo(Boolean(aberta.concluidaEm));
      })
      .catch((err) => setErro(err.message));
  }, [atividadeAlunoId]);

  function atualizarQuestao(atividadeAlunoQuestaoId, mudanca) {
    setFolha((anterior) => ({
      ...anterior,
      questoes: anterior.questoes.map((q) => (q.atividadeAlunoQuestaoId === atividadeAlunoQuestaoId ? { ...q, ...mudanca } : q)),
    }));
  }

  /** Só marca: a escolha aparece na hora e é guardada no servidor, sem virar resposta. */
  async function marcar(questao, alternativa) {
    setErro(null);
    atualizarQuestao(questao.atividadeAlunoQuestaoId, { alternativaMarcadaId: alternativa.id });
    const envio = filaMarcacoes.current.then(() =>
      marcarAlternativa(folha.atividadeAlunoId, questao.atividadeAlunoQuestaoId, alternativa.id)
    );
    filaMarcacoes.current = envio.catch(() => undefined);
    try {
      await envio;
      setAvisoMarcacao(null);
    } catch {
      setAvisoMarcacao("Não foi possível guardar a sua marcação agora. Ela continua valendo nesta tela.");
    }
  }

  async function responder(questao) {
    const alternativa = questao.alternativas.find((a) => a.id === questao.alternativaMarcadaId);
    if (!alternativa) {
      return;
    }
    setErro(null);
    setEnviando(true);
    try {
      // Espera as marcações pendentes chegarem antes, para nenhuma delas chegar depois da resposta.
      await filaMarcacoes.current;
      const resultado = await responderQuestao(folha.atividadeAlunoId, questao.atividadeAlunoQuestaoId, alternativa.id);
      const resposta = {
        alternativaEscolhidaId: alternativa.id,
        letraEscolhida: alternativa.letra,
        acertou: resultado.acertou,
        alternativaCorretaId: resultado.alternativaCorretaId,
        letraCorreta: resultado.letraCorreta,
      };
      setFolha((anterior) => ({
        ...anterior,
        respondidas: resultado.respondidas,
        acertos: resultado.acertos,
        concluidaEm: resultado.atividadeConcluida ? anterior.concluidaEm ?? new Date().toISOString() : null,
        questoes: anterior.questoes.map((q) =>
          q.atividadeAlunoQuestaoId === questao.atividadeAlunoQuestaoId ? { ...q, resposta, alternativaMarcadaId: null } : q
        ),
      }));
    } catch (err) {
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  if (erro && !folha) {
    return (
      <>
        <Mensagens erro={erro} />
        <Link to="/minhas-atividades">← Voltar para as atividades</Link>
      </>
    );
  }
  if (!folha) {
    return <Carregando />;
  }

  const total = folha.questoes.length;
  const progresso = total > 0 ? Math.round((folha.respondidas * 100) / total) : 0;
  const concluida = Boolean(folha.concluidaEm);
  const praticaAdaptativa = folha.tipo === "ADAPTATIVA";
  const marcadasSemConfirmar = folha.questoes.filter((q) => !q.resposta && q.alternativaMarcadaId).length;

  return (
    <div className="me-largura-md">
      <Link to="/minhas-atividades" className="small">
        ← Minhas atividades
      </Link>
      <div className="d-flex flex-wrap justify-content-between align-items-end gap-2 mt-2 mb-3">
        <h1 className="me-titulo mb-0">{folha.titulo}</h1>
        <span className="text-secondary small">
          {folha.respondidas} de {total} respondidas · {folha.acertos} acerto(s)
        </span>
      </div>
      <div
        className="progress mb-4"
        role="progressbar"
        aria-label="Progresso da atividade"
        aria-valuenow={progresso}
        aria-valuemin="0"
        aria-valuemax="100"
      >
        <div className="progress-bar" style={{ width: `${progresso}%` }} />
      </div>

      <Mensagens erro={erro} aviso={avisoMarcacao} />

      {concluida && verResumo ? (
        <ResumoFinal folha={folha} praticaAdaptativa={praticaAdaptativa} aoRever={() => { setVerResumo(false); setAtual(0); }} />
      ) : (
        <QuestaoAtual
          questao={folha.questoes[atual]}
          numero={atual + 1}
          total={total}
          enviando={enviando}
          aoMarcar={marcar}
          aoResponder={responder}
          aoAnterior={atual > 0 ? () => setAtual(atual - 1) : null}
          aoProxima={atual < total - 1 ? () => setAtual(atual + 1) : null}
          aoFinalizar={concluida ? () => setVerResumo(true) : null}
          questoes={folha.questoes}
          aoIrPara={setAtual}
          atual={atual}
          marcadasSemConfirmar={marcadasSemConfirmar}
        />
      )}
    </div>
  );
}

function QuestaoAtual({
  questao,
  numero,
  total,
  enviando,
  aoMarcar,
  aoResponder,
  aoAnterior,
  aoProxima,
  aoFinalizar,
  questoes,
  aoIrPara,
  atual,
  marcadasSemConfirmar,
}) {
  const resposta = questao.resposta;
  const marcada = questao.alternativas.find((a) => a.id === questao.alternativaMarcadaId);
  return (
    <>
      <div className="card">
        <div className="card-body p-4">
          <div className="small text-secondary mb-2">
            Questão {numero} de {total} · {questao.topico} · {rotuloNivel(questao.nivelDificuldade)}
          </div>
          <p className="fs-5 mb-4">{questao.enunciado}</p>
          <div className="d-grid gap-2" role="radiogroup" aria-label={`Alternativas da questão ${numero}`}>
            {questao.alternativas.map((alternativa) => {
              let classe = "alternativa";
              if (resposta && alternativa.id === resposta.alternativaCorretaId) {
                classe += " alternativa-certa";
              } else if (resposta && alternativa.id === resposta.alternativaEscolhidaId) {
                classe += " alternativa-errada";
              } else if (!resposta && alternativa.id === questao.alternativaMarcadaId) {
                classe += " alternativa-marcada";
              }
              const selecionada = resposta
                ? alternativa.id === resposta.alternativaEscolhidaId
                : alternativa.id === questao.alternativaMarcadaId;
              return (
                <button
                  key={alternativa.id}
                  type="button"
                  role="radio"
                  aria-checked={selecionada}
                  className={classe}
                  disabled={Boolean(resposta) || enviando}
                  onClick={() => aoMarcar(questao, alternativa)}
                >
                  <span className="alternativa-letra">{alternativa.letra})</span>
                  <span>{alternativa.texto}</span>
                </button>
              );
            })}
          </div>
          {!resposta && (
            <div className="d-flex flex-wrap align-items-center gap-3 mt-3">
              <button
                type="button"
                className="btn btn-primary"
                disabled={!marcada || enviando}
                onClick={() => aoResponder(questao)}
              >
                {enviando ? "Enviando..." : "Confirmar resposta"}
              </button>
              <span className="small text-secondary">
                {marcada
                  ? `Você marcou ${marcada.letra}. Pode trocar até confirmar; depois de confirmada, a resposta não muda.`
                  : "Escolha uma alternativa e depois confirme."}
              </span>
            </div>
          )}
          {resposta && (
            <div className={`alert mt-3 mb-0 ${resposta.acertou ? "alert-success" : "alert-danger"}`} role="status">
              {resposta.acertou
                ? "Você acertou!"
                : `Você marcou ${resposta.letraEscolhida}; a resposta certa é ${resposta.letraCorreta}.`}
            </div>
          )}
        </div>
      </div>

      {marcadasSemConfirmar > 0 && (
        <div className="alert alert-info py-2 mt-3 mb-0 small" role="status">
          {marcadasSemConfirmar === 1
            ? "1 questão está marcada mas ainda não foi confirmada."
            : `${marcadasSemConfirmar} questões estão marcadas mas ainda não foram confirmadas.`}{" "}
          Só as respostas confirmadas contam.
        </div>
      )}

      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3">
        <button type="button" className="btn btn-outline-secondary" disabled={!aoAnterior} onClick={aoAnterior ?? undefined}>
          ← Anterior
        </button>
        <nav aria-label="Questões da atividade" className="d-flex flex-wrap gap-1">
          {questoes.map((q, indice) => {
            let cor = "btn-outline-secondary";
            if (q.resposta) {
              cor = q.resposta.acertou ? "btn-success" : "btn-danger";
            } else if (q.alternativaMarcadaId) {
              cor = "btn-outline-primary";
            }
            return (
              <button
                key={q.atividadeAlunoQuestaoId}
                type="button"
                className={`btn btn-sm ${cor} ${indice === atual ? "fw-bold border-2" : ""}`}
                aria-current={indice === atual ? "step" : undefined}
                aria-label={`Questão ${indice + 1}${
                  q.resposta ? (q.resposta.acertou ? ", acertou" : ", errou") : q.alternativaMarcadaId ? ", marcada sem confirmar" : ", sem resposta"
                }`}
                onClick={() => aoIrPara(indice)}
              >
                {indice + 1}
              </button>
            );
          })}
        </nav>
        {aoProxima ? (
          <button type="button" className="btn btn-primary" onClick={aoProxima}>
            Próxima →
          </button>
        ) : aoFinalizar ? (
          <button type="button" className="btn btn-primary" onClick={aoFinalizar}>
            Ver resultado
          </button>
        ) : (
          <span />
        )}
      </div>
    </>
  );
}

function ResumoFinal({ folha, praticaAdaptativa, aoRever }) {
  const topicos = resumoPorTopico(folha.questoes);
  const percentual = folha.questoes.length > 0 ? (folha.acertos * 100) / folha.questoes.length : 0;
  return (
    <div className="card">
      <div className="card-body p-4">
        <h2 className="h4">{praticaAdaptativa ? "Prática concluída!" : "Atividade concluída!"}</h2>
        <p className="text-secondary">
          Você acertou <strong>{folha.acertos}</strong> de {folha.questoes.length} questões. Seu desempenho já foi
          atualizado.
        </p>
        <BarraPercentual valor={percentual} destaque />
        <h3 className="h6 text-secondary text-uppercase mt-4">Acertos por tópico</h3>
        <ul className="list-group mb-4">
          {topicos.map((topico) => (
            <li key={topico.topico} className="list-group-item d-flex justify-content-between">
              <span>{topico.topico}</span>
              <span className="fw-semibold">
                {topico.acertos}/{topico.total}
              </span>
            </li>
          ))}
        </ul>
        <div className="d-flex flex-wrap gap-2">
          <Link to="/praticar" className="btn btn-primary">
            Praticar os pontos fracos
          </Link>
          <Link to="/meu-desempenho" className="btn btn-outline-primary">
            Ver meu desempenho
          </Link>
          <button type="button" className="btn btn-outline-secondary" onClick={aoRever}>
            Rever as questões
          </button>
        </div>
      </div>
    </div>
  );
}
