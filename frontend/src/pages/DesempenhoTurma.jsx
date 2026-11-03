import { useEffect, useState } from "react";
import { listarMinhasTurmas } from "../services/turmaService";
import { desempenhoDaTurma, desempenhoDoAlunoNaTurma } from "../services/atividadeService";
import { BarraPercentual } from "../components/BarraPercentual";
import { PainelDesempenho } from "../components/PainelDesempenho";
import { rotuloAno } from "../components/rotulos";
import { Mensagens } from "../components/Mensagens";

/** RF19 (professor): desempenho dos alunos das turmas em que leciona, por aluno e por tópico. */
export default function DesempenhoTurma() {
  const [turmas, setTurmas] = useState([]);
  const [turmaId, setTurmaId] = useState("");
  const [dados, setDados] = useState(null);
  const [alunoAberto, setAlunoAberto] = useState(null);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    listarMinhasTurmas()
      .then((lista) => {
        setTurmas(lista);
        if (lista.length > 0) {
          setTurmaId(String(lista[0].id));
        }
      })
      .catch((err) => setErro(err.message));
  }, []);

  useEffect(() => {
    if (!turmaId) {
      return;
    }
    setAlunoAberto(null);
    setErro(null);
    desempenhoDaTurma(turmaId).then(setDados).catch((err) => setErro(err.message));
  }, [turmaId]);

  async function abrirAluno(aluno) {
    try {
      const desempenho = await desempenhoDoAlunoNaTurma(turmaId, aluno.alunoId);
      setAlunoAberto(desempenho);
    } catch (err) {
      setErro(err.message);
    }
  }

  return (
    <div className="pagina">
      <h2>Desempenho da turma</h2>
      {turmas.length === 0 && <p>Você ainda não está vinculado a nenhuma turma.</p>}
      {turmas.length > 0 && (
        <div className="campos">
          <label>
            Turma
            <select value={turmaId} onChange={(e) => setTurmaId(e.target.value)}>
              {turmas.map((turma) => (
                <option key={turma.id} value={turma.id}>{turma.nome} ({rotuloAno(turma.anoEscolar)})</option>
              ))}
            </select>
          </label>
        </div>
      )}
      <Mensagens erro={erro} />

      {dados && !alunoAberto && (
        <>
          <h3>Tópicos da turma (do mais fraco para o mais forte)</h3>
          {dados.topicos.length === 0 ? (
            <p>Nenhuma resposta registrada nesta turma ainda.</p>
          ) : (
            <table className="tabela">
              <thead>
                <tr><th>Tópico</th><th>Alunos</th><th>Respostas</th><th>Acerto</th></tr>
              </thead>
              <tbody>
                {dados.topicos.map((topico) => (
                  <tr key={topico.topicoId}>
                    <td>{topico.topico}</td>
                    <td>{topico.alunosComRespostas}</td>
                    <td>{topico.totalRespostas}</td>
                    <td><BarraPercentual valor={topico.percentual} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          <h3>Alunos</h3>
          {dados.alunos.length === 0 ? (
            <p>Nenhum aluno vinculado a esta turma.</p>
          ) : (
            <table className="tabela">
              <thead>
                <tr><th>Aluno</th><th>Respostas</th><th>Acerto geral</th><th>Tópico mais fraco</th><th></th></tr>
              </thead>
              <tbody>
                {dados.alunos.map((aluno) => (
                  <tr key={aluno.alunoId}>
                    <td>{aluno.nome}</td>
                    <td>{aluno.totalRespostas}</td>
                    <td>{aluno.totalRespostas > 0 ? <BarraPercentual valor={aluno.percentual} /> : "—"}</td>
                    <td>
                      {aluno.topicoMaisFraco
                        ? `${aluno.topicoMaisFraco} (${Number(aluno.percentualTopicoMaisFraco).toLocaleString("pt-BR")}%)`
                        : "—"}
                    </td>
                    <td>
                      <button type="button" onClick={() => abrirAluno(aluno)} disabled={aluno.totalRespostas === 0}>
                        Detalhes
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}

      {alunoAberto && (
        <div>
          <button type="button" onClick={() => setAlunoAberto(null)}>← Voltar para a turma</button>
          <h3>{alunoAberto.nome}</h3>
          <PainelDesempenho desempenho={alunoAberto} />
        </div>
      )}
    </div>
  );
}
