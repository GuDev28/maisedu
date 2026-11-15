import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { desempenhoDaTurma, desempenhoDoAlunoNaTurma } from "../services/atividadeService";
import { BarraPercentual } from "../components/BarraPercentual";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { EstadoVazio } from "../components/EstadoVazio";
import { Mensagens } from "../components/Mensagens";
import { Modal } from "../components/Modal";
import { PainelDesempenho } from "../components/PainelDesempenho";
import { SeletorTurma } from "../components/SeletorTurma";
import { formatarPercentual } from "../components/rotulos";

/**
 * RF19 (professor): desempenho de uma turma por tópico (do mais fraco para o mais forte) e por
 * aluno. Usado na página Desempenho e na aba Desempenho da tela da Turma.
 */
export function DesempenhoDaTurma({ turmaId }) {
  const [dados, setDados] = useState(null);
  const [alunoAberto, setAlunoAberto] = useState(null);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    setDados(null);
    setErro(null);
    desempenhoDaTurma(turmaId).then(setDados).catch((err) => setErro(err.message));
  }, [turmaId]);

  async function abrirAluno(aluno) {
    try {
      setAlunoAberto(await desempenhoDoAlunoNaTurma(turmaId, aluno.alunoId));
    } catch (err) {
      setErro(err.message);
    }
  }

  if (erro) {
    return <Mensagens erro={erro} />;
  }
  if (!dados) {
    return <Carregando />;
  }

  return (
    <>
      <h2 className="h6 text-secondary text-uppercase">Tópicos da turma (do mais fraco para o mais forte)</h2>
      {dados.topicos.length === 0 ? (
        <EstadoVazio titulo="Nenhuma resposta registrada nesta turma ainda">
          Os números aparecem assim que os alunos começarem a responder as atividades.
        </EstadoVazio>
      ) : (
        <div className="card mb-4">
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead>
                <tr>
                  <th>Tópico</th>
                  <th className="text-end">Alunos</th>
                  <th className="text-end">Respostas</th>
                  <th>Acerto</th>
                </tr>
              </thead>
              <tbody>
                {dados.topicos.map((topico, indice) => (
                  <tr key={topico.topicoId}>
                    <td>
                      {topico.topico}
                      {indice === 0 && dados.topicos.length > 1 && <span className="badge text-bg-warning ms-2">mais fraco</span>}
                    </td>
                    <td className="text-end">{topico.alunosComRespostas}</td>
                    <td className="text-end">{topico.totalRespostas}</td>
                    <td>
                      <BarraPercentual valor={topico.percentual} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <h2 className="h6 text-secondary text-uppercase mt-4">Alunos</h2>
      {dados.alunos.length === 0 ? (
        <p className="text-secondary">Nenhum aluno vinculado a esta turma.</p>
      ) : (
        <div className="card">
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead>
                <tr>
                  <th>Aluno</th>
                  <th className="text-end">Respostas</th>
                  <th>Acerto geral</th>
                  <th>Tópico mais fraco</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {dados.alunos.map((aluno) => (
                  <tr key={aluno.alunoId}>
                    <td>{aluno.nome}</td>
                    <td className="text-end">{aluno.totalRespostas}</td>
                    <td>{aluno.totalRespostas > 0 ? <BarraPercentual valor={aluno.percentual} /> : "—"}</td>
                    <td className="small">
                      {aluno.topicoMaisFraco
                        ? `${aluno.topicoMaisFraco} (${formatarPercentual(aluno.percentualTopicoMaisFraco)})`
                        : "—"}
                    </td>
                    <td className="text-end">
                      <button
                        type="button"
                        className="btn btn-sm btn-outline-primary"
                        onClick={() => abrirAluno(aluno)}
                        disabled={aluno.totalRespostas === 0}
                      >
                        Detalhes
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {alunoAberto && (
        <Modal titulo={alunoAberto.nome} tamanho="xl" aoFechar={() => setAlunoAberto(null)}>
          <PainelDesempenho desempenho={alunoAberto} />
        </Modal>
      )}
    </>
  );
}

export default function Desempenho() {
  const [parametros, setParametros] = useSearchParams();
  const [turmaId, setTurmaId] = useState(null);

  function escolher(id) {
    setTurmaId(id);
    setParametros({ turma: String(id) }, { replace: true });
  }

  return (
    <>
      <Cabecalho titulo="Desempenho" acoes={<SeletorTurma valor={turmaId} aoMudar={escolher} turmaInicial={parametros.get("turma")} />}>
        Acerto da turma por tópico e de cada aluno. São os mesmos números que o motor adaptativo usa.
      </Cabecalho>
      {turmaId && <DesempenhoDaTurma turmaId={turmaId} />}
    </>
  );
}
