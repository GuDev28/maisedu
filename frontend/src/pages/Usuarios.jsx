import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { useInterface } from "../context/InterfaceContext";
import {
  anonimizarUsuario,
  corrigirNome,
  desativarUsuario,
  exportarDadosPessoais,
  listarUsuarios,
  reativarUsuario,
  redefinirSenha,
} from "../services/gestaoUsuarioService";
import { listarMinhasTurmas } from "../services/turmaService";
import { cadastrarProfessor } from "../services/usuarioService";
import { baixarJson } from "../services/http";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { Mensagens } from "../components/Mensagens";
import { Modal } from "../components/Modal";
import { SenhaProvisoria } from "../components/SenhaProvisoria";
import { EmailDoResponsavel } from "../components/EmailDoResponsavel";
import { PAPEIS, rotuloAno } from "../components/rotulos";

/**
 * Admin: usuários da escola, cadastro de professor (já podendo escolher a turma) e os controles
 * da LGPD (art. 18): exportar, corrigir, desativar e anonimizar.
 */
export default function Usuarios() {
  const { usuario: eu } = useAuth();
  const { avisar, confirmar } = useInterface();
  const [usuarios, setUsuarios] = useState(null);
  const [papel, setPapel] = useState("");
  const [busca, setBusca] = useState("");
  const [erro, setErro] = useState(null);
  const [senhaGerada, setSenhaGerada] = useState(null);
  const [editando, setEditando] = useState(null);
  const [editandoEmail, setEditandoEmail] = useState(null);
  const [cadastrando, setCadastrando] = useState(false);

  async function carregar(papelFiltro = papel) {
    try {
      setUsuarios(await listarUsuarios(papelFiltro));
    } catch (err) {
      setErro(err.message);
    }
  }

  useEffect(() => {
    carregar(papel);
    // recarrega quando o filtro de papel muda
  }, [papel]);

  async function executar(acao, mensagem) {
    setErro(null);
    try {
      const resultado = await acao();
      if (mensagem) {
        avisar(mensagem);
      }
      await carregar();
      return resultado;
    } catch (err) {
      setErro(err.message);
      return null;
    }
  }

  async function handleRedefinir(alvo) {
    const ehAluno = alvo.roles.includes("ALUNO");
    let destino = `A senha nova vai por e-mail para ${alvo.identificadorDeAcesso}.`;
    if (ehAluno) {
      destino = alvo.emailResponsavel
        ? `A senha nova vai para o e-mail do responsável (${alvo.emailResponsavel}).`
        : "Este aluno não tem e-mail do responsável cadastrado: a senha nova aparece aqui para você entregar.";
    }
    const ok = await confirmar({
      titulo: `Gerar uma senha nova para ${alvo.nome}?`,
      mensagem: `${destino} A senha atual deixa de valer e as sessões abertas dessa conta são encerradas.`,
      textoConfirmar: "Gerar senha",
    });
    if (!ok) {
      return;
    }
    const resultado = await executar(() => redefinirSenha(alvo.id));
    if (resultado) {
      if (resultado.enviadaPorEmail && ehAluno) {
        avisar(`Senha nova de ${alvo.nome} enviada para o e-mail do responsável.`);
      } else if (resultado.enviadaPorEmail) {
        avisar(`Senha provisória enviada por e-mail para ${resultado.identificadorDeAcesso}.`);
      } else {
        setSenhaGerada(resultado);
      }
    }
  }

  async function handleDesativar(alvo) {
    const ok = await confirmar({
      titulo: `Desativar a conta de ${alvo.nome}?`,
      mensagem: "A pessoa deixa de conseguir entrar e as sessões abertas são encerradas. Dá para reativar depois.",
      textoConfirmar: "Desativar",
      perigo: true,
    });
    if (ok) {
      await executar(() => desativarUsuario(alvo.id), "Conta desativada e sessões encerradas.");
    }
  }

  async function handleExportar(alvo) {
    const dados = await executar(() => exportarDadosPessoais(alvo.id), "Exportação gerada (arquivo JSON).");
    if (dados) {
      baixarJson(dados, `maisedu-dados-usuario-${alvo.id}.json`);
    }
  }

  async function handleAnonimizar(alvo) {
    const ok = await confirmar({
      titulo: `Anonimizar ${alvo.nome}?`,
      mensagem:
        "Nome, e-mail/login e vínculos com turmas serão apagados e a conta será desativada. Respostas e desempenho ficam só para estatística, sem identificar a pessoa. Isto não pode ser desfeito.",
      textoConfirmar: "Anonimizar",
      perigo: true,
      digitarParaConfirmar: "ANONIMIZAR",
    });
    if (ok) {
      await executar(() => anonimizarUsuario(alvo.id), "Conta anonimizada.");
    }
  }

  async function salvarNome(alvo, novoNome) {
    const ok = await executar(() => corrigirNome(alvo.id, novoNome), "Nome corrigido.");
    if (ok !== null) {
      setEditando(null);
    }
  }

  const termo = busca.trim().toLowerCase();
  const filtrados = (usuarios ?? []).filter(
    (u) => !termo || u.nome.toLowerCase().includes(termo) || (u.identificadorDeAcesso ?? "").toLowerCase().includes(termo)
  );

  return (
    <>
      <Cabecalho
        titulo="Usuários"
        acoes={
          <button type="button" className="btn btn-primary" onClick={() => setCadastrando(true)}>
            Cadastrar professor
          </button>
        }
      >
        Contas da escola. Aqui também são atendidos os direitos do titular (LGPD, art. 18): exportar, corrigir,
        desativar e anonimizar. Alunos são cadastrados pelos professores, na tela da turma.
      </Cabecalho>

      <div className="row g-2 mb-3">
        <div className="col-sm-6 col-md-4">
          <input
            type="search"
            className="form-control"
            placeholder="Buscar por nome ou login"
            aria-label="Buscar usuário"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
          />
        </div>
        <div className="col-sm-6 col-md-3">
          <select className="form-select" aria-label="Filtrar por papel" value={papel} onChange={(e) => setPapel(e.target.value)}>
            <option value="">Todos os papéis</option>
            <option value="PROFESSOR">Professores</option>
            <option value="ALUNO">Alunos</option>
            <option value="ADMIN">Administradores</option>
          </select>
        </div>
      </div>

      <Mensagens erro={erro} />
      {!usuarios && !erro && <Carregando />}
      {usuarios && filtrados.length === 0 && <p className="text-secondary">Nenhum usuário encontrado.</p>}
      {filtrados.length > 0 && (
        <div className="card">
          <div className="table-responsive me-tabela-com-menu">
            <table className="table align-middle mb-0">
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>Acesso</th>
                  <th>Papéis</th>
                  <th>Situação</th>
                  <th className="text-end">Ações</th>
                </tr>
              </thead>
              <tbody>
                {filtrados.map((alvo) => (
                  <tr key={alvo.id}>
                    <td>
                      {editando === alvo.id ? (
                        <EdicaoNome alvo={alvo} aoSalvar={salvarNome} aoCancelar={() => setEditando(null)} />
                      ) : (
                        alvo.nome
                      )}
                    </td>
                    <td className="small">
                      <code>{alvo.identificadorDeAcesso}</code>
                      {alvo.emailResponsavel && <div className="text-secondary">responsável: {alvo.emailResponsavel}</div>}
                    </td>
                    <td className="small">{alvo.roles.map((r) => PAPEIS[r] ?? r).join(", ")}</td>
                    <td>
                      {alvo.anonimizado ? (
                        <span className="badge text-bg-dark">anonimizada</span>
                      ) : alvo.ativo ? (
                        <span className="badge text-bg-success">ativa</span>
                      ) : (
                        <span className="badge text-bg-secondary">desativada</span>
                      )}
                      {alvo.bloqueado && <span className="badge text-bg-danger ms-1">bloqueada</span>}
                      {alvo.senhaTemporaria && !alvo.anonimizado && (
                        <span className="badge text-bg-light border ms-1">1º acesso pendente</span>
                      )}
                    </td>
                    <td className="text-end">
                      {!alvo.anonimizado && alvo.id !== eu.id && (
                        <div className="dropdown-center d-inline-block">
                          <MenuAcoes
                            alvo={alvo}
                            aoRedefinir={() => handleRedefinir(alvo)}
                            aoCorrigir={() => setEditando(alvo.id)}
                            aoEmailResponsavel={alvo.roles.includes("ALUNO") ? () => setEditandoEmail(alvo) : null}
                            aoAlternar={() =>
                              alvo.ativo ? handleDesativar(alvo) : executar(() => reativarUsuario(alvo.id), "Conta reativada.")
                            }
                            aoExportar={() => handleExportar(alvo)}
                            aoAnonimizar={!alvo.roles.includes("ADMIN") ? () => handleAnonimizar(alvo) : null}
                          />
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {senhaGerada && <SenhaProvisoria dados={senhaGerada} aoFechar={() => setSenhaGerada(null)} />}
      {editandoEmail && (
        <EmailDoResponsavel
          aluno={editandoEmail}
          aoFechar={() => setEditandoEmail(null)}
          aoSalvar={async (mensagem) => {
            setEditandoEmail(null);
            avisar(mensagem);
            await carregar();
          }}
        />
      )}
      {cadastrando && (
        <CadastroProfessor
          aoFechar={() => setCadastrando(false)}
          aoCadastrar={async (professor) => {
            setCadastrando(false);
            avisar(`${professor.nome} cadastrado(a). A senha provisória foi enviada para ${professor.identificadorDeAcesso}.`);
            await carregar();
          }}
        />
      )}
    </>
  );
}

function EdicaoNome({ alvo, aoSalvar, aoCancelar }) {
  const [nome, setNome] = useState(alvo.nome);
  return (
    <form
      className="d-flex gap-1"
      onSubmit={(evento) => {
        evento.preventDefault();
        aoSalvar(alvo, nome);
      }}
    >
      <input className="form-control form-control-sm" value={nome} onChange={(e) => setNome(e.target.value)} maxLength={255} aria-label="Nome" autoFocus />
      <button type="submit" className="btn btn-sm btn-primary">
        Salvar
      </button>
      <button type="button" className="btn btn-sm btn-outline-secondary" onClick={aoCancelar}>
        Cancelar
      </button>
    </form>
  );
}

/** Ações da linha num menu suspenso controlado pelo React (sem o JavaScript do Bootstrap). */
function MenuAcoes({ alvo, aoRedefinir, aoCorrigir, aoEmailResponsavel, aoAlternar, aoExportar, aoAnonimizar }) {
  const [aberto, setAberto] = useState(false);

  useEffect(() => {
    if (!aberto) {
      return undefined;
    }
    function fechar() {
      setAberto(false);
    }
    document.addEventListener("click", fechar);
    return () => document.removeEventListener("click", fechar);
  }, [aberto]);

  function item(rotulo, acao, classe = "") {
    return (
      <li>
        <button type="button" className={`dropdown-item ${classe}`} onClick={acao}>
          {rotulo}
        </button>
      </li>
    );
  }

  return (
    <>
      <button
        type="button"
        className="btn btn-sm btn-outline-secondary dropdown-toggle"
        aria-expanded={aberto}
        onClick={(evento) => {
          evento.stopPropagation();
          setAberto(!aberto);
        }}
      >
        Ações
      </button>
      <ul className={`dropdown-menu dropdown-menu-end ${aberto ? "show" : ""}`} data-bs-popper="static">
        {item("Gerar senha nova", aoRedefinir)}
        {item("Corrigir nome", aoCorrigir)}
        {aoEmailResponsavel && item("E-mail do responsável", aoEmailResponsavel)}
        {item(alvo.ativo ? "Desativar conta" : "Reativar conta", aoAlternar)}
        {item("Exportar dados (JSON)", aoExportar)}
        {aoAnonimizar && (
          <>
            <li>
              <hr className="dropdown-divider" />
            </li>
            {item("Anonimizar (LGPD)", aoAnonimizar, "text-danger")}
          </>
        )}
      </ul>
    </>
  );
}

function CadastroProfessor({ aoCadastrar, aoFechar }) {
  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [turmaId, setTurmaId] = useState("");
  const [turmas, setTurmas] = useState([]);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    listarMinhasTurmas().then(setTurmas).catch(() => setTurmas([]));
  }, []);

  async function salvar(evento) {
    evento.preventDefault();
    setErro(null);
    setSalvando(true);
    try {
      await aoCadastrar(await cadastrarProfessor(nome, email, turmaId ? Number(turmaId) : null));
    } catch (err) {
      setErro(err.message);
      setSalvando(false);
    }
  }

  return (
    <Modal titulo="Cadastrar professor" aoFechar={aoFechar}>
      <form onSubmit={salvar}>
        <Mensagens erro={erro} />
        <div className="mb-3">
          <label htmlFor="nome-professor" className="form-label">
            Nome completo
          </label>
          <input id="nome-professor" className="form-control" value={nome} onChange={(e) => setNome(e.target.value)} maxLength={255} autoFocus required />
        </div>
        <div className="mb-3">
          <label htmlFor="email-professor" className="form-label">
            E-mail
          </label>
          <input id="email-professor" type="email" className="form-control" value={email} onChange={(e) => setEmail(e.target.value)} required />
          <div className="form-text">A senha provisória vai para este e-mail.</div>
        </div>
        <div className="mb-4">
          <label htmlFor="turma-professor" className="form-label">
            Turma <span className="text-secondary">(opcional)</span>
          </label>
          <select id="turma-professor" className="form-select" value={turmaId} onChange={(e) => setTurmaId(e.target.value)}>
            <option value="">Vincular depois</option>
            {turmas.map((turma) => (
              <option key={turma.id} value={turma.id}>
                {turma.nome} · {rotuloAno(turma.anoEscolar)}
              </option>
            ))}
          </select>
          <div className="form-text">Com a turma escolhida, o professor já sai vinculado a ela.</div>
        </div>
        <div className="d-flex justify-content-end gap-2">
          <button type="button" className="btn btn-outline-secondary" onClick={aoFechar}>
            Cancelar
          </button>
          <button type="submit" className="btn btn-primary" disabled={salvando}>
            {salvando ? "Cadastrando..." : "Cadastrar"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
