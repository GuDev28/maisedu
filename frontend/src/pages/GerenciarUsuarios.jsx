import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import {
  anonimizarUsuario,
  corrigirNome,
  desativarUsuario,
  exportarDadosPessoais,
  listarUsuarios,
  reativarUsuario,
  redefinirSenha,
} from "../services/gestaoUsuarioService";
import { baixarJson } from "../services/http";
import { PAPEIS } from "../components/rotulos";
import { Mensagens } from "../components/Mensagens";

/**
 * Admin: usuários da instituição, com redefinição de senha e os controles da LGPD (corrigir,
 * desativar, exportar e anonimizar). Professor: alunos das suas turmas, só com redefinição de senha.
 */
export default function GerenciarUsuarios() {
  const { usuario: eu } = useAuth();
  const ehAdmin = eu.roles.includes("ADMIN");
  const [usuarios, setUsuarios] = useState([]);
  const [papel, setPapel] = useState("");
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState(null);
  const [sucesso, setSucesso] = useState(null);
  const [senhaGerada, setSenhaGerada] = useState(null);
  const [editando, setEditando] = useState(null);
  const [novoNome, setNovoNome] = useState("");

  async function carregar() {
    try {
      setUsuarios(await listarUsuarios(ehAdmin ? papel : undefined));
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    carregar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [papel]);

  async function executar(acao, mensagem) {
    setErro(null);
    setSucesso(null);
    setSenhaGerada(null);
    try {
      const resultado = await acao();
      if (mensagem) {
        setSucesso(typeof mensagem === "function" ? mensagem(resultado) : mensagem);
      }
      await carregar();
      return resultado;
    } catch (err) {
      setErro(err.message);
      return null;
    }
  }

  async function handleRedefinir(alvo) {
    if (!window.confirm(`Gerar uma nova senha provisória para ${alvo.nome}? As sessões abertas dessa conta serão encerradas.`)) {
      return;
    }
    const resultado = await executar(() => redefinirSenha(alvo.id));
    if (resultado) {
      if (resultado.enviadaPorEmail) {
        setSucesso(`Nova senha provisória enviada por e-mail para ${resultado.identificadorDeAcesso}.`);
      } else {
        setSenhaGerada(resultado);
      }
    }
  }

  async function handleExportar(alvo) {
    const dados = await executar(() => exportarDadosPessoais(alvo.id), "Exportação gerada (arquivo JSON).");
    if (dados) {
      baixarJson(dados, `maisedu-dados-usuario-${alvo.id}.json`);
    }
  }

  async function handleAnonimizar(alvo) {
    const confirmacao = window.prompt(
      `Anonimizar ${alvo.nome}? Nome, e-mail/login e vínculos com turmas serão apagados e a conta será ` +
        `desativada. Isto NÃO pode ser desfeito. Digite ANONIMIZAR para confirmar.`
    );
    if (confirmacao !== "ANONIMIZAR") {
      return;
    }
    await executar(() => anonimizarUsuario(alvo.id), "Conta anonimizada.");
  }

  async function salvarNome(alvo) {
    const ok = await executar(() => corrigirNome(alvo.id, novoNome), "Nome corrigido.");
    if (ok) {
      setEditando(null);
    }
  }

  return (
    <div className="pagina">
      <h2>{ehAdmin ? "Usuários da instituição" : "Meus alunos"}</h2>
      {ehAdmin ? (
        <p className="texto-suave">
          Aqui a instituição atende os direitos do titular previstos na Política de Privacidade (LGPD, art. 18):
          acesso e portabilidade (exportar), correção, bloqueio (desativar) e eliminação (anonimizar).
        </p>
      ) : (
        <p className="texto-suave">Alunos das turmas em que você leciona. Use para gerar uma nova senha provisória.</p>
      )}

      {ehAdmin && (
        <div className="campos">
          <label>
            Papel
            <select value={papel} onChange={(e) => setPapel(e.target.value)}>
              <option value="">Todos</option>
              <option value="PROFESSOR">Professores</option>
              <option value="ALUNO">Alunos</option>
              <option value="ADMIN">Administradores</option>
            </select>
          </label>
        </div>
      )}

      <Mensagens erro={erro} sucesso={sucesso} />
      {senhaGerada && (
        <div className="cartao cartao-selecionado" role="status">
          Nova senha provisória de <strong>{senhaGerada.nome}</strong> (login <code>{senhaGerada.identificadorDeAcesso}</code>):{" "}
          <code style={{ fontSize: "1.2rem" }}>{senhaGerada.senhaProvisoria}</code>
          <br />
          <small>Repasse ao aluno. Ela será trocada no próximo acesso e não será exibida novamente.</small>
        </div>
      )}

      {carregando ? (
        <p>Carregando...</p>
      ) : usuarios.length === 0 ? (
        <p>Nenhum usuário encontrado.</p>
      ) : (
        <table className="tabela">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Acesso</th>
              <th>Papéis</th>
              <th>Situação</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {usuarios.map((alvo) => (
              <tr key={alvo.id}>
                <td>
                  {editando === alvo.id ? (
                    <span className="linha-acoes">
                      <input value={novoNome} onChange={(e) => setNovoNome(e.target.value)} maxLength={255} />
                      <button type="button" onClick={() => salvarNome(alvo)}>Salvar</button>
                      <button type="button" onClick={() => setEditando(null)}>Cancelar</button>
                    </span>
                  ) : (
                    alvo.nome
                  )}
                </td>
                <td><code>{alvo.identificadorDeAcesso}</code></td>
                <td>{alvo.roles.map((r) => PAPEIS[r] ?? r).join(", ")}</td>
                <td>
                  {alvo.anonimizado ? "Anonimizada" : alvo.ativo ? "Ativa" : "Desativada"}
                  {alvo.bloqueado && <span className="etiqueta etiqueta-alerta">bloqueada</span>}
                  {alvo.senhaTemporaria && !alvo.anonimizado && (
                    <span className="etiqueta etiqueta-neutra">1º acesso pendente</span>
                  )}
                </td>
                <td>
                  {!alvo.anonimizado && alvo.id !== eu.id && (
                    <span className="linha-acoes">
                      <button type="button" onClick={() => handleRedefinir(alvo)}>Nova senha</button>
                      {ehAdmin && (
                        <>
                          <button type="button" onClick={() => { setEditando(alvo.id); setNovoNome(alvo.nome); }}>
                            Corrigir nome
                          </button>
                          {alvo.ativo ? (
                            <button type="button" onClick={() => executar(() => desativarUsuario(alvo.id), "Conta desativada e sessões encerradas.")}>
                              Desativar
                            </button>
                          ) : (
                            <button type="button" onClick={() => executar(() => reativarUsuario(alvo.id), "Conta reativada.")}>
                              Reativar
                            </button>
                          )}
                          <button type="button" onClick={() => handleExportar(alvo)}>Exportar dados</button>
                          {!alvo.roles.includes("ADMIN") && (
                            <button type="button" onClick={() => handleAnonimizar(alvo)}>Anonimizar</button>
                          )}
                        </>
                      )}
                    </span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
