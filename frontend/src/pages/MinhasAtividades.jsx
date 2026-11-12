import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { iniciarAtividade, listarMinhasAtividades } from "../services/alunoService";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { EstadoVazio } from "../components/EstadoVazio";
import { Mensagens } from "../components/Mensagens";
import { SITUACOES, formatarDataHora } from "../components/rotulos";

/** RF16 (aluno): atividades da turma e práticas adaptativas anteriores. */
export default function MinhasAtividades() {
  const navigate = useNavigate();
  const [atividades, setAtividades] = useState(null);
  const [abrindo, setAbrindo] = useState(null);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    listarMinhasAtividades()
      .then(setAtividades)
      .catch((err) => setErro(err.message));
  }, []);

  async function abrir(item) {
    setErro(null);
    if (item.atividadeAlunoId) {
      navigate(`/minhas-atividades/${item.atividadeAlunoId}`);
      return;
    }
    setAbrindo(item.atividadeId);
    try {
      // RN03: ao começar, as questões da atividade ficam fixas para este aluno
      const folha = await iniciarAtividade(item.atividadeId);
      navigate(`/minhas-atividades/${folha.atividadeAlunoId}`);
    } catch (err) {
      setErro(err.message);
      setAbrindo(null);
    }
  }

  const regulares = atividades?.filter((a) => a.tipo === "REGULAR") ?? [];
  const praticas = atividades?.filter((a) => a.tipo === "ADAPTATIVA") ?? [];

  return (
    <>
      <Cabecalho
        titulo="Minhas atividades"
        acoes={
          <Link to="/praticar" className="btn btn-primary">
            Praticar agora
          </Link>
        }
      >
        Atividades que o professor passou para a sua turma.
      </Cabecalho>
      <Mensagens erro={erro} />
      {!atividades && !erro && <Carregando />}

      {atividades && (
        <>
          {regulares.length === 0 ? (
            <EstadoVazio
              titulo="Nenhuma atividade da turma por enquanto"
              acao={
                <Link to="/praticar" className="btn btn-outline-primary">
                  Fazer uma prática adaptativa
                </Link>
              }
            >
              Quando o seu professor criar uma atividade, ela aparece aqui.
            </EstadoVazio>
          ) : (
            <div className="list-group mb-4">
              {regulares.map((item) => (
                <ItemAtividade key={`r${item.atividadeId}`} item={item} abrindo={abrindo} aoAbrir={abrir} />
              ))}
            </div>
          )}

          {praticas.length > 0 && (
            <>
              <h2 className="h6 text-secondary text-uppercase mt-4">Práticas adaptativas</h2>
              <div className="list-group">
                {praticas.map((item) => (
                  <ItemAtividade key={`p${item.atividadeAlunoId}`} item={item} abrindo={abrindo} aoAbrir={abrir} />
                ))}
              </div>
            </>
          )}
        </>
      )}
    </>
  );
}

function ItemAtividade({ item, abrindo, aoAbrir }) {
  const situacao = SITUACOES[item.situacao];
  const rotuloBotao =
    item.situacao === "NAO_INICIADA" ? "Começar" : item.situacao === "CONCLUIDA" ? "Rever" : "Continuar";
  const progresso = item.totalQuestoes > 0 ? Math.round((item.respondidas * 100) / item.totalQuestoes) : 0;
  return (
    <div className="list-group-item d-flex flex-wrap align-items-center gap-3 py-3">
      <div className="flex-grow-1">
        <div className="fw-semibold">
          {item.titulo}
          {item.turmaNome && <span className="text-secondary fw-normal"> · {item.turmaNome}</span>}
        </div>
        <div className="small text-secondary">
          <span className={`badge text-bg-${situacao?.cor ?? "secondary"} me-2`}>{situacao?.rotulo ?? item.situacao}</span>
          {item.respondidas} de {item.totalQuestoes} respondidas
          {item.respondidas > 0 && ` · ${item.acertos} acerto(s)`}
          {item.concluidaEm && ` · concluída em ${formatarDataHora(item.concluidaEm)}`}
        </div>
        {item.situacao === "EM_ANDAMENTO" && (
          <div
            className="progress mt-2"
            role="progressbar"
            aria-label="Progresso"
            aria-valuenow={progresso}
            aria-valuemin="0"
            aria-valuemax="100"
          >
            <div className="progress-bar" style={{ width: `${progresso}%` }} />
          </div>
        )}
      </div>
      <button
        type="button"
        className={`btn btn-sm ${item.situacao === "CONCLUIDA" ? "btn-outline-secondary" : "btn-primary"}`}
        disabled={abrindo === item.atividadeId && !item.atividadeAlunoId}
        onClick={() => aoAbrir(item)}
      >
        {abrindo === item.atividadeId && !item.atividadeAlunoId ? "Abrindo..." : rotuloBotao}
      </button>
    </div>
  );
}
