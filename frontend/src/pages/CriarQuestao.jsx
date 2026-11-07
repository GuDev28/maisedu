import { useEffect, useState } from "react";
import { ANOS_ESCOLARES, rotuloAno, rotuloNivel } from "../components/rotulos";
import {
  criarQuestao,
  enviarParaValidacao,
  listarMinhasQuestoes,
  listarTopicos,
} from "../services/questaoService";
import { Mensagens } from "../components/Mensagens";

const CORES_STATUS = {
  RASCUNHO: "text-bg-secondary",
  PENDENTE: "text-bg-warning",
  APROVADA: "text-bg-success",
  REJEITADA: "text-bg-danger",
};

const NUMERO_ALTERNATIVAS = 5;
const NIVEIS = ["FACIL", "MEDIO", "DIFICIL"];

function alternativasVazias() {
  return Array(NUMERO_ALTERNATIVAS).fill("");
}

export default function CriarQuestao() {
  const [topicos, setTopicos] = useState([]);
  const [enunciado, setEnunciado] = useState("");
  const [topicoId, setTopicoId] = useState("");
  const [nivelDificuldade, setNivelDificuldade] = useState(NIVEIS[0]);
  const [anoEscolar, setAnoEscolar] = useState(ANOS_ESCOLARES[0].valor);
  const [alternativas, setAlternativas] = useState(alternativasVazias());
  const [indiceCorreta, setIndiceCorreta] = useState(0);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);
  const [sucesso, setSucesso] = useState(null);

  const [minhasQuestoes, setMinhasQuestoes] = useState([]);
  const [carregandoLista, setCarregandoLista] = useState(true);

  async function carregarMinhasQuestoes() {
    try {
      setMinhasQuestoes(await listarMinhasQuestoes());
    } catch {
      // silencioso: a lista é um complemento, não bloqueia o cadastro
    } finally {
      setCarregandoLista(false);
    }
  }

  useEffect(() => {
    listarTopicos()
      .then((lista) => {
        setTopicos(lista);
        if (lista.length > 0) {
          setTopicoId(String(lista[0].id));
        }
      })
      .catch((err) => setErro(err.message));
    carregarMinhasQuestoes();
  }, []);

  function handleAlternativaChange(indice, valor) {
    setAlternativas((atual) => {
      const copia = [...atual];
      copia[indice] = valor;
      return copia;
    });
  }

  function limparFormulario() {
    setEnunciado("");
    setAlternativas(alternativasVazias());
    setIndiceCorreta(0);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setErro(null);
    setSucesso(null);
    setCarregando(true);

    try {
      await criarQuestao({
        enunciado,
        topicoId: Number(topicoId),
        nivelDificuldade,
        anoEscolar,
        alternativas,
        indiceCorreta,
      });
      setSucesso("Questão salva como rascunho. Envie para validação quando estiver pronta.");
      limparFormulario();
      await carregarMinhasQuestoes();
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  async function handleEnviar(questaoId) {
    setErro(null);
    setSucesso(null);
    try {
      await enviarParaValidacao(questaoId);
      setSucesso("Questão enviada para validação dos outros professores.");
      await carregarMinhasQuestoes();
    } catch (err) {
      setErro(err.message);
    }
  }

  return (
    <div className="pagina">
      <h2>Criar questão</h2>

      <form onSubmit={handleSubmit} className="cartao">
        <div className="mb-3">
          <label className="form-label" htmlFor="enunciado">Enunciado</label>
          <textarea id="enunciado" className="form-control" value={enunciado}
                    onChange={(e) => setEnunciado(e.target.value)} rows={3} required />
        </div>

        <div className="row g-3 mb-3">
          <div className="col-md-6">
            <label className="form-label" htmlFor="topico">Tópico</label>
            <select id="topico" className="form-select" value={topicoId} onChange={(e) => setTopicoId(e.target.value)}
                    required>
              {topicos.map((topico) => (
                <option key={topico.id} value={topico.id}>{topico.nome}</option>
              ))}
            </select>
          </div>
          <div className="col-6 col-md-3">
            <label className="form-label" htmlFor="nivel">Dificuldade</label>
            <select id="nivel" className="form-select" value={nivelDificuldade}
                    onChange={(e) => setNivelDificuldade(e.target.value)}>
              {NIVEIS.map((nivel) => (
                <option key={nivel} value={nivel}>{rotuloNivel(nivel)}</option>
              ))}
            </select>
          </div>
          <div className="col-6 col-md-3">
            <label className="form-label" htmlFor="ano">Ano escolar</label>
            <select id="ano" className="form-select" value={anoEscolar} onChange={(e) => setAnoEscolar(e.target.value)}>
              {ANOS_ESCOLARES.map((ano) => (
                <option key={ano.valor} value={ano.valor}>{ano.rotulo}</option>
              ))}
            </select>
          </div>
        </div>

        <p className="form-label mb-2">Alternativas (marque a correta)</p>
        {alternativas.map((texto, indice) => (
          <div key={indice} className="input-group mb-2">
            <span className="input-group-text">
              <input className="form-check-input mt-0" type="radio" name="indiceCorreta"
                     aria-label={`Alternativa ${String.fromCharCode(65 + indice)} é a correta`}
                     checked={indiceCorreta === indice} onChange={() => setIndiceCorreta(indice)} />
            </span>
            <span className="input-group-text">{String.fromCharCode(65 + indice)}</span>
            <input type="text" className="form-control" value={texto}
                   aria-label={`Texto da alternativa ${String.fromCharCode(65 + indice)}`}
                   onChange={(e) => handleAlternativaChange(indice, e.target.value)} required />
          </div>
        ))}

        <button type="submit" className="btn btn-primary mt-2" disabled={carregando || topicos.length === 0}>
          {carregando ? "Salvando..." : "Salvar rascunho"}
        </button>
      </form>

      <Mensagens erro={erro} sucesso={sucesso} />

      <h3 className="mt-4">Minhas questões</h3>
      {carregandoLista ? (
        <p>Carregando...</p>
      ) : minhasQuestoes.length === 0 ? (
        <p>Nenhuma questão criada ainda.</p>
      ) : (
        <ul className="list-group">
          {minhasQuestoes.map((questao) => (
            <li key={questao.id} className="list-group-item">
              <div className="d-flex justify-content-between align-items-start gap-2">
                <div>
                  <strong>#{questao.id}</strong> — {questao.enunciado}
                  <div className="small text-body-secondary">
                    {questao.topico} · {rotuloNivel(questao.nivelDificuldade)} · {rotuloAno(questao.anoEscolar)}
                  </div>
                </div>
                <span className={`badge ${CORES_STATUS[questao.status] ?? "text-bg-secondary"}`}>{questao.status}</span>
              </div>
              {questao.status === "RASCUNHO" && (
                <button type="button" className="btn btn-outline-primary btn-sm mt-2"
                        onClick={() => handleEnviar(questao.id)}>
                  Enviar para validação
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
