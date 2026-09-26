import { useEffect, useState } from "react";
import {
  criarQuestao,
  enviarParaValidacao,
  listarMinhasQuestoes,
  listarTopicos,
} from "../services/questaoService";

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
    <div style={{ maxWidth: 640, margin: "2rem auto" }}>
      <h2>Criar questão</h2>

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 12 }}>
          <label>
            Enunciado:{" "}
            <textarea
              value={enunciado}
              onChange={(e) => setEnunciado(e.target.value)}
              rows={3}
              style={{ width: "100%" }}
              required
            />
          </label>
        </div>

        <div style={{ marginBottom: 12, display: "flex", gap: "1rem" }}>
          <label>
            Tópico:{" "}
            <select value={topicoId} onChange={(e) => setTopicoId(e.target.value)} required>
              {topicos.map((topico) => (
                <option key={topico.id} value={topico.id}>
                  {topico.nome}
                </option>
              ))}
            </select>
          </label>

          <label>
            Dificuldade:{" "}
            <select value={nivelDificuldade} onChange={(e) => setNivelDificuldade(e.target.value)}>
              {NIVEIS.map((nivel) => (
                <option key={nivel} value={nivel}>
                  {nivel}
                </option>
              ))}
            </select>
          </label>
        </div>

        <p style={{ marginBottom: 4 }}>Alternativas (marque a correta):</p>
        {alternativas.map((texto, indice) => (
          <div key={indice} style={{ marginBottom: 8, display: "flex", alignItems: "center", gap: 8 }}>
            <input
              type="radio"
              name="indiceCorreta"
              checked={indiceCorreta === indice}
              onChange={() => setIndiceCorreta(indice)}
            />
            <span>{String.fromCharCode(65 + indice)})</span>
            <input
              type="text"
              value={texto}
              onChange={(e) => handleAlternativaChange(indice, e.target.value)}
              style={{ flex: 1 }}
              required
            />
          </div>
        ))}

        <button type="submit" disabled={carregando || topicos.length === 0}>
          {carregando ? "Salvando..." : "Salvar rascunho"}
        </button>
      </form>

      {erro && <p style={{ color: "red" }}>{erro}</p>}
      {sucesso && <p style={{ color: "green" }}>{sucesso}</p>}

      <h3 style={{ marginTop: "2rem" }}>Minhas questões</h3>
      {carregandoLista ? (
        <p>Carregando...</p>
      ) : minhasQuestoes.length === 0 ? (
        <p>Nenhuma questão criada ainda.</p>
      ) : (
        <ul style={{ listStyle: "none", padding: 0 }}>
          {minhasQuestoes.map((questao) => (
            <li
              key={questao.id}
              style={{ border: "1px solid #ddd", borderRadius: 4, padding: 8, marginBottom: 8 }}
            >
              <strong>#{questao.id}</strong> — {questao.enunciado}
              <br />
              <small>
                {questao.topico} · {questao.nivelDificuldade} · status: {questao.status}
              </small>
              {questao.status === "RASCUNHO" && (
                <div style={{ marginTop: 4 }}>
                  <button type="button" onClick={() => handleEnviar(questao.id)}>
                    Enviar para validação
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
