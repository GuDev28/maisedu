import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { CartaoNumero } from "../../components/CartaoNumero";
import { Carregando } from "../../components/Carregando";
import { EstadoVazio } from "../../components/EstadoVazio";
import { formatarPercentual, rotuloAno } from "../../components/rotulos";
import { desempenhoDaTurma, listarAtividadesDaTurma } from "../../services/atividadeService";
import { listarQuestoesPendentes } from "../../services/questaoService";
import { listarMinhasTurmas } from "../../services/turmaService";

/** Junta, para cada turma, o desempenho e as atividades em andamento. */
async function carregarTurmas() {
  const turmas = await listarMinhasTurmas();
  return Promise.all(
    turmas.map(async (turma) => {
      const [desempenho, atividades] = await Promise.all([
        desempenhoDaTurma(turma.id),
        listarAtividadesDaTurma(turma.id),
      ]);
      const totalAlunos = desempenho.alunos.length;
      let maisFraco = null;
      for (const topico of desempenho.topicos) {
        if (topico.totalRespostas > 0 && (!maisFraco || Number(topico.percentual) < Number(maisFraco.percentual))) {
          maisFraco = topico;
        }
      }
      const pendentes = [];
      for (const atividade of atividades) {
        const faltam = totalAlunos - atividade.alunosQueIniciaram;
        if (faltam > 0) {
          pendentes.push({ ...atividade, faltam });
        }
      }
      return { ...turma, totalAlunos, maisFraco, pendentes };
    })
  );
}

export default function InicioProfessor() {
  const { usuario } = useAuth();
  const [fila, setFila] = useState(null);
  const [turmas, setTurmas] = useState(null);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    listarQuestoesPendentes().then((lista) => setFila(lista.length)).catch(() => setFila(null));
    carregarTurmas().then(setTurmas).catch((err) => setErro(err.message));
  }, []);

  return (
    <>
      <div className="mb-4">
        <h1 className="me-titulo">Olá, {usuario.nome.split(" ")[0]}</h1>
        <p className="text-secondary mb-0">O que precisa da sua atenção hoje</p>
      </div>

      <div className="row g-3 mb-4">
        <div className="col-sm-6 col-lg-4">
          <CartaoNumero
            rotulo="Questões esperando o seu voto"
            valor={fila ?? "—"}
            detalhe="cada voto ajuda uma questão de outra escola a entrar no banco"
            para="/questoes/avaliar"
            tom={fila > 0 ? "warning" : ""}
          />
        </div>
        <div className="col-sm-6 col-lg-4">
          <CartaoNumero
            rotulo="Turmas em que você leciona"
            valor={turmas ? turmas.length : "—"}
            detalhe="cadastre e vincule alunos na tela da turma"
            para="/turmas"
          />
        </div>
        <div className="col-lg-4 d-flex flex-column gap-2 justify-content-center">
          <Link to="/questoes/nova" className="btn btn-primary">
            Criar questão
          </Link>
          <Link to="/atividades" className="btn btn-outline-primary">
            Nova atividade
          </Link>
        </div>
      </div>

      <h2 className="h5 mb-3">Suas turmas</h2>
      {erro && <div className="alert alert-danger">{erro}</div>}
      {!turmas && !erro && <Carregando />}
      {turmas && turmas.length === 0 && (
        <EstadoVazio titulo="Você ainda não leciona em nenhuma turma">
          O administrador da escola vincula os professores às turmas.
        </EstadoVazio>
      )}
      <div className="row g-3">
        {turmas?.map((turma) => (
          <div key={turma.id} className="col-md-6">
            <div className="card h-100">
              <div className="card-body">
                <div className="d-flex justify-content-between align-items-start">
                  <div>
                    <h3 className="h6 mb-0">{turma.nome}</h3>
                    <div className="small text-secondary">
                      {rotuloAno(turma.anoEscolar)} · {turma.totalAlunos} aluno(s)
                    </div>
                  </div>
                  <Link to={`/turmas/${turma.id}`} className="btn btn-sm btn-outline-secondary">
                    Abrir
                  </Link>
                </div>
                <hr />
                <div className="small mb-2">
                  <span className="text-secondary">Tópico mais fraco: </span>
                  {turma.maisFraco ? (
                    <strong>
                      {turma.maisFraco.topico} ({formatarPercentual(turma.maisFraco.percentual)})
                    </strong>
                  ) : (
                    <span className="text-secondary">sem respostas ainda</span>
                  )}
                </div>
                {turma.pendentes.length === 0 ? (
                  <div className="small text-secondary">Nenhuma atividade com alunos por começar.</div>
                ) : (
                  <ul className="small mb-0 ps-3">
                    {turma.pendentes.slice(0, 3).map((atividade) => (
                      <li key={atividade.id}>
                        <strong>{atividade.faltam}</strong> aluno(s) ainda não começaram “{atividade.titulo}”
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </>
  );
}
