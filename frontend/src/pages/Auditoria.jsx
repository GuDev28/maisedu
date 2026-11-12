import { useEffect, useState } from "react";
import { listarAcoes, listarLogs } from "../services/auditoriaService";
import { listarUsuarios } from "../services/gestaoUsuarioService";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { Mensagens } from "../components/Mensagens";
import { ACOES_AUDITORIA, formatarDataHora, rotuloAcao } from "../components/rotulos";

const FILTROS_INICIAIS = { usuarioId: "", acao: "", de: "", ate: "" };

/** RF13: trilha de auditoria da instituição do administrador, com filtros e paginação. */
export default function Auditoria() {
  const [filtros, setFiltros] = useState(FILTROS_INICIAIS);
  const [acoes, setAcoes] = useState(Object.keys(ACOES_AUDITORIA));
  const [usuarios, setUsuarios] = useState([]);
  const [pagina, setPagina] = useState(0);
  const [resultado, setResultado] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState(null);

  async function carregar(numeroPagina, filtrosDaBusca = filtros) {
    setCarregando(true);
    setErro(null);
    try {
      const dados = await listarLogs({
        usuarioId: filtrosDaBusca.usuarioId,
        acao: filtrosDaBusca.acao,
        de: filtrosDaBusca.de ? `${filtrosDaBusca.de}T00:00:00` : "",
        ate: filtrosDaBusca.ate ? `${filtrosDaBusca.ate}T23:59:59` : "",
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
    carregar(0, FILTROS_INICIAIS);
    // a lista de ações vem do back-end: uma ação nova aparece no filtro sem mudar o front
    listarAcoes().then(setAcoes).catch(() => {});
    listarUsuarios().then(setUsuarios).catch(() => {});
    // só na abertura da tela
  }, []);

  function alterar(campo, valor) {
    setFiltros((atual) => ({ ...atual, [campo]: valor }));
  }

  function limpar() {
    setFiltros(FILTROS_INICIAIS);
    carregar(0, FILTROS_INICIAIS);
  }

  const registros = resultado?.content ?? [];
  // Spring Data serializa a página como {content, totalPages} ou {content, page: {totalPages}}
  const totalPaginas = resultado?.totalPages ?? resultado?.page?.totalPages ?? 0;

  return (
    <>
      <Cabecalho titulo="Auditoria">
        Quem fez o quê e quando, só na sua escola. Os registros são guardados pelo prazo da Política de Privacidade.
      </Cabecalho>

      <div className="card mb-3">
        <div className="card-body">
          <form
            className="row g-2 align-items-end"
            onSubmit={(evento) => {
              evento.preventDefault();
              carregar(0);
            }}
          >
            <div className="col-md-3">
              <label htmlFor="filtro-usuario" className="form-label small text-secondary mb-1">
                Usuário
              </label>
              <select id="filtro-usuario" className="form-select" value={filtros.usuarioId} onChange={(e) => alterar("usuarioId", e.target.value)}>
                <option value="">Todos</option>
                {usuarios.map((usuario) => (
                  <option key={usuario.id} value={usuario.id}>
                    {usuario.nome} ({usuario.identificadorDeAcesso})
                  </option>
                ))}
              </select>
            </div>
            <div className="col-md-3">
              <label htmlFor="filtro-acao" className="form-label small text-secondary mb-1">
                Ação
              </label>
              <select id="filtro-acao" className="form-select" value={filtros.acao} onChange={(e) => alterar("acao", e.target.value)}>
                <option value="">Todas</option>
                {acoes.map((acao) => (
                  <option key={acao} value={acao}>
                    {rotuloAcao(acao)}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-6 col-md-2">
              <label htmlFor="filtro-de" className="form-label small text-secondary mb-1">
                De
              </label>
              <input id="filtro-de" type="date" className="form-control" value={filtros.de} onChange={(e) => alterar("de", e.target.value)} />
            </div>
            <div className="col-6 col-md-2">
              <label htmlFor="filtro-ate" className="form-label small text-secondary mb-1">
                Até
              </label>
              <input id="filtro-ate" type="date" className="form-control" value={filtros.ate} onChange={(e) => alterar("ate", e.target.value)} />
            </div>
            <div className="col-md-2 d-flex gap-2">
              <button type="submit" className="btn btn-primary flex-grow-1">
                Filtrar
              </button>
              <button type="button" className="btn btn-outline-secondary" onClick={limpar} title="Limpar filtros">
                Limpar
              </button>
            </div>
          </form>
        </div>
      </div>

      <Mensagens erro={erro} />
      {carregando && <Carregando />}
      {!carregando && registros.length === 0 && !erro && <p className="text-secondary">Nenhum registro encontrado.</p>}
      {!carregando && registros.length > 0 && (
        <>
          <div className="card">
            <div className="table-responsive">
              <table className="table table-sm align-middle mb-0">
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
                      <td className="text-nowrap small">{formatarDataHora(log.criadoEm)}</td>
                      <td>
                        {log.usuarioNome ?? log.identificadorAcesso ?? "—"}
                      </td>
                      <td>{rotuloAcao(log.acao)}</td>
                      <td>
                        {log.sucesso ? (
                          <span className="badge text-bg-success">sucesso</span>
                        ) : (
                          <span className="badge text-bg-danger">falha</span>
                        )}
                      </td>
                      <td className="small">
                        {log.entidade && (
                          <span className="text-secondary">
                            {log.entidade} {log.entidadeId}
                          </span>
                        )}
                        {log.detalhes && <div>{log.detalhes}</div>}
                      </td>
                      <td className="small text-secondary">{log.ip ?? "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          <nav className="d-flex justify-content-between align-items-center mt-3" aria-label="Páginas da auditoria">
            <button type="button" className="btn btn-outline-secondary btn-sm" disabled={pagina === 0} onClick={() => carregar(pagina - 1)}>
              ← Anterior
            </button>
            <span className="small text-secondary">
              Página {pagina + 1} de {Math.max(totalPaginas, 1)}
            </span>
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              disabled={pagina + 1 >= totalPaginas}
              onClick={() => carregar(pagina + 1)}
            >
              Próxima →
            </button>
          </nav>
        </>
      )}
    </>
  );
}
