import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { abrirFolha, enviarFolha, marcarAlternativa } from "../services/alunoService";
import { BarraPercentual } from "../components/BarraPercentual";
import { Carregando } from "../components/Carregando";
import { Mensagens } from "../components/Mensagens";
import { formatarDataHora, rotuloNivel } from "../components/rotulos";
import { useInterface } from "../context/InterfaceContext";

/** Primeira questão ainda sem alternativa escolhida (ou a primeira, se todas têm). */
function primeiraSemEscolha(questoes) {
  const indice = questoes.findIndex((questao) => !questao.alternativaEscolhidaId);
  return indice === -1 ? 0 : indice;
}

/** Acertos por tópico, para o resultado depois da correção. */
function resumoPorTopico(questoes) {
  const porTopico = new Map();
  for (const questao of questoes) {
    const atual = porTopico.get(questao.topico) ?? { topico: questao.topico, total: 0, acertos: 0 };
    atual.total += 1;
    if (questao.correcao?.acertou) {
      atual.acertos += 1;
    }
    porTopico.set(questao.topico, atual);
  }
  return [...porTopico.values()];
}

/**
 * Folha da atividade (ou da prática), com entrega única: uma questão por vez, na ordem fixada ao
 * iniciar (RN03). Clicar numa alternativa a escolhe; a escolha fica guardada no servidor (volta se o
 * aluno sair e entrar de novo) e pode ser trocada até o envio e, na atividade, até o prazo. No fim,
 * "Enviar respostas" manda tudo de uma vez, só com todas as questões respondidas (RN04).
 *
 * Correção: na prática adaptativa, logo depois do envio; na atividade da turma, só depois do prazo,
 * para quem já enviou não repassar o gabarito a quem ainda não enviou.
 */
export default function FolhaAtividade() {
  const { atividadeAlunoId } = useParams();
  const { confirmar, avisar } = useInterface();
  const [folha, setFolha] = useState(null);
  const [atual, setAtual] = useState(0);
  const [enviando, setEnviando] = useState(false);
  const [verResultado, setVerResultado] = useState(false);
  const [erro, setErro] = useState(null);
  const [avisoMarcacao, setAvisoMarcacao] = useState(null);
  // As marcações vão ao servidor uma de cada vez, na ordem dos cliques: assim a última sempre vale.
  const filaMarcacoes = useRef(Promise.resolve());

  useEffect(() => {
    abrirFolha(atividadeAlunoId)
      .then((aberta) => {
        setFolha(aberta);
        setAtual(primeiraSemEscolha(aberta.questoes));
        setVerResultado(!aberta.podeResponder);
      })
      .catch((err) => setErro(err.message));
  }, [atividadeAlunoId]);

  /** Escolhe (ou troca) a alternativa: aparece na hora e é guardada no servidor, sem virar resposta. */
  async function escolher(questao, alternativa) {
    setErro(null);
    setFolha((anterior) => {
      const questoes = anterior.questoes.map((q) =>
        q.atividadeAlunoQuestaoId === questao.atividadeAlunoQuestaoId ? { ...q, alternativaEscolhidaId: alternativa.id } : q
      );
      return { ...anterior, questoes, escolhidas: questoes.filter((q) => q.alternativaEscolhidaId).length };
    });
    const envio = filaMarcacoes.current.then(() =>
      marcarAlternativa(folha.atividadeAlunoId, questao.atividadeAlunoQuestaoId, alternativa.id)
    );
    filaMarcacoes.current = envio.catch(() => undefined);
    try {
      await envio;
      setAvisoMarcacao(null);
    } catch (err) {
      setAvisoMarcacao(
        err.status === 400
          ? err.message
          : "Não foi possível guardar a sua escolha agora. Ela continua valendo nesta tela até você enviar."
      );
    }
  }

  async function enviar() {
    const ok = await confirmar({
      titulo: "Enviar as respostas?",
      mensagem:
        folha.tipo === "ADAPTATIVA"
          ? "Depois de enviar, as respostas não podem mais ser trocadas. A correção aparece em seguida."
          : `Depois de enviar, as respostas não podem mais ser trocadas. A correção aparece depois do prazo (${formatarDataHora(folha.prazo)}).`,
      textoConfirmar: "Enviar respostas",
    });
    if (!ok) {
      return;
    }
    setErro(null);
    setEnviando(true);
    try {
      // Espera as marcações pendentes, para nenhuma chegar ao servidor depois do envio.
      await filaMarcacoes.current;
      const respostas = folha.questoes.map((q) => ({
        atividadeAlunoQuestaoId: q.atividadeAlunoQuestaoId,
        alternativaId: q.alternativaEscolhidaId,
      }));
      const enviada = await enviarFolha(folha.atividadeAlunoId, respostas);
      setFolha(enviada);
      setVerResultado(true);
      avisar("Respostas enviadas!");
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
  const faltam = total - folha.escolhidas;
  const progresso = total > 0 ? Math.round((folha.escolhidas * 100) / total) : 0;
  const praticaAdaptativa = folha.tipo === "ADAPTATIVA";

  return (
    <div className="me-largura-md">
      <Link to={praticaAdaptativa ? "/praticar" : "/minhas-atividades"} className="small">
        ← {praticaAdaptativa ? "Praticar" : "Minhas atividades"}
      </Link>
      <div className="d-flex flex-wrap justify-content-between align-items-end gap-2 mt-2 mb-1">
        <h1 className="me-titulo mb-0">{folha.titulo}</h1>
        <span className="text-secondary small">
          {folha.podeResponder ? `${folha.escolhidas} de ${total} respondidas` : situacaoDaFolha(folha)}
        </span>
      </div>
      {folha.prazo && (
        <p className={`small mb-3 ${folha.prazoEncerrado ? "text-secondary" : "text-warning fw-semibold"}`}>
          {folha.prazoEncerrado ? "Prazo encerrado em " : "Prazo de entrega: "}
          {formatarDataHora(folha.prazo)}
        </p>
      )}
      {folha.podeResponder && (
        <div
          className="progress mb-4"
          role="progressbar"
          aria-label="Questões respondidas"
          aria-valuenow={progresso}
          aria-valuemin="0"
          aria-valuemax="100"
        >
          <div className="progress-bar" style={{ width: `${progresso}%` }} />
        </div>
      )}

      <Mensagens erro={erro} aviso={avisoMarcacao} />

      {!folha.podeResponder && verResultado ? (
        <Resultado folha={folha} praticaAdaptativa={praticaAdaptativa} aoRever={() => { setVerResultado(false); setAtual(0); }} />
      ) : (
        <>
          <QuestaoAtual
            questao={folha.questoes[atual]}
            numero={atual + 1}
            total={total}
            podeResponder={folha.podeResponder}
            correcaoLiberada={folha.correcaoLiberada}
            enviando={enviando}
            aoEscolher={escolher}
          />
          <Navegacao
            questoes={folha.questoes}
            atual={atual}
            correcaoLiberada={folha.correcaoLiberada}
            aoIrPara={setAtual}
          />
          {folha.podeResponder ? (
            <div className="card mt-3 border-primary-subtle">
              <div className="card-body d-flex flex-wrap align-items-center justify-content-between gap-3">
                <span className="small text-secondary">
                  {faltam > 0
                    ? `Falta${faltam > 1 ? "m" : ""} ${faltam} questã${faltam > 1 ? "ões" : "o"} para poder enviar. Você pode trocar as alternativas até enviar.`
                    : "Todas respondidas. Confira se quiser e envie quando estiver pronto(a)."}
                </span>
                <button type="button" className="btn btn-primary" disabled={faltam > 0 || enviando} onClick={enviar}>
                  {enviando ? "Enviando..." : "Enviar respostas"}
                </button>
              </div>
            </div>
          ) : (
            <div className="d-flex justify-content-end mt-3">
              <button type="button" className="btn btn-outline-primary" onClick={() => setVerResultado(true)}>
                Voltar ao resultado
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function situacaoDaFolha(folha) {
  if (!folha.enviadaEm) {
    return "Não entregue";
  }
  return folha.correcaoLiberada
    ? `${folha.acertos} de ${folha.totalQuestoes} acertos`
    : `Enviada em ${formatarDataHora(folha.enviadaEm)}`;
}

function QuestaoAtual({ questao, numero, total, podeResponder, correcaoLiberada, enviando, aoEscolher }) {
  const correcao = questao.correcao;
  const escolhida = questao.alternativas.find((a) => a.id === questao.alternativaEscolhidaId);
  return (
    <div className="card">
      <div className="card-body p-4">
        <div className="small text-secondary mb-2">
          Questão {numero} de {total} · {questao.topico} · {rotuloNivel(questao.nivelDificuldade)}
        </div>
        <p className="fs-5 mb-4">{questao.enunciado}</p>
        <div className="d-grid gap-2" role="radiogroup" aria-label={`Alternativas da questão ${numero}`}>
          {questao.alternativas.map((alternativa) => {
            const minha = alternativa.id === questao.alternativaEscolhidaId;
            const certa = correcao && alternativa.id === correcao.alternativaCorretaId;
            let classe = "alternativa";
            let selo = null;
            if (correcao) {
              if (certa) {
                classe += " alternativa-certa";
                selo = <span className="alternativa-selo text-success">✓ Correta</span>;
              } else if (minha) {
                classe += " alternativa-errada";
                selo = <span className="alternativa-selo text-danger">✗ Sua resposta</span>;
              }
            } else if (minha) {
              classe += " alternativa-marcada";
              selo = <span className="alternativa-selo text-primary">{podeResponder ? "Escolhida" : "Enviada"}</span>;
            }
            return (
              <button
                key={alternativa.id}
                type="button"
                role="radio"
                aria-checked={minha}
                className={classe}
                disabled={!podeResponder || enviando}
                onClick={() => aoEscolher(questao, alternativa)}
              >
                <span className="alternativa-letra">{alternativa.letra})</span>
                <span>{alternativa.texto}</span>
                {selo}
              </button>
            );
          })}
        </div>
        {podeResponder && (
          <p className="small text-secondary mt-3 mb-0">
            {escolhida
              ? `Você escolheu ${escolhida.letra}. Pode trocar até enviar as respostas.`
              : "Escolha uma alternativa. As respostas são enviadas todas juntas, no fim."}
          </p>
        )}
        {!podeResponder && !correcaoLiberada && (
          <p className="small text-secondary mt-3 mb-0">A correção aparece depois do prazo de entrega.</p>
        )}
        {correcao && !correcao.respondida && (
          <div className="alert alert-secondary mt-3 mb-0 py-2" role="status">
            Esta questão não foi respondida. A resposta certa é {correcao.letraCorreta}.
          </div>
        )}
      </div>
    </div>
  );
}

function Navegacao({ questoes, atual, correcaoLiberada, aoIrPara }) {
  return (
    <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3">
      <button type="button" className="btn btn-outline-secondary" disabled={atual === 0} onClick={() => aoIrPara(atual - 1)}>
        ← Anterior
      </button>
      <nav aria-label="Questões" className="d-flex flex-wrap gap-1">
        {questoes.map((q, indice) => {
          let cor = "btn-outline-secondary";
          let estado = "sem resposta";
          if (correcaoLiberada && q.correcao) {
            cor = q.correcao.acertou ? "btn-success" : "btn-danger";
            estado = q.correcao.acertou ? "acertou" : q.correcao.respondida ? "errou" : "não respondida";
          } else if (q.alternativaEscolhidaId) {
            cor = "btn-outline-primary";
            estado = "respondida";
          }
          return (
            <button
              key={q.atividadeAlunoQuestaoId}
              type="button"
              className={`btn btn-sm ${cor} ${indice === atual ? "fw-bold border-2" : ""}`}
              aria-current={indice === atual ? "step" : undefined}
              aria-label={`Questão ${indice + 1}, ${estado}`}
              onClick={() => aoIrPara(indice)}
            >
              {indice + 1}
            </button>
          );
        })}
      </nav>
      <button
        type="button"
        className="btn btn-outline-secondary"
        disabled={atual === questoes.length - 1}
        onClick={() => aoIrPara(atual + 1)}
      >
        Próxima →
      </button>
    </div>
  );
}

function Resultado({ folha, praticaAdaptativa, aoRever }) {
  if (!folha.correcaoLiberada) {
    return (
      <div className="card">
        <div className="card-body p-4">
          <h2 className="h4">Respostas enviadas!</h2>
          <p className="text-secondary mb-4">
            Você enviou as {folha.totalQuestoes} respostas em {formatarDataHora(folha.enviadaEm)}. A correção e o seu
            desempenho aparecem depois do prazo, em {formatarDataHora(folha.prazo)}.
          </p>
          <div className="d-flex flex-wrap gap-2">
            <Link to="/minhas-atividades" className="btn btn-primary">
              Voltar para as atividades
            </Link>
            <button type="button" className="btn btn-outline-secondary" onClick={aoRever}>
              Ver o que respondi
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (!folha.enviadaEm) {
    return (
      <div className="card">
        <div className="card-body p-4">
          <h2 className="h4">Atividade não entregue</h2>
          <p className="text-secondary mb-4">
            O prazo terminou em {formatarDataHora(folha.prazo)} sem o envio das respostas, então elas não contam no
            desempenho. Você ainda pode ver as questões com o gabarito.
          </p>
          <button type="button" className="btn btn-outline-primary" onClick={aoRever}>
            Ver as questões e o gabarito
          </button>
        </div>
      </div>
    );
  }

  const topicos = resumoPorTopico(folha.questoes);
  const percentual = folha.totalQuestoes > 0 ? (folha.acertos * 100) / folha.totalQuestoes : 0;
  return (
    <div className="card">
      <div className="card-body p-4">
        <h2 className="h4">{praticaAdaptativa ? "Prática concluída!" : "Atividade corrigida"}</h2>
        <p className="text-secondary">
          Você acertou <strong>{folha.acertos}</strong> de {folha.totalQuestoes} questões. Seu desempenho já foi
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
            Praticar de novo
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
