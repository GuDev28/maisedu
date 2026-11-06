import { useEffect, useState } from "react";
import { listarMinhasTurmas } from "../services/turmaService";
import { desempenhoDaTurma, desempenhoDoAlunoNaTurma } from "../services/atividadeService";
import { BarraPercentual } from "../components/BarraPercentual";
import { CartaoNumero } from "../components/CartaoNumero";
import { GraficoBarras } from "../components/GraficoBarras";
import { GraficoEvolucao } from "../components/GraficoEvolucao";
import { PainelDesempenho } from "../components/PainelDesempenho";
import { NIVEIS, formatarPercentual, rotuloAno } from "../components/rotulos";
import { Mensagens } from "../components/Mensagens";

/** Texto de um nível na tabela da turma: "acertos/respostas (percentual)" ou "—" sem respostas. */
function celulaNivel(topico, nivel) {
  const encontrado = topico.niveis.find((n) => n.nivelDificuldade === nivel);
  if (!encontrado || encontrado.totalRespostas === 0) {
    return "—";
  }
  return `${encontrado.totalAcertos}/${encontrado.totalRespostas} (${formatarPercentual(encontrado.percentual)})`;
}

/**
 * RF19 (professor): painel de desempenho das turmas em que leciona (o admin vê as da instituição).
 * Indicadores da turma, acerto por tópico, evolução semanal, detalhe por tópico e nível e resumo por aluno,
 * com acesso ao painel individual de cada aluno.
 */
export default function DesempenhoTurma() {
  const [turmas, setTurmas] = useState([]);
  const [carregandoTurmas, setCarregandoTurmas] = useState(true);
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
      .catch((err) => setErro(err.message))
      .finally(() => setCarregandoTurmas(false));
  }, []);

  useEffect(() => {
    if (!turmaId) {
      return;
    }
    setAlunoAberto(null);
    setDados(null);
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

  const resumo = dados?.resumo;
  const maisFraco = dados?.topicos?.[0];
  const barras = (dados?.topicos ?? []).map((topico) => ({
    chave: topico.topicoId,
    rotulo: topico.topico,
    valor: topico.percentual,
    detalhe: `${topico.totalAcertos} acertos em ${topico.totalRespostas} respostas de ${topico.alunosComRespostas} aluno(s)`,
  }));

  return (
    <div className="pagina">
      <h2>Painel de desempenho da turma</h2>
      {carregandoTurmas && <p>Carregando...</p>}
      {!carregandoTurmas && turmas.length === 0 && !erro && <p>Você ainda não está vinculado a nenhuma turma.</p>}
      {turmas.length > 0 && (
        <div className="campos">
          <label>
            Turma
            <select className="form-select" value={turmaId} onChange={(e) => setTurmaId(e.target.value)}>
              {turmas.map((turma) => (
                <option key={turma.id} value={turma.id}>{turma.nome} ({rotuloAno(turma.anoEscolar)})</option>
              ))}
            </select>
          </label>
        </div>
      )}
      <Mensagens erro={erro} />
      {turmaId && !dados && !erro && <p>Carregando...</p>}

      {dados && !alunoAberto && (
        <>
          <div className="resumo-numeros">
            <CartaoNumero valor={resumo.totalAlunos} rotulo="alunos"
                          detalhe={`${resumo.alunosComRespostas} já responderam`} />
            <CartaoNumero valor={resumo.totalAtividades} rotulo="atividades criadas" />
            <CartaoNumero valor={resumo.totalRespostas} rotulo="respostas" detalhe={`${resumo.totalAcertos} acertos`} />
            <CartaoNumero rotulo="acerto da turma">
              <BarraPercentual valor={resumo.percentual} destaque />
            </CartaoNumero>
            {maisFraco && (
              <CartaoNumero valor={maisFraco.topico} rotulo="tópico mais fraco" texto
                            detalhe={`${formatarPercentual(maisFraco.percentual)} de acerto`} />
            )}
          </div>

          <div className="painel-graficos">
            <section>
              <h3>Acerto por tópico</h3>
              <GraficoBarras itens={barras}
                             descricao="Percentual de acerto da turma por tópico, do mais fraco para o mais forte" />
            </section>
            <section>
              <h3>Evolução da turma</h3>
              <GraficoEvolucao pontos={dados.evolucao} />
            </section>
          </div>

          <h3>Tópicos por nível (do mais fraco para o mais forte)</h3>
          {dados.topicos.length === 0 ? (
            <p>Nenhuma resposta registrada nesta turma ainda.</p>
          ) : (
            <table className="table table-hover align-middle tabela">
              <thead>
                <tr>
                  <th>Tópico</th>
                  <th>Alunos</th>
                  <th>Acerto</th>
                  {NIVEIS.map((nivel) => (
                    <th key={nivel.valor}>{nivel.rotulo}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {dados.topicos.map((topico, indice) => (
                  <tr key={topico.topicoId}>
                    <td>
                      {topico.topico}
                      {indice === 0 && dados.topicos.length > 1 && (
                        <span className="etiqueta etiqueta-alerta">precisa de atenção</span>
                      )}
                    </td>
                    <td>{topico.alunosComRespostas}</td>
                    <td><BarraPercentual valor={topico.percentual} /></td>
                    {NIVEIS.map((nivel) => (
                      <td key={nivel.valor} className="linha-nivel">{celulaNivel(topico, nivel.valor)}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          <h3>Alunos</h3>
          {dados.alunos.length === 0 ? (
            <p>Nenhum aluno vinculado a esta turma.</p>
          ) : (
            <table className="table table-hover align-middle tabela">
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
                        ? `${aluno.topicoMaisFraco} (${formatarPercentual(aluno.percentualTopicoMaisFraco)})`
                        : "—"}
                    </td>
                    <td>
                      <button className="btn btn-outline-secondary btn-sm" type="button" onClick={() => abrirAluno(aluno)} disabled={aluno.totalRespostas === 0}>
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
          <button className="btn btn-link px-0 mb-2" type="button" onClick={() => setAlunoAberto(null)}>← Voltar para a turma</button>
          <h3>{alunoAberto.nome}</h3>
          <PainelDesempenho desempenho={alunoAberto} />
        </div>
      )}
    </div>
  );
}
