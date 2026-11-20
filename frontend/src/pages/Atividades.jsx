import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { listarAprovadas } from "../services/questaoService";
import { alterarPrazo, criarAtividade, listarAtividadesDaTurma, resultadosDaAtividade } from "../services/atividadeService";
import { useInterface } from "../context/useInterface";
import { BarraPercentual } from "../components/BarraPercentual";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { AlternativasComGabarito, EtiquetasQuestao } from "../components/EtiquetasQuestao";
import { EstadoVazio } from "../components/EstadoVazio";
import { FiltrosBanco } from "../components/FiltrosBanco";
import { Mensagens } from "../components/Mensagens";
import { Modal } from "../components/Modal";
import { SeletorTurma } from "../components/SeletorTurma";
import { FILTROS_VAZIOS, SITUACOES, formatarDataHora, formatarPercentual, paraCampoDataHora, rotuloNivel, tempoAtePrazo } from "../components/rotulos";

const MINUTO = 60 * 1000;
const DIA = 24 * 60 * MINUTO;

/** Limites do prazo aceitos pelo back-end: ao menos 5 minutos à frente e no máximo 365 dias. */
function limitesDoPrazo() {
  return { minimo: paraCampoDataHora(Date.now() + 6 * MINUTO), maximo: paraCampoDataHora(Date.now() + 365 * DIA) };
}

/** Prazo sugerido para uma atividade nova: daqui a 7 dias, às 23h59. */
function prazoSugerido() {
  const data = new Date(Date.now() + 7 * DIA);
  data.setHours(23, 59, 0, 0);
  return paraCampoDataHora(data);
}

/**
 * RF15: atividades de uma turma. O professor escolhe questões APROVADAS do banco compartilhado (de
 * qualquer escola), define a ordem e o prazo de entrega e acompanha quem já entregou. Os alunos
 * enviam as respostas uma vez só e veem a correção depois do prazo. Usada na página Atividades (com
 * seletor de turma) e na aba Atividades da tela da Turma.
 */
export function AtividadesDaTurma({ turmaId, podeCriar = true }) {
  // a chave recomeça a seção a cada turma: lista, formulário de nova atividade e erro voltam ao início
  return <AtividadesDaTurmaConteudo key={turmaId} turmaId={turmaId} podeCriar={podeCriar} />;
}

function AtividadesDaTurmaConteudo({ turmaId, podeCriar }) {
  const { avisar } = useInterface();
  const [atividades, setAtividades] = useState(null);
  const [resultado, setResultado] = useState(null);
  const [editandoPrazo, setEditandoPrazo] = useState(null);
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
    listarAtividadesDaTurma(turmaId)
      .then(setAtividades)
      .catch((err) => setErro(err.message));
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
    avisar(
      `Atividade "${atividade.titulo}" criada com ${atividade.totalQuestoes} questão(ões) e prazo ${formatarDataHora(atividade.prazo)}. Os alunos já podem começar.`
    );
    await carregar();
  }

  async function aoAlterarPrazo(atividade) {
    setEditandoPrazo(null);
    avisar(`Novo prazo de "${atividade.titulo}": ${formatarDataHora(atividade.prazo)}.`);
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
          Monte uma atividade com questões aprovadas do banco compartilhado e defina o prazo de entrega.
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
                  <th>Prazo</th>
                  <th className="text-end">Iniciaram</th>
                  <th className="text-end">Entregaram</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {atividades.map((atividade) => (
                  <tr key={atividade.id}>
                    <td>
                      <div className="fw-semibold">{atividade.titulo}</div>
                      <div className="small text-secondary">
                        por {atividade.professorNome} · criada em {formatarDataHora(atividade.criadoEm)}
                      </div>
                    </td>
                    <td className="text-end">{atividade.totalQuestoes}</td>
                    <td className="small">
                      <div>{formatarDataHora(atividade.prazo)}</div>
                      {atividade.prazoEncerrado ? (
                        <span className="badge text-bg-secondary">{atividade.fechada ? "Encerrada e corrigida" : "Encerrada"}</span>
                      ) : (
                        <span className="text-secondary">termina {tempoAtePrazo(atividade.prazo)}</span>
                      )}
                    </td>
                    <td className="text-end">{atividade.alunosQueIniciaram}</td>
                    <td className="text-end text-nowrap">
                      {atividade.alunosQueEntregaram} de {atividade.alunosDaTurma}
                    </td>
                    <td className="text-end text-nowrap">
                      {podeCriar && !atividade.prazoEncerrado && (
                        <button type="button" className="btn btn-sm btn-outline-secondary me-2" onClick={() => setEditandoPrazo(atividade)}>
                          Alterar prazo
                        </button>
                      )}
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

      {editandoPrazo && (
        <AlterarPrazo atividade={editandoPrazo} aoSalvar={aoAlterarPrazo} aoFechar={() => setEditandoPrazo(null)} />
      )}

      {resultado && <Resultados resultado={resultado} aoFechar={() => setResultado(null)} />}
    </>
  );
}

/** Modal para estender ou antecipar o prazo de uma atividade que ainda não terminou. */
function AlterarPrazo({ atividade, aoSalvar, aoFechar }) {
  const [prazo, setPrazo] = useState(paraCampoDataHora(atividade.prazo));
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState(null);
  const { minimo, maximo } = limitesDoPrazo();

  async function salvar(evento) {
    evento.preventDefault();
    setSalvando(true);
    setErro(null);
    try {
      aoSalvar(await alterarPrazo(atividade.id, prazo));
    } catch (err) {
      setErro(err.message);
      setSalvando(false);
    }
  }

  return (
    <Modal
      titulo={`Prazo de "${atividade.titulo}"`}
      aoFechar={aoFechar}
      rodape={
        <>
          <button type="button" className="btn btn-outline-secondary" onClick={aoFechar}>
            Cancelar
          </button>
          <button type="submit" form="form-prazo" className="btn btn-primary" disabled={salvando || !prazo}>
            {salvando ? "Salvando..." : "Salvar prazo"}
          </button>
        </>
      }
    >
      <form id="form-prazo" onSubmit={salvar}>
        <p className="text-secondary small">
          Prazo atual: {formatarDataHora(atividade.prazo)}. Quem ainda não entregou pode responder até o novo prazo; a
          correção aparece para os alunos quando ele terminar.
        </p>
        <label htmlFor="novo-prazo" className="form-label">
          Novo prazo
        </label>
        <input
          id="novo-prazo"
          type="datetime-local"
          className="form-control"
          value={prazo}
          min={minimo}
          max={maximo}
          onChange={(e) => setPrazo(e.target.value)}
          required
        />
        <div className="mt-3">
          <Mensagens erro={erro} />
        </div>
      </form>
    </Modal>
  );
}

/** Resultados para o professor: situação de cada aluno e análise de cada questão. */
function Resultados({ resultado, aoFechar }) {
  const entregaram = resultado.alunos.filter((aluno) => aluno.situacao === "ENTREGUE").length;
  return (
    <Modal titulo={`Resultados: ${resultado.titulo}`} tamanho="xl" aoFechar={aoFechar}>
      <p className="small text-secondary">
        Prazo: {formatarDataHora(resultado.prazo)} ·{" "}
        {resultado.prazoEncerrado
          ? "encerrado: os alunos já veem a correção."
          : `termina ${tempoAtePrazo(resultado.prazo)}. Você já vê os acertos de quem entregou; os alunos, só depois do prazo.`}
      </p>

      <h3 className="h6 mt-3">
        Alunos <span className="text-secondary fw-normal">({entregaram} de {resultado.alunos.length} entregaram)</span>
      </h3>
      {resultado.alunos.length === 0 ? (
        <p className="text-secondary">A turma ainda não tem alunos.</p>
      ) : (
        <div className="table-responsive">
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
                const entregou = aluno.situacao === "ENTREGUE";
                return (
                  <tr key={aluno.alunoId}>
                    <td>{aluno.nome}</td>
                    <td>
                      <span className={`badge text-bg-${situacao?.cor ?? "secondary"}`}>{situacao?.rotulo ?? aluno.situacao}</span>
                    </td>
                    <td className="text-end">
                      {aluno.escolhidas} de {aluno.totalQuestoes}
                      {aluno.situacao === "EM_ANDAMENTO" && <span className="d-block small text-secondary">ainda não enviou</span>}
                    </td>
                    <td className="text-end">{entregou ? `${aluno.acertos} de ${aluno.totalQuestoes}` : "—"}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      <h3 className="h6 mt-4">Questão por questão</h3>
      <p className="small text-secondary">
        Considera só as respostas enviadas. O erro mais comum é a alternativa errada mais escolhida: costuma mostrar o
        raciocínio que a turma precisa rever.
      </p>
      <div className="table-responsive">
        <table className="table table-sm align-middle">
          <thead>
            <tr>
              <th>#</th>
              <th>Questão</th>
              <th style={{ minWidth: "10rem" }}>Acerto</th>
              <th style={{ minWidth: "13rem" }}>Erro mais comum</th>
            </tr>
          </thead>
          <tbody>
            {resultado.analise.map((questao) => (
              <tr key={questao.questaoId}>
                <td className="text-secondary">{questao.ordem}</td>
                <td>
                  <div className="me-texto-limitado">{questao.enunciado}</div>
                  <div className="small text-secondary">
                    {questao.topico} · {rotuloNivel(questao.nivelDificuldade)} · gabarito {questao.letraCorreta}
                  </div>
                </td>
                <td>
                  {questao.respostas === 0 ? (
                    <span className="small text-secondary">sem respostas</span>
                  ) : (
                    <>
                      <BarraPercentual valor={questao.percentualAcerto} />
                      <div className="small text-secondary">
                        {questao.acertos} de {questao.respostas}
                      </div>
                    </>
                  )}
                </td>
                <td className="small">
                  {questao.erroMaisComum ? (
                    <>
                      <span className="fw-semibold">
                        {questao.erroMaisComum.letra}) {questao.erroMaisComum.texto}
                      </span>
                      <div className="text-secondary">
                        {questao.erroMaisComum.vezes} aluno(s) · {formatarPercentual(questao.erroMaisComum.percentualDosErros)} dos erros
                      </div>
                    </>
                  ) : (
                    <span className="text-secondary">—</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

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
  );
}

function NovaAtividade({ turmaId, aoCriar, aoCancelar }) {
  const [titulo, setTitulo] = useState("");
  const [prazo, setPrazo] = useState(prazoSugerido);
  const [filtros, setFiltros] = useState(FILTROS_VAZIOS);
  const [banco, setBanco] = useState(null);
  const [selecionadas, setSelecionadas] = useState([]);
  const [buscando, setBuscando] = useState(true);
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
    listarAprovadas(FILTROS_VAZIOS)
      .then(setBanco)
      .catch((err) => setErro(err.message))
      .finally(() => setBuscando(false));
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
      aoCriar(await criarAtividade(turmaId, titulo, prazo, selecionadas.map((q) => q.id)));
    } catch (err) {
      setErro(err.message);
      setSalvando(false);
    }
  }

  return (
    <div className="card">
      <div className="card-body p-4">
        <h2 className="h5 mb-3">Nova atividade</h2>
        <div className="row g-3 mb-4">
          <div className="col-md-8">
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
          <div className="col-md-4">
            <label htmlFor="prazo-atividade" className="form-label">
              Prazo de entrega
            </label>
            <input
              id="prazo-atividade"
              type="datetime-local"
              className="form-control"
              value={prazo}
              min={limitesDoPrazo().minimo}
              max={limitesDoPrazo().maximo}
              onChange={(e) => setPrazo(e.target.value)}
              aria-describedby="ajuda-prazo"
            />
            <div id="ajuda-prazo" className="form-text">
              Até lá o aluno pode mudar as respostas antes de enviar. A correção aparece depois do prazo.
            </div>
          </div>
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
              disabled={salvando || !titulo.trim() || !prazo || selecionadas.length === 0}
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
        Monte atividades com questões aprovadas do banco compartilhado, defina o prazo e acompanhe as entregas.
      </Cabecalho>
      {turmaId && <AtividadesDaTurma turmaId={turmaId} />}
    </>
  );
}
