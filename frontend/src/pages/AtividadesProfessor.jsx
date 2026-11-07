import { useEffect, useState } from "react";
import { listarMinhasTurmas } from "../services/turmaService";
import { listarAprovadas, listarTopicos } from "../services/questaoService";
import { criarAtividade, listarAtividadesDaTurma, resultadosDaAtividade } from "../services/atividadeService";
import { ANOS_ESCOLARES, NIVEIS, formatarDataHora, rotuloAno, rotuloNivel } from "../components/rotulos";
import { Mensagens } from "../components/Mensagens";

const SITUACOES = { NAO_INICIADA: "Não iniciou", EM_ANDAMENTO: "Em andamento", CONCLUIDA: "Concluída" };

/**
 * RF15: o professor escolhe questões APROVADAS do banco compartilhado (de qualquer escola) e cria
 * atividades para as turmas em que leciona; acompanha quem já respondeu.
 */
export default function AtividadesProfessor() {
  const [turmas, setTurmas] = useState([]);
  const [turmaId, setTurmaId] = useState("");
  const [atividades, setAtividades] = useState([]);
  const [resultado, setResultado] = useState(null);
  const [criando, setCriando] = useState(false);
  const [erro, setErro] = useState(null);
  const [sucesso, setSucesso] = useState(null);

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

  async function carregarAtividades(id) {
    if (!id) {
      return;
    }
    try {
      setAtividades(await listarAtividadesDaTurma(id));
    } catch (err) {
      setErro(err.message);
    }
  }

  useEffect(() => {
    setResultado(null);
    carregarAtividades(turmaId);
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
    setSucesso(`Atividade "${atividade.titulo}" criada com ${atividade.totalQuestoes} questão(ões). Ela já aparece para os alunos da turma.`);
    await carregarAtividades(turmaId);
  }

  if (turmas.length === 0) {
    return (
      <div className="pagina">
        <h2>Atividades</h2>
        <Mensagens erro={erro} />
        <p>Você ainda não está vinculado a nenhuma turma. Peça ao administrador para vinculá-lo.</p>
      </div>
    );
  }

  return (
    <div className="pagina">
      <h2>Atividades</h2>
      <div className="campos">
        <label>
          Turma
          <select className="form-select" value={turmaId} onChange={(e) => setTurmaId(e.target.value)}>
            {turmas.map((turma) => (
              <option key={turma.id} value={turma.id}>{turma.nome} ({rotuloAno(turma.anoEscolar)})</option>
            ))}
          </select>
        </label>
        {!criando && (
          <button className="btn btn-primary" type="button" onClick={() => { setCriando(true); setSucesso(null); }}>Nova atividade</button>
        )}
      </div>

      <Mensagens erro={erro} sucesso={sucesso} />

      {criando && (
        <NovaAtividade turmaId={Number(turmaId)} onCriada={aoCriar} onCancelar={() => setCriando(false)} />
      )}

      {atividades.length === 0 ? (
        <p>Nenhuma atividade criada para esta turma.</p>
      ) : (
        <table className="table table-hover align-middle tabela">
          <thead>
            <tr>
              <th>Atividade</th>
              <th>Questões</th>
              <th>Iniciaram</th>
              <th>Concluíram</th>
              <th>Criada em</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {atividades.map((atividade) => (
              <tr key={atividade.id}>
                <td>{atividade.titulo}<br /><small className="texto-suave">por {atividade.professorNome}</small></td>
                <td>{atividade.totalQuestoes}</td>
                <td>{atividade.alunosQueIniciaram}</td>
                <td>{atividade.alunosQueConcluiram}</td>
                <td>{formatarDataHora(atividade.criadoEm)}</td>
                <td><button className="btn btn-outline-secondary btn-sm" type="button" onClick={() => verResultados(atividade.id)}>Resultados</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {resultado && (
        <div className="cartao" style={{ marginTop: "1rem" }}>
          <h3 style={{ marginTop: 0 }}>Resultados: {resultado.titulo}</h3>
          <table className="table table-hover align-middle tabela">
            <thead>
              <tr><th>Aluno</th><th>Situação</th><th>Respondidas</th><th>Acertos</th></tr>
            </thead>
            <tbody>
              {resultado.alunos.map((aluno) => (
                <tr key={aluno.alunoId}>
                  <td>{aluno.nome}</td>
                  <td>{SITUACOES[aluno.situacao] ?? aluno.situacao}</td>
                  <td>{aluno.respondidas} de {aluno.totalQuestoes}</td>
                  <td>{aluno.acertos}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <details style={{ marginTop: "0.75rem" }}>
            <summary>Questões da atividade (com gabarito)</summary>
            <ol>
              {resultado.questoes.map((questao) => (
                <li key={questao.id}>
                  {questao.enunciado}{" "}
                  <small className="texto-suave">
                    ({questao.topico}, {rotuloNivel(questao.nivelDificuldade)}) — resposta:{" "}
                    {questao.alternativas.find((a) => a.correta)?.letra}
                  </small>
                </li>
              ))}
            </ol>
          </details>
          <button className="btn btn-outline-secondary btn-sm" type="button" onClick={() => setResultado(null)}>Fechar</button>
        </div>
      )}
    </div>
  );
}

function NovaAtividade({ turmaId, onCriada, onCancelar }) {
  const [titulo, setTitulo] = useState("");
  const [topicos, setTopicos] = useState([]);
  const [filtros, setFiltros] = useState({ topicoId: "", nivel: "", anoEscolar: "" });
  const [banco, setBanco] = useState([]);
  const [selecionadas, setSelecionadas] = useState([]);
  const [buscando, setBuscando] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    listarTopicos().then(setTopicos).catch((err) => setErro(err.message));
  }, []);

  async function buscar(event) {
    event?.preventDefault();
    setBuscando(true);
    setErro(null);
    try {
      setBanco(await listarAprovadas(filtros));
    } catch (err) {
      setErro(err.message);
    } finally {
      setBuscando(false);
    }
  }

  useEffect(() => {
    buscar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
      const atividade = await criarAtividade(turmaId, titulo, selecionadas.map((q) => q.id));
      onCriada(atividade);
    } catch (err) {
      setErro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <div className="cartao">
      <h3 style={{ marginTop: 0 }}>Nova atividade</h3>
      <div className="campos">
        <label style={{ flex: 1 }}>
          Título
          <input className="form-control" value={titulo} onChange={(e) => setTitulo(e.target.value)} maxLength={150}
                 placeholder="Ex.: Revisão de frações" />
        </label>
      </div>

      <h4>Banco compartilhado (somente questões aprovadas)</h4>
      <form className="campos" onSubmit={buscar}>
        <label>
          Tópico
          <select className="form-select" value={filtros.topicoId} onChange={(e) => setFiltros({ ...filtros, topicoId: e.target.value })}>
            <option value="">Todos</option>
            {topicos.map((t) => <option key={t.id} value={t.id}>{t.nome}</option>)}
          </select>
        </label>
        <label>
          Nível
          <select className="form-select" value={filtros.nivel} onChange={(e) => setFiltros({ ...filtros, nivel: e.target.value })}>
            <option value="">Todos</option>
            {NIVEIS.map((n) => <option key={n.valor} value={n.valor}>{n.rotulo}</option>)}
          </select>
        </label>
        <label>
          Ano
          <select className="form-select" value={filtros.anoEscolar} onChange={(e) => setFiltros({ ...filtros, anoEscolar: e.target.value })}>
            <option value="">Todos</option>
            {ANOS_ESCOLARES.map((a) => <option key={a.valor} value={a.valor}>{a.rotulo}</option>)}
          </select>
        </label>
        <button className="btn btn-primary" type="submit" disabled={buscando}>{buscando ? "Buscando..." : "Buscar"}</button>
      </form>

      <div style={{ maxHeight: 280, overflowY: "auto", border: "1px solid var(--border)", borderRadius: 6 }}>
        {banco.length === 0 ? (
          <p style={{ padding: "0.5rem" }}>Nenhuma questão aprovada com esses filtros.</p>
        ) : (
          <table className="table table-hover align-middle tabela">
            <tbody>
              {banco.map((questao) => (
                <tr key={questao.id}>
                  <td style={{ width: 28 }}>
                    <input className="form-check-input" type="checkbox" aria-label={`Selecionar questão ${questao.id}`}
                           checked={selecionadas.some((q) => q.id === questao.id)} onChange={() => alternar(questao)} />
                  </td>
                  <td>
                    {questao.enunciado}
                    <br />
                    <small className="texto-suave">
                      {questao.topico} · {rotuloNivel(questao.nivelDificuldade)} · {rotuloAno(questao.anoEscolar)} · autor(a): {questao.autorNome}
                    </small>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <h4>Ordem na atividade ({selecionadas.length})</h4>
      {selecionadas.length === 0 ? (
        <p className="texto-suave">Marque as questões acima.</p>
      ) : (
        <ol>
          {selecionadas.map((questao, indice) => (
            <li key={questao.id}>
              {questao.enunciado}{" "}
              <button className="btn btn-outline-secondary btn-sm" type="button" onClick={() => mover(indice, -1)} disabled={indice === 0} aria-label="Subir">↑</button>
              <button className="btn btn-outline-secondary btn-sm" type="button" onClick={() => mover(indice, 1)} disabled={indice === selecionadas.length - 1}
                      aria-label="Descer">↓</button>
            </li>
          ))}
        </ol>
      )}

      <Mensagens erro={erro} />
      <div className="linha-acoes">
        <button className="btn btn-primary" type="button" onClick={salvar} disabled={salvando || !titulo.trim() || selecionadas.length === 0}>
          {salvando ? "Criando..." : "Criar atividade"}
        </button>
        <button className="btn btn-outline-secondary btn-sm" type="button" onClick={onCancelar}>Cancelar</button>
      </div>
    </div>
  );
}
