import { useEffect, useState } from "react";
import {
  abrirFolha,
  gerarPraticaAdaptativa,
  iniciarAtividade,
  listarMinhasAtividades,
  responderQuestao,
} from "../services/alunoService";
import { formatarDataHora, rotuloNivel } from "../components/rotulos";
import { Mensagens } from "../components/Mensagens";

const SITUACOES = { NAO_INICIADA: "Não iniciada", EM_ANDAMENTO: "Em andamento", CONCLUIDA: "Concluída" };

/** RF16 e RF18 (aluno): atividades da turma, prática adaptativa e resolução das questões. */
export default function MinhasAtividades() {
  const [atividades, setAtividades] = useState([]);
  const [folha, setFolha] = useState(null);
  const [aviso, setAviso] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const [gerando, setGerando] = useState(false);
  const [erro, setErro] = useState(null);

  async function carregar() {
    try {
      setAtividades(await listarMinhasAtividades());
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    carregar();
  }, []);

  async function abrir(item) {
    setErro(null);
    setAviso(null);
    try {
      const aberta = item.atividadeAlunoId
        ? await abrirFolha(item.atividadeAlunoId)
        : await iniciarAtividade(item.atividadeId);
      setFolha(aberta);
    } catch (err) {
      setErro(err.message);
    }
  }

  async function gerarPratica() {
    setErro(null);
    setAviso(null);
    setGerando(true);
    try {
      const pratica = await gerarPraticaAdaptativa();
      const topicos = pratica.topicos.map((t) => `${t.topico} (${rotuloNivel(t.nivelDificuldade)})`).join(", ");
      setAviso(pratica.nova
        ? `Prática montada para você: ${topicos}.`
        : "Você já tinha uma prática adaptativa em andamento. Termine esta antes de gerar outra.");
      setFolha(await abrirFolha(pratica.atividadeAlunoId));
    } catch (err) {
      setErro(err.message);
    } finally {
      setGerando(false);
    }
  }

  async function voltar() {
    setFolha(null);
    setAviso(null);
    await carregar();
  }

  if (folha) {
    return <FolhaAtividade folha={folha} aviso={aviso} onAtualizar={setFolha} onVoltar={voltar} />;
  }

  const regulares = atividades.filter((a) => a.tipo === "REGULAR");
  const praticas = atividades.filter((a) => a.tipo === "ADAPTATIVA");

  return (
    <div className="pagina">
      <h2>Minhas atividades</h2>
      <Mensagens erro={erro} />

      <div className="cartao cartao-selecionado">
        <strong>Prática adaptativa</strong>
        <p style={{ margin: "0.25rem 0 0.5rem" }}>
          Dez questões escolhidas a partir das suas respostas: os tópicos em que você mais erra, no nível certo para você.
        </p>
        <button type="button" onClick={gerarPratica} disabled={gerando}>
          {gerando ? "Montando..." : "Praticar agora"}
        </button>
      </div>

      <h3>Atividades da turma</h3>
      {carregando ? (
        <p>Carregando...</p>
      ) : regulares.length === 0 ? (
        <p>Nenhuma atividade disponível. Quando seu professor criar uma, ela aparece aqui.</p>
      ) : (
        regulares.map((item) => <ItemAtividade key={`r${item.atividadeId}`} item={item} onAbrir={abrir} />)
      )}

      {praticas.length > 0 && (
        <>
          <h3>Práticas adaptativas anteriores</h3>
          {praticas.map((item) => <ItemAtividade key={`p${item.atividadeAlunoId}`} item={item} onAbrir={abrir} />)}
        </>
      )}
    </div>
  );
}

function ItemAtividade({ item, onAbrir }) {
  const rotuloBotao = item.situacao === "NAO_INICIADA" ? "Começar" : item.situacao === "CONCLUIDA" ? "Rever" : "Continuar";
  return (
    <div className="cartao">
      <strong>{item.titulo}</strong>
      {item.turmaNome && <span className="texto-suave"> · {item.turmaNome}</span>}
      <div className="linha-acoes">
        <span className="etiqueta etiqueta-neutra">{SITUACOES[item.situacao]}</span>
        <span className="texto-suave">
          {item.respondidas} de {item.totalQuestoes} respondidas
          {item.respondidas > 0 && ` · ${item.acertos} acerto(s)`}
          {item.concluidaEm && ` · concluída em ${formatarDataHora(item.concluidaEm)}`}
        </span>
        <button type="button" onClick={() => onAbrir(item)}>{rotuloBotao}</button>
      </div>
    </div>
  );
}

/**
 * Folha da atividade: as questões entregues ao aluno, na ordem fixada ao iniciar (RN03).
 * Cada questão aceita uma única resposta (RN04); depois de responder, aparece a correção.
 */
function FolhaAtividade({ folha, aviso, onAtualizar, onVoltar }) {
  const [enviando, setEnviando] = useState(null);
  const [erro, setErro] = useState(null);

  async function responder(questao, alternativa) {
    setErro(null);
    setEnviando(questao.atividadeAlunoQuestaoId);
    try {
      await responderQuestao(folha.atividadeAlunoId, questao.atividadeAlunoQuestaoId, alternativa.id);
      onAtualizar(await abrirFolha(folha.atividadeAlunoId));
    } catch (err) {
      setErro(err.message);
    } finally {
      setEnviando(null);
    }
  }

  const concluida = Boolean(folha.concluidaEm);

  return (
    <div className="pagina">
      <button type="button" onClick={onVoltar}>← Voltar</button>
      <h2>{folha.titulo}</h2>
      <Mensagens sucesso={aviso} erro={erro} />
      <p>
        <strong>{folha.respondidas}</strong> de {folha.totalQuestoes} respondidas · <strong>{folha.acertos}</strong> acerto(s)
        {concluida && <span className="etiqueta etiqueta-ok">concluída</span>}
      </p>

      <ol style={{ paddingLeft: "1.2rem" }}>
        {folha.questoes.map((questao) => {
          const resposta = questao.resposta;
          return (
            <li key={questao.atividadeAlunoQuestaoId} className="cartao" style={{ listStylePosition: "outside" }}>
              <p style={{ marginTop: 0 }}>{questao.enunciado}</p>
              <small className="texto-suave">{questao.topico} · {rotuloNivel(questao.nivelDificuldade)}</small>
              <ul className="alternativas">
                {questao.alternativas.map((alternativa) => {
                  let classe = "alternativa";
                  if (resposta && alternativa.id === resposta.alternativaCorretaId) {
                    classe += " alternativa-certa";
                  } else if (resposta && alternativa.id === resposta.alternativaEscolhidaId) {
                    classe += " alternativa-errada";
                  }
                  return (
                    <li key={alternativa.id}>
                      <button
                        type="button"
                        className={classe}
                        disabled={Boolean(resposta) || enviando !== null}
                        onClick={() => responder(questao, alternativa)}
                      >
                        <strong>{alternativa.letra})</strong> {alternativa.texto}
                      </button>
                    </li>
                  );
                })}
              </ul>
              {resposta && (
                <p style={{ margin: 0 }} role="status">
                  {resposta.acertou
                    ? "✔ Você acertou!"
                    : `✘ Você marcou ${resposta.letraEscolhida}; a resposta certa é ${resposta.letraCorreta}.`}
                </p>
              )}
            </li>
          );
        })}
      </ol>

      {concluida && (
        <p>
          Atividade concluída. Seu desempenho foi atualizado — veja em <strong>Meu desempenho</strong> ou faça uma
          prática adaptativa para treinar os tópicos em que você mais errou.
        </p>
      )}
    </div>
  );
}
