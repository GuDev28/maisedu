import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { desempenhoDaTurma, desempenhoDoAlunoNaTurma } from "../services/atividadeService";
import { Avatar } from "../components/Avatar";
import { BarraPercentual } from "../components/BarraPercentual";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { EstadoVazio } from "../components/EstadoVazio";
import { Mensagens } from "../components/Mensagens";
import { GraficoEvolucao } from "../components/GraficoEvolucao";
import { GraficoHaltere } from "../components/GraficoHaltere";
import { Dica, LegendaCalor } from "../components/MapaDeCalor";
import { classeCalor } from "../components/escalaCalor";
import { useDica } from "../components/useDica";
import { Modal } from "../components/Modal";
import { PainelDesempenho } from "../components/PainelDesempenho";
import { SeletorTurma } from "../components/SeletorTurma";
import { formatarPercentual } from "../components/rotulos";

/** Célula com menos respostas que isto ganha um asterisco: o percentual ainda diz pouco. */
const POUCAS_RESPOSTAS = 3;

/**
 * RF19 (professor): desempenho de uma turma por tópico (do mais fraco para o mais forte), mapa de
 * calor aluno × tópico, evolução semanal, efeito da prática adaptativa e a lista de alunos. Só entram
 * respostas corrigidas (atividades depois do prazo e práticas enviadas). Usado na página Desempenho e
 * na aba Desempenho da tela da Turma.
 */
export function DesempenhoDaTurma({ turmaId }) {
  // a chave recomeça a seção a cada turma: nada da turma anterior fica na tela enquanto carrega
  return <DesempenhoDaTurmaConteudo key={turmaId} turmaId={turmaId} />;
}

function DesempenhoDaTurmaConteudo({ turmaId }) {
  const [dados, setDados] = useState(null);
  const [alunoAberto, setAlunoAberto] = useState(null);
  const [erro, setErro] = useState(null);

  useEffect(() => {
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

      {dados.topicos.length > 0 && dados.alunos.length > 0 && (
        <>
          <h2 className="h6 text-secondary text-uppercase mt-4">Mapa de calor: alunos × tópicos</h2>
          <div className="card mb-4">
            <div className="card-body">
              <MapaAlunosTopicos dados={dados} aoAbrirAluno={abrirAluno} />
            </div>
          </div>
        </>
      )}

      {dados.evolucao.length > 0 && (
        <div className="row g-4 mb-4">
          <div className={`d-flex flex-column ${dados.efeitoPratica.length > 0 ? "col-xl-6" : "col-12"}`}>
            <h2 className="h6 text-secondary text-uppercase">Evolução da turma</h2>
            <div className="card flex-grow-1">
              <div className="card-body">
                <GraficoEvolucao
                  pontos={dados.evolucao}
                  rotulo="Acerto acumulado da turma por semana"
                  detalhe={(ponto) => `${ponto.alunosAtivos} aluno(s) responderam`}
                />
              </div>
            </div>
          </div>
          {dados.efeitoPratica.length > 0 && (
            <div className="col-xl-6 d-flex flex-column">
              <h2 className="h6 text-secondary text-uppercase">Efeito da prática adaptativa</h2>
              <div className="card flex-grow-1">
                <div className="card-body">
                  <EfeitoDaPratica efeitos={dados.efeitoPratica} />
                </div>
              </div>
            </div>
          )}
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

/**
 * Linhas: alunos (do menor para o maior acerto geral); colunas: tópicos (do mais fraco para o mais
 * forte). Cada célula traz o percentual do aluno no tópico; sem respostas, fica vazia.
 */
function MapaAlunosTopicos({ dados, aoAbrirAluno }) {
  const { ref, dica, alvo, esconder } = useDica();
  const porCelula = new Map();
  for (const celula of dados.celulas) {
    porCelula.set(`${celula.alunoId}-${celula.topicoId}`, celula);
  }
  const alunos = dados.alunos.filter((aluno) => aluno.totalRespostas > 0);
  const semRespostas = dados.alunos.length - alunos.length;

  return (
    <div ref={ref} className="position-relative" onMouseLeave={esconder}>
      <div className="table-responsive me-calor-rolagem">
        <table className="me-calor">
          <thead>
            <tr>
              <th scope="col" className="me-calor-fixa">Aluno</th>
              {dados.topicos.map((topico) => (
                <th key={topico.topicoId} scope="col" className="me-calor-topico">
                  <span>{topico.topico}</span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {alunos.map((aluno) => (
              <tr key={aluno.alunoId}>
                <th scope="row" className="me-calor-fixa">
                  <button
                    type="button"
                    className="me-calor-aluno"
                    onClick={() => aoAbrirAluno(aluno)}
                    title={`Ver o desempenho de ${aluno.nome}`}
                  >
                    <Avatar nome={aluno.nome} tamanho={30} />
                    <span className="me-calor-aluno-texto">
                      <span className="me-calor-aluno-nome">{aluno.nome}</span>
                      <span className="me-calor-aluno-geral">{formatarPercentual(aluno.percentual)} no geral</span>
                    </span>
                  </button>
                </th>
                {dados.topicos.map((topico) => {
                  const celula = porCelula.get(`${aluno.alunoId}-${topico.topicoId}`);
                  if (!celula || celula.respostas === 0) {
                    return (
                      <td key={topico.topicoId} className="me-calor-celula me-calor-vazio" aria-label="sem respostas">
                        ·
                      </td>
                    );
                  }
                  const poucas = celula.respostas < POUCAS_RESPOSTAS;
                  return (
                    <td
                      key={topico.topicoId}
                      className={`me-calor-celula ${classeCalor(celula.percentual)}`}
                      {...alvo(
                        <>
                          <div className="fw-semibold">{aluno.nome}</div>
                          <div>
                            {topico.topico}: {celula.acertos} de {celula.respostas} ({formatarPercentual(celula.percentual)})
                          </div>
                        </>
                      )}
                    >
                      {Math.round(Number(celula.percentual))}
                      {poucas ? "*" : ""}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <LegendaCalor
        notas={[
          `* menos de ${POUCAS_RESPOSTAS} respostas no tópico`,
          "· sem respostas",
          ...(semRespostas > 0 ? [`${semRespostas} aluno(s) sem respostas corrigidas não aparecem`] : []),
        ]}
      />
      <Dica dica={dica} />
    </div>
  );
}

/** Antes × depois da primeira prática adaptativa de cada tópico, com o aviso de que não é causal. */
function EfeitoDaPratica({ efeitos }) {
  const linhas = efeitos.map((efeito) => ({
    chave: efeito.topicoId,
    rotulo: efeito.topico,
    detalhe: `${efeito.alunos} aluno(s)`,
    antes: efeito.percentualAntes,
    depois: efeito.percentualDepois,
    dica: (
      <>
        <div className="fw-semibold">{efeito.topico}</div>
        <div>
          Antes: {formatarPercentual(efeito.percentualAntes)} em {efeito.respostasAntes} resposta(s)
        </div>
        <div>
          Depois: {formatarPercentual(efeito.percentualDepois)} em {efeito.respostasDepois} resposta(s)
        </div>
        <div>{efeito.alunos} aluno(s) com respostas nos dois períodos</div>
      </>
    ),
  }));
  return (
    <>
      <p className="small text-secondary">
        Acerto nas atividades da turma, por tópico, antes e depois da primeira prática adaptativa de cada aluno nesse tópico
        (as questões da própria prática não entram). Só conta quem respondeu nos dois períodos.
      </p>
      <GraficoHaltere linhas={linhas} rotuloAntes="Antes da 1ª prática" rotuloDepois="Depois da 1ª prática" />
      <p className="small text-secondary mt-3 mb-0">
        É uma comparação descritiva: a melhora pode ter outras causas (aulas, revisão, questões mais fáceis), então não prova
        sozinha que a prática causou a diferença.
      </p>
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
        Acerto da turma por tópico e de cada aluno, evolução e efeito da prática. Só entram respostas já corrigidas.
      </Cabecalho>
      {turmaId && <DesempenhoDaTurma turmaId={turmaId} />}
    </>
  );
}
