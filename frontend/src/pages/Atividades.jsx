import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { listarAprovadas } from "../services/questaoService";
import { criarAtividade, listarAtividadesDaTurma, resultadosDaAtividade } from "../services/atividadeService";
import { useInterface } from "../context/InterfaceContext";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { AlternativasComGabarito, EtiquetasQuestao } from "../components/EtiquetasQuestao";
import { EstadoVazio } from "../components/EstadoVazio";
import { FILTROS_VAZIOS, FiltrosBanco } from "../components/FiltrosBanco";
import { Mensagens } from "../components/Mensagens";
import { Modal } from "../components/Modal";
import { SeletorTurma } from "../components/SeletorTurma";
import { SITUACOES, formatarDataHora } from "../components/rotulos";

/**
 * RF15: atividades de uma turma. O professor escolhe questões APROVADAS do banco compartilhado (de
 * qualquer escola), define a ordem e acompanha quem já respondeu. Usada na página Atividades (com
 * seletor de turma) e na aba Atividades da tela da Turma.
 */
export function AtividadesDaTurma({ turmaId, podeCriar = true }) {
  const { avisar } = useInterface();
  const [atividades, setAtividades] = useState(null);
  const [resultado, setResultado] = useState(null);
  const [criando, setCriando] = useState(false);
  const [erro, setErro] = useState(null);

  async function carregar() {
    try {
      setAtividades(await listarAtividadesDaTurma(turmaId));
    } catch (err) {
      setErro(err.message);
    }
  }

  useEffect(() => {
    setAtividades(null);
    setCriando(false);
    setErro(null);
    carregar();
    // recarrega só quando a turma muda
  }, [turmaId]);

  async function verResultados(atividadeId) {
    setErro(null);
    try {
      setResultado(await resultadosDaAtividade(atividadeId));
    } catch (err) {
      setErro(err.message);
    }
  }

  async function aoCriar(atividade) {
    setCriando(false);
    avisar(`Atividade "${atividade.titulo}" criada com ${atividade.totalQuestoes} questão(ões). Os alunos já podem começar.`);
    await carregar();
  }

  if (criando) {
    return <NovaAtividade turmaId={turmaId} aoCriar={aoCriar} aoCancelar={() => setCriando(false)} />;
  }

  return (
    <>
      <Mensagens erro={erro} />
      {podeCriar && (
        <div className="d-flex justify-content-end mb-3">
          <button type="button" className="btn btn-primary" onClick={() => setCriando(true)}>
            Nova atividade
          </button>
        </div>
      )}
      {!atividades && !erro && <Carregando />}
      {atividades && atividades.length === 0 && (
        <EstadoVazio
          titulo="Nenhuma atividade nesta turma ainda"
          acao={
            podeCriar && (
              <button type="button" className="btn btn-primary" onClick={() => setCriando(true)}>
                Criar a primeira atividade
              </button>
            )
          }
        >
          Monte uma atividade com questões aprovadas do banco compartilhado.
        </EstadoVazio>
      )}
      {atividades && atividades.length > 0 && (
        <div className="card">
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead>
                <tr>
                  <th>Atividade</th>
                  <th className="text-end">Questões</th>
                  <th className="text-end">Iniciaram</th>
                  <th className="text-end">Concluíram</th>
                  <th>Criada em</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {atividades.map((atividade) => (
                  <tr key={atividade.id}>
                    <td>
                      <div className="fw-semibold">{atividade.titulo}</div>
                      <div className="small text-secondary">por {atividade.professorNome}</div>
                    </td>
                    <td className="text-end">{atividade.totalQuestoes}</td>
                    <td className="text-end">{atividade.alunosQueIniciaram}</td>
                    <td className="text-end">{atividade.alunosQueConcluiram}</td>
                    <td className="small">{formatarDataHora(atividade.criadoEm)}</td>
                    <td className="text-end">
                      <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => verResultados(atividade.id)}>
                        Resultados
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {resultado && (
        <Modal titulo={`Resultados: ${resultado.titulo}`} tamanho="lg" aoFechar={() => setResultado(null)}>
          {resultado.alunos.length === 0 ? (
            <p className="text-secondary">A turma ainda não tem alunos.</p>
          ) : (
            <table className="table table-sm align-middle">
              <thead>
                <tr>
                  <th>Aluno</th>
                  <th>Situação</th>
                  <th className="text-end">Respondidas</th>
                  <th className="text-end">Acertos</th>
                </tr>
              </thead>
              <tbody>
                {resultado.alunos.map((aluno) => {
                  const situacao = SITUACOES[aluno.situacao];
                  return (
                    <tr key={aluno.alunoId}>
                      <td>{aluno.nome}</td>
                      <td>
                        <span className={`badge text-bg-${situacao?.cor ?? "secondary"}`}>{situacao?.rotulo ?? aluno.situacao}</span>
                      </td>
                      <td className="text-end">
                        {aluno.respondidas} de {aluno.totalQuestoes}
                      </td>
                      <td className="text-end">{aluno.acertos}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
          <details>
            <summary>Questões da atividade (com gabarito)</summary>
            <ol className="mt-2">
              {resultado.questoes.map((questao) => (
                <li key={questao.id} className="mb-3">
                  <div>{questao.enunciado}</div>
                  <EtiquetasQuestao questao={questao} />
                  <AlternativasComGabarito alternativas={questao.alternativas} />
                </li>
              ))}
            </ol>
          </details>
        </Modal>
      )}
    </>
  );
}

function NovaAtividade({ turmaId, aoCriar, aoCancelar }) {
  const [titulo, setTitulo] = useState("");
  const [filtros, setFiltros] = useState(FILTROS_VAZIOS);
  const [banco, setBanco] = useState(null);
  const [selecionadas, setSelecionadas] = useState([]);
  const [buscando, setBuscando] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState(null);

  async function buscar(filtrosDaBusca = filtros) {
    setBuscando(true);
    setErro(null);
    try {
      setBanco(await listarAprovadas(filtrosDaBusca));
    } catch (err) {
      setErro(err.message);
    } finally {
      setBuscando(false);
    }
  }

  useEffect(() => {
    buscar(FILTROS_VAZIOS);
  }, []);

  function alternar(questao) {
    setSelecionadas((atual) =>
      atual.some((q) => q.id === questao.id) ? atual.filter((q) => q.id !== questao.id) : [...atual, questao]
    );
  }

  function mover(indice, deslocamento) {
    setSelecionadas((atual) => {
      const destino = indice + deslocamento;
      if (destino < 0 || destino >= atual.length) {
        return atual;
      }
      const copia = [...atual];
      [copia[indice], copia[destino]] = [copia[destino], copia[indice]];
      return copia;
    });
  }

  async function salvar() {
    setSalvando(true);
    setErro(null);
    try {
      aoCriar(await criarAtividade(turmaId, titulo, selecionadas.map((q) => q.id)));
    } catch (err) {
      setErro(err.message);
      setSalvando(false);
    }
  }

  return (
    <div className="card">
      <div className="card-body p-4">
        <h2 className="h5 mb-3">Nova atividade</h2>
        <div className="mb-4">
          <label htmlFor="titulo-atividade" className="form-label">
            Título
          </label>
          <input
            id="titulo-atividade"
            className="form-control"
            value={titulo}
            onChange={(e) => setTitulo(e.target.value)}
            maxLength={150}
            placeholder="Ex.: Revisão de frações"
          />
        </div>

        <div className="row g-4">
          <div className="col-lg-7">
            <h3 className="h6 text-secondary text-uppercase">Banco compartilhado (questões aprovadas)</h3>
            <FiltrosBanco filtros={filtros} aoMudar={setFiltros} aoBuscar={() => buscar()} buscando={buscando} />
            <div className="list-group overflow-auto me-lista-rolavel">
              {banco === null && <div className="list-group-item"><Carregando /></div>}
              {banco?.length === 0 && (
                <div className="list-group-item text-secondary">Nenhuma questão aprovada com esses filtros.</div>
              )}
              {banco?.map((questao) => {
                const marcada = selecionadas.some((q) => q.id === questao.id);
                return (
                  <label key={questao.id} className={`list-group-item d-flex gap-2 ${marcada ? "active-subtle" : ""}`}>
                    <input
                      type="checkbox"
                      className="form-check-input flex-shrink-0 mt-1"
                      checked={marcada}
                      onChange={() => alternar(questao)}
                    />
                    <span>
                      {questao.enunciado}
                      <span className="d-block mt-1">
                        <EtiquetasQuestao questao={questao} />
                        <span className="small text-secondary ms-1">
                          {questao.autorNome}
                          {questao.autorInstituicao ? ` · ${questao.autorInstituicao}` : ""}
                        </span>
                      </span>
                    </span>
                  </label>
                );
              })}
            </div>
          </div>
          <div className="col-lg-5">
            <h3 className="h6 text-secondary text-uppercase">Ordem na atividade ({selecionadas.length})</h3>
            {selecionadas.length === 0 ? (
              <p className="text-secondary">Marque as questões ao lado. A ordem aqui é a que os alunos vão ver.</p>
            ) : (
              <ol className="list-group list-group-numbered">
                {selecionadas.map((questao, indice) => (
                  <li key={questao.id} className="list-group-item d-flex gap-2 align-items-start">
                    <span className="flex-grow-1 small">{questao.enunciado}</span>
                    <span className="btn-group btn-group-sm">
                      <button type="button" className="btn btn-outline-secondary" onClick={() => mover(indice, -1)} disabled={indice === 0} aria-label="Subir">
                        ↑
                      </button>
                      <button
                        type="button"
                        className="btn btn-outline-secondary"
                        onClick={() => mover(indice, 1)}
                        disabled={indice === selecionadas.length - 1}
                        aria-label="Descer"
                      >
                        ↓
                      </button>
                    </span>
                  </li>
                ))}
              </ol>
            )}
          </div>
        </div>

        <div className="mt-4">
          <Mensagens erro={erro} />
          <div className="d-flex gap-2">
            <button
              type="button"
              className="btn btn-primary"
              onClick={salvar}
              disabled={salvando || !titulo.trim() || selecionadas.length === 0}
            >
              {salvando ? "Criando..." : "Criar atividade"}
            </button>
            <button type="button" className="btn btn-outline-secondary" onClick={aoCancelar}>
              Cancelar
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

/** Página do menu: as atividades da turma escolhida no seletor. */
export default function Atividades() {
  const [parametros, setParametros] = useSearchParams();
  const [turmaId, setTurmaId] = useState(null);

  function escolher(id) {
    setTurmaId(id);
    setParametros({ turma: String(id) }, { replace: true });
  }

  return (
    <>
      <Cabecalho titulo="Atividades" acoes={<SeletorTurma valor={turmaId} aoMudar={escolher} turmaInicial={parametros.get("turma")} />}>
        Monte atividades com questões aprovadas do banco compartilhado e acompanhe quem já respondeu.
      </Cabecalho>
      {turmaId && <AtividadesDaTurma turmaId={turmaId} />}
    </>
  );
}
