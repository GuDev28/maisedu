import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { gerarPraticaAdaptativa } from "../services/alunoService";
import { Cabecalho } from "../components/Cabecalho";
import { Mensagens } from "../components/Mensagens";
import { rotuloNivel } from "../components/rotulos";

/**
 * RF18 (aluno): monta a prática adaptativa e mostra o que o motor escolheu (tópicos e níveis)
 * antes de começar. Sem histórico, a primeira prática é diagnóstica: questões fáceis de vários
 * tópicos do ano da turma.
 */
export default function Praticar() {
  const navigate = useNavigate();
  const [pratica, setPratica] = useState(null);
  const [gerando, setGerando] = useState(false);
  const [erro, setErro] = useState(null);

  async function montar() {
    setErro(null);
    setGerando(true);
    try {
      setPratica(await gerarPraticaAdaptativa());
    } catch (err) {
      setErro(err.message);
    } finally {
      setGerando(false);
    }
  }

  return (
    <div className="me-largura-md">
      <Cabecalho titulo="Praticar agora">
        O MaisEdu olha as suas respostas, encontra os tópicos em que você mais erra e escolhe questões no nível
        certo para você.
      </Cabecalho>
      <Mensagens erro={erro} />

      {!pratica && (
        <div className="card">
          <div className="card-body p-4">
            <ol className="mb-4">
              <li>Os tópicos com o menor percentual de acerto vêm primeiro.</li>
              <li>Em cada tópico, você começa pelo nível que ainda não domina.</li>
              <li>Questões que você nunca respondeu têm prioridade.</li>
            </ol>
            <button type="button" className="btn btn-primary btn-lg" onClick={montar} disabled={gerando}>
              {gerando ? "Montando a sua prática..." : "Montar minha prática"}
            </button>
          </div>
        </div>
      )}

      {pratica && (
        <div className="card border-primary">
          <div className="card-body p-4">
            {pratica.diagnostica && (
              <span className="badge text-bg-info mb-2">Prática diagnóstica</span>
            )}
            <h2 className="h5">
              {!pratica.nova
                ? "Você tem uma prática em andamento"
                : pratica.diagnostica
                  ? "Vamos conhecer você!"
                  : "Sua prática está pronta"}
            </h2>
            <p className="text-secondary">
              {!pratica.nova
                ? "Termine esta prática antes de montar outra."
                : pratica.diagnostica
                  ? "Como você ainda não respondeu nenhuma questão, esta primeira prática tem questões fáceis de vários tópicos. Com as suas respostas, as próximas já vêm sob medida."
                  : `${pratica.totalQuestoes} questões escolhidas para você:`}
            </p>
            <ul className="list-group mb-4">
              {pratica.topicos.map((topico) => (
                <li key={`${topico.topico}-${topico.nivelDificuldade}`} className="list-group-item d-flex justify-content-between">
                  <span>
                    {topico.topico} <span className="text-secondary">· {rotuloNivel(topico.nivelDificuldade)}</span>
                  </span>
                  <span className="badge text-bg-light">{topico.questoes} questão(ões)</span>
                </li>
              ))}
            </ul>
            <button
              type="button"
              className="btn btn-primary btn-lg"
              onClick={() => navigate(`/minhas-atividades/${pratica.atividadeAlunoId}`)}
            >
              {pratica.nova ? "Começar" : "Continuar"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
