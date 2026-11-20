import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../context/useAuth";
import { BarraPercentual } from "../../components/BarraPercentual";
import { Carregando } from "../../components/Carregando";
import { formatarDataHora, formatarPercentual, tempoAtePrazo } from "../../components/rotulos";
import { listarMinhasAtividades, meuDesempenho } from "../../services/alunoService";

export default function InicioAluno() {
  const { usuario } = useAuth();
  const [desempenho, setDesempenho] = useState(null);
  const [atividades, setAtividades] = useState(null);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    Promise.all([meuDesempenho(), listarMinhasAtividades()])
      .then(([painel, lista]) => {
        setDesempenho(painel);
        setAtividades(lista);
      })
      .catch((err) => setErro(err.message));
  }, []);

  let atencao = null;
  for (const topico of desempenho?.topicos ?? []) {
    if (topico.totalRespostas > 0 && (!atencao || Number(topico.percentual) < Number(atencao.percentual))) {
      atencao = topico;
    }
  }
  // Pendentes: atividades da turma ainda não enviadas e dentro do prazo; a próxima é a de prazo mais perto.
  const pendentes = (atividades ?? [])
    .filter((a) => a.tipo === "REGULAR" && (a.situacao === "NAO_INICIADA" || a.situacao === "EM_ANDAMENTO"))
    .sort((a, b) => new Date(a.prazo) - new Date(b.prazo));
  const proxima = pendentes[0];

  return (
    <>
      <div className="mb-4">
        <h1 className="me-titulo">Olá, {usuario.nome.split(" ")[0]}!</h1>
        <p className="text-secondary mb-0">Bora praticar Matemática?</p>
      </div>
      {erro && <div className="alert alert-danger">{erro}</div>}
      {!desempenho && !erro && <Carregando />}
      {desempenho && (
        <div className="row g-3">
          <div className="col-md-6 col-lg-4">
            <div className="card h-100">
              <div className="card-body">
                <div className="text-secondary small">Acerto geral</div>
                <div className="me-numero">
                  {desempenho.totalRespostas > 0 ? formatarPercentual(desempenho.percentualGeral) : "—"}
                </div>
                <div className="small text-secondary">
                  {desempenho.totalAcertos} acerto(s) em {desempenho.totalRespostas} resposta(s)
                </div>
                <Link to="/meu-desempenho" className="small">
                  Ver meu desempenho
                </Link>
              </div>
            </div>
          </div>
          <div className="col-md-6 col-lg-4">
            <div className="card h-100">
              <div className="card-body">
                <div className="text-secondary small">Precisa de atenção</div>
                {atencao ? (
                  <>
                    <div className="fw-semibold fs-5">{atencao.topico}</div>
                    <BarraPercentual valor={atencao.percentual} />
                  </>
                ) : (
                  <p className="mb-0 mt-1">Responda algumas questões para o MaisEdu descobrir seus pontos fortes.</p>
                )}
              </div>
            </div>
          </div>
          <div className="col-lg-4">
            <div className="card h-100 border-primary">
              <div className="card-body d-flex flex-column">
                <div className="text-secondary small">Prática adaptativa</div>
                <p className="mb-3 mt-1">
                  Deixe o MaisEdu escolher as questões pelos seus pontos fracos ou escolha o tópico que quer treinar.
                </p>
                <Link to="/praticar" className="btn btn-primary mt-auto">
                  Praticar agora
                </Link>
              </div>
            </div>
          </div>
          <div className="col-12">
            <div className="card">
              <div className="card-body d-flex flex-wrap align-items-center gap-3">
                <div className="me-auto">
                  <div className="text-secondary small">Próxima entrega</div>
                  {proxima ? (
                    <>
                      <div className="fw-semibold">
                        {proxima.titulo}{" "}
                        <span className="text-secondary fw-normal">
                          · {proxima.turmaNome} · {proxima.escolhidas}/{proxima.totalQuestoes} respondidas
                        </span>
                      </div>
                      <div className="small">
                        Prazo: {formatarDataHora(proxima.prazo)} ({tempoAtePrazo(proxima.prazo)})
                      </div>
                    </>
                  ) : (
                    <div>Nenhuma atividade pendente. Que tal praticar?</div>
                  )}
                  {pendentes.length > 1 && (
                    <div className="small text-secondary">{pendentes.length} atividades para entregar</div>
                  )}
                </div>
                <Link to="/minhas-atividades" className="btn btn-outline-primary">
                  {proxima ? "Ir para as atividades" : "Ver atividades"}
                </Link>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
