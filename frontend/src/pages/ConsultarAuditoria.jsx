import { useEffect, useState } from "react";
import { listarLogs } from "../services/auditoriaService";
import { formatarDataHora } from "../components/rotulos";
import { Mensagens } from "../components/Mensagens";

const ACOES = [
  "LOGIN", "LOGOUT", "TROCA_SENHA", "ACEITE_TERMOS", "CADASTRO_PROFESSOR", "CADASTRO_ALUNO", "CADASTRO_TURMA",
  "ALTERACAO_TURMA", "VINCULO_ALUNO_TURMA", "VINCULO_PROFESSOR_TURMA", "CRIACAO_QUESTAO", "ENVIO_VALIDACAO_QUESTAO",
  "AVALIACAO_QUESTAO", "CRIACAO_ATIVIDADE", "INICIO_ATIVIDADE", "GERACAO_PRATICA_ADAPTATIVA", "ENVIO_EMAIL",
  "REDEFINICAO_SENHA", "CORRECAO_DADOS_USUARIO", "DESATIVACAO_USUARIO", "REATIVACAO_USUARIO",
  "EXPORTACAO_DADOS_USUARIO", "ANONIMIZACAO_USUARIO",
];

/** RF13: trilha de auditoria da instituição do administrador, com filtros e paginação. */
export default function ConsultarAuditoria() {
  const [filtros, setFiltros] = useState({ usuarioId: "", acao: "", de: "", ate: "" });
  const [pagina, setPagina] = useState(0);
  const [resultado, setResultado] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState(null);

  async function carregar(numeroPagina) {
    setCarregando(true);
    setErro(null);
    try {
      const dados = await listarLogs({
        usuarioId: filtros.usuarioId,
        acao: filtros.acao,
        de: filtros.de ? `${filtros.de}:00` : "",
        ate: filtros.ate ? `${filtros.ate}:59` : "",
        pagina: numeroPagina,
      });
      setResultado(dados);
      setPagina(numeroPagina);
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    carregar(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function alterar(campo, valor) {
    setFiltros((atual) => ({ ...atual, [campo]: valor }));
  }

  const registros = resultado?.content ?? [];
  const totalPaginas = resultado?.totalPages ?? 0;

  return (
    <div className="pagina" style={{ maxWidth: 1080 }}>
      <h2>Auditoria</h2>
      <p className="texto-suave">Somente registros da sua instituição. Mantidos pelo prazo de retenção da Política de Privacidade.</p>

      <form className="campos" onSubmit={(e) => { e.preventDefault(); carregar(0); }}>
        <label>
          Id do usuário
          <input type="number" min="1" value={filtros.usuarioId} onChange={(e) => alterar("usuarioId", e.target.value)}
                 style={{ width: 110 }} />
        </label>
        <label>
          Ação
          <select value={filtros.acao} onChange={(e) => alterar("acao", e.target.value)}>
            <option value="">Todas</option>
            {ACOES.map((acao) => <option key={acao} value={acao}>{acao}</option>)}
          </select>
        </label>
        <label>
          De
          <input type="datetime-local" value={filtros.de} onChange={(e) => alterar("de", e.target.value)} />
        </label>
        <label>
          Até
          <input type="datetime-local" value={filtros.ate} onChange={(e) => alterar("ate", e.target.value)} />
        </label>
        <button type="submit">Filtrar</button>
      </form>

      <Mensagens erro={erro} />

      {carregando ? (
        <p>Carregando...</p>
      ) : registros.length === 0 ? (
        <p>Nenhum registro encontrado.</p>
      ) : (
        <>
          <table className="tabela">
            <thead>
              <tr>
                <th>Data e hora</th>
                <th>Usuário</th>
                <th>Ação</th>
                <th>Resultado</th>
                <th>Detalhes</th>
                <th>IP</th>
              </tr>
            </thead>
            <tbody>
              {registros.map((log) => (
                <tr key={log.id}>
                  <td>{formatarDataHora(log.criadoEm)}</td>
                  <td>
                    {log.usuarioNome ?? "—"}
                    {log.usuarioId && <small className="texto-suave"> #{log.usuarioId}</small>}
                  </td>
                  <td><code>{log.acao}</code></td>
                  <td>{log.sucesso ? "Sucesso" : <span className="etiqueta etiqueta-alerta">falha</span>}</td>
                  <td>
                    {log.entidade && <small>{log.entidade} {log.entidadeId}</small>}
                    {log.detalhes && <div><small>{log.detalhes}</small></div>}
                  </td>
                  <td><small>{log.ip ?? "—"}</small></td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="linha-acoes">
            <button type="button" disabled={pagina === 0} onClick={() => carregar(pagina - 1)}>Anterior</button>
            <span>Página {pagina + 1} de {Math.max(totalPaginas, 1)}</span>
            <button type="button" disabled={pagina + 1 >= totalPaginas} onClick={() => carregar(pagina + 1)}>Próxima</button>
          </div>
        </>
      )}
    </div>
  );
}
