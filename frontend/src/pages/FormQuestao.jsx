import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useInterface } from "../context/InterfaceContext";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { Mensagens } from "../components/Mensagens";
import { ANOS_ESCOLARES, NIVEIS } from "../components/rotulos";
import {
  criarQuestao,
  editarQuestao,
  enviarParaValidacao,
  listarMinhasQuestoes,
  listarTopicos,
} from "../services/questaoService";

const NUMERO_ALTERNATIVAS = 5;
const LETRAS = ["A", "B", "C", "D", "E"];

/**
 * Criar ou editar uma questão (só rascunho). Dois botões: "Salvar rascunho" e "Salvar e enviar"
 * para a validação colaborativa, num clique só.
 */
export default function FormQuestao() {
  const { questaoId } = useParams();
  const editando = Boolean(questaoId);
  const navigate = useNavigate();
  const { avisar } = useInterface();
  const [topicos, setTopicos] = useState([]);
  const [enunciado, setEnunciado] = useState("");
  const [topicoId, setTopicoId] = useState("");
  const [nivelDificuldade, setNivelDificuldade] = useState(NIVEIS[0].valor);
  const [anoEscolar, setAnoEscolar] = useState(ANOS_ESCOLARES[0].valor);
  const [alternativas, setAlternativas] = useState(Array(NUMERO_ALTERNATIVAS).fill(""));
  const [indiceCorreta, setIndiceCorreta] = useState(0);
  const [carregando, setCarregando] = useState(editando);
  const [salvando, setSalvando] = useState(null);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    listarTopicos()
      .then((lista) => {
        setTopicos(lista);
        if (!editando && lista.length > 0) {
          setTopicoId(String(lista[0].id));
        }
      })
      .catch((err) => setErro(err.message));
  }, [editando]);

  useEffect(() => {
    if (!editando) {
      return;
    }
    listarMinhasQuestoes()
      .then((minhas) => {
        const questao = minhas.find((q) => String(q.id) === questaoId);
        if (!questao) {
          setErro("Questão não encontrada entre as suas.");
          return;
        }
        if (questao.status !== "RASCUNHO") {
          setErro("Esta questão já foi enviada para validação e não pode mais ser editada.");
        }
        setEnunciado(questao.enunciado);
        setTopicoId(String(questao.topicoId));
        setNivelDificuldade(questao.nivelDificuldade);
        setAnoEscolar(questao.anoEscolar);
        setAlternativas(questao.alternativas.map((a) => a.texto));
        setIndiceCorreta(Math.max(0, questao.alternativas.findIndex((a) => a.correta)));
      })
      .catch((err) => setErro(err.message))
      .finally(() => setCarregando(false));
  }, [editando, questaoId]);

  function mudarAlternativa(indice, valor) {
    setAlternativas((atual) => atual.map((texto, i) => (i === indice ? valor : texto)));
  }

  async function salvar(enviar) {
    setErro(null);
    setSalvando(enviar ? "enviar" : "rascunho");
    const dados = { enunciado, topicoId: Number(topicoId), nivelDificuldade, anoEscolar, alternativas, indiceCorreta };
    try {
      const questao = editando ? await editarQuestao(questaoId, dados) : await criarQuestao(dados);
      if (enviar) {
        await enviarParaValidacao(questao.id);
        avisar("Questão enviada para a validação dos professores. Acompanhe o placar em Minhas questões.");
      } else {
        avisar(editando ? "Rascunho atualizado." : "Questão salva como rascunho.");
      }
      navigate("/questoes/minhas");
    } catch (err) {
      setErro(err.message);
      setSalvando(null);
    }
  }

  if (carregando) {
    return <Carregando />;
  }

  return (
    <div className="me-largura-md">
      <Link to="/questoes/minhas" className="small">
        ← Minhas questões
      </Link>
      <div className="mt-2">
        <Cabecalho titulo={editando ? "Editar rascunho" : "Nova questão"}>
          Cinco alternativas, uma correta. Depois de enviada, a questão é votada por professores de outras escolas e
          só entra no banco com 3 aprovações.
        </Cabecalho>
      </div>
      <Mensagens erro={erro} />

      <form
        className="card"
        onSubmit={(evento) => {
          evento.preventDefault();
          salvar(false);
        }}
      >
        <div className="card-body p-4">
          <div className="mb-3">
            <label htmlFor="enunciado" className="form-label">
              Enunciado
            </label>
            <textarea
              id="enunciado"
              className="form-control"
              rows={3}
              value={enunciado}
              onChange={(e) => setEnunciado(e.target.value)}
              maxLength={2000}
              required
            />
          </div>
          <div className="row g-3 mb-4">
            <div className="col-md-6">
              <label htmlFor="topico" className="form-label">
                Tópico
              </label>
              <select id="topico" className="form-select" value={topicoId} onChange={(e) => setTopicoId(e.target.value)} required>
                {topicos.map((topico) => (
                  <option key={topico.id} value={topico.id}>
                    {topico.nome}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-6 col-md-3">
              <label htmlFor="nivel" className="form-label">
                Nível
              </label>
              <select id="nivel" className="form-select" value={nivelDificuldade} onChange={(e) => setNivelDificuldade(e.target.value)}>
                {NIVEIS.map((nivel) => (
                  <option key={nivel.valor} value={nivel.valor}>
                    {nivel.rotulo}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-6 col-md-3">
              <label htmlFor="ano" className="form-label">
                Ano
              </label>
              <select id="ano" className="form-select" value={anoEscolar} onChange={(e) => setAnoEscolar(e.target.value)}>
                {ANOS_ESCOLARES.map((ano) => (
                  <option key={ano.valor} value={ano.valor}>
                    {ano.rotulo}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <fieldset>
            <legend className="form-label fs-6">Alternativas (marque a correta)</legend>
            {alternativas.map((texto, indice) => (
              <div key={LETRAS[indice]} className="input-group mb-2">
                <span className="input-group-text">
                  <input
                    type="radio"
                    className="form-check-input mt-0"
                    name="correta"
                    checked={indiceCorreta === indice}
                    onChange={() => setIndiceCorreta(indice)}
                    aria-label={`Alternativa ${LETRAS[indice]} é a correta`}
                  />
                </span>
                <span className="input-group-text fw-semibold">{LETRAS[indice]}</span>
                <input
                  className={`form-control ${indiceCorreta === indice ? "border-success" : ""}`}
                  value={texto}
                  onChange={(e) => mudarAlternativa(indice, e.target.value)}
                  aria-label={`Texto da alternativa ${LETRAS[indice]}`}
                  maxLength={500}
                  required
                />
              </div>
            ))}
          </fieldset>
        </div>
        <div className="card-footer bg-white d-flex flex-wrap gap-2 justify-content-end">
          <Link to="/questoes/minhas" className="btn btn-outline-secondary">
            Cancelar
          </Link>
          <button type="submit" className="btn btn-outline-primary" disabled={salvando !== null}>
            {salvando === "rascunho" ? "Salvando..." : "Salvar rascunho"}
          </button>
          <button
            type="button"
            className="btn btn-primary"
            disabled={salvando !== null}
            onClick={(evento) => {
              const formulario = evento.currentTarget.form;
              if (formulario.reportValidity()) {
                salvar(true);
              }
            }}
          >
            {salvando === "enviar" ? "Enviando..." : "Salvar e enviar para validação"}
          </button>
        </div>
      </form>
    </div>
  );
}
