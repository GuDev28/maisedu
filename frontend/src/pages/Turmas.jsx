import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { useInterface } from "../context/useInterface";
import { cadastrarTurma, desativarTurma, listarMinhasTurmas, listarTurmasParaGestao, reativarTurma } from "../services/turmaService";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { EstadoVazio } from "../components/EstadoVazio";
import { Mensagens } from "../components/Mensagens";
import { Modal } from "../components/Modal";
import { ANOS_ESCOLARES, formatarDataHora, rotuloAno } from "../components/rotulos";

/**
 * Admin (RF06): todas as turmas da escola, com cadastro e ativar/desativar.
 * Professor: as turmas em que leciona ("Turmas e alunos"). Nos dois casos, cada turma abre a tela
 * da Turma, onde ficam pessoas, atividades e desempenho.
 */
export default function Turmas() {
  const { usuario } = useAuth();
  return usuario.roles.includes("ADMIN") ? <TurmasDaEscola /> : <MinhasTurmas />;
}

function TurmasDaEscola() {
  const { avisar, confirmar } = useInterface();
  const [turmas, setTurmas] = useState(null);
  const [cadastrando, setCadastrando] = useState(false);
  const [erro, setErro] = useState(null);

  async function carregar() {
    try {
      setTurmas(await listarTurmasParaGestao());
    } catch (err) {
      setErro(err.message);
    }
  }

  useEffect(() => {
    listarTurmasParaGestao()
      .then(setTurmas)
      .catch((err) => setErro(err.message));
  }, []);

  async function alternar(turma) {
    setErro(null);
    if (turma.ativo) {
      const ok = await confirmar({
        titulo: `Desativar ${turma.nome}?`,
        mensagem: "A turma deixa de aceitar novos vínculos e atividades. O histórico continua guardado e dá para reativar depois.",
        textoConfirmar: "Desativar",
        perigo: true,
      });
      if (!ok) {
        return;
      }
    }
    try {
      if (turma.ativo) {
        await desativarTurma(turma.id);
        avisar(`Turma "${turma.nome}" desativada.`);
      } else {
        await reativarTurma(turma.id);
        avisar(`Turma "${turma.nome}" reativada.`);
      }
      await carregar();
    } catch (err) {
      setErro(err.message);
    }
  }

  async function aoCadastrar(turma) {
    setCadastrando(false);
    avisar(`Turma "${turma.nome}" cadastrada. Abra a turma para vincular os professores.`);
    await carregar();
  }

  return (
    <>
      <Cabecalho
        titulo="Turmas"
        acoes={
          <button type="button" className="btn btn-primary" onClick={() => setCadastrando(true)}>
            Cadastrar turma
          </button>
        }
      >
        Turmas da escola. Abra uma turma para ver e vincular professores e alunos.
      </Cabecalho>
      <Mensagens erro={erro} />
      {!turmas && !erro && <Carregando />}
      {turmas?.length === 0 && (
        <EstadoVazio
          titulo="Nenhuma turma ainda"
          acao={
            <button type="button" className="btn btn-primary" onClick={() => setCadastrando(true)}>
              Cadastrar turma
            </button>
          }
        >
          Comece cadastrando as turmas da escola; depois vincule os professores a cada uma.
        </EstadoVazio>
      )}
      {turmas?.length > 0 && (
        <div className="card">
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead>
                <tr>
                  <th>Turma</th>
                  <th>Ano</th>
                  <th>Situação</th>
                  <th>Criada em</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {turmas.map((turma) => (
                  <tr key={turma.id}>
                    <td>
                      <Link to={`/turmas/${turma.id}`} className="fw-semibold">
                        {turma.nome}
                      </Link>
                    </td>
                    <td>{rotuloAno(turma.anoEscolar)}</td>
                    <td>
                      {turma.ativo ? (
                        <span className="badge text-bg-success">Ativa</span>
                      ) : (
                        <span className="badge text-bg-secondary">Inativa</span>
                      )}
                    </td>
                    <td className="small">{formatarDataHora(turma.criadoEm)}</td>
                    <td className="text-end text-nowrap">
                      <Link to={`/turmas/${turma.id}`} className="btn btn-sm btn-outline-primary me-2">
                        Abrir
                      </Link>
                      <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => alternar(turma)}>
                        {turma.ativo ? "Desativar" : "Reativar"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
      {cadastrando && <CadastroTurma aoCadastrar={aoCadastrar} aoFechar={() => setCadastrando(false)} />}
    </>
  );
}

function CadastroTurma({ aoCadastrar, aoFechar }) {
  const [nome, setNome] = useState("");
  const [anoEscolar, setAnoEscolar] = useState(ANOS_ESCOLARES[0].valor);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState(null);

  async function salvar(evento) {
    evento.preventDefault();
    setErro(null);
    setSalvando(true);
    try {
      aoCadastrar(await cadastrarTurma(nome, anoEscolar));
    } catch (err) {
      setErro(err.message);
      setSalvando(false);
    }
  }

  return (
    <Modal titulo="Cadastrar turma" aoFechar={aoFechar}>
      <form onSubmit={salvar}>
        <Mensagens erro={erro} />
        <div className="mb-3">
          <label htmlFor="nome-turma" className="form-label">
            Nome da turma
          </label>
          <input
            id="nome-turma"
            className="form-control"
            value={nome}
            onChange={(e) => setNome(e.target.value)}
            placeholder="Ex.: 7º ano A"
            maxLength={100}
            autoFocus
            required
          />
        </div>
        <div className="mb-4">
          <label htmlFor="ano-turma" className="form-label">
            Ano escolar
          </label>
          <select id="ano-turma" className="form-select" value={anoEscolar} onChange={(e) => setAnoEscolar(e.target.value)}>
            {ANOS_ESCOLARES.map((ano) => (
              <option key={ano.valor} value={ano.valor}>
                {ano.rotulo}
              </option>
            ))}
          </select>
        </div>
        <div className="d-flex justify-content-end gap-2">
          <button type="button" className="btn btn-outline-secondary" onClick={aoFechar}>
            Cancelar
          </button>
          <button type="submit" className="btn btn-primary" disabled={salvando}>
            {salvando ? "Salvando..." : "Cadastrar"}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function MinhasTurmas() {
  const [turmas, setTurmas] = useState(null);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    listarMinhasTurmas().then(setTurmas).catch((err) => setErro(err.message));
  }, []);

  return (
    <>
      <Cabecalho titulo="Turmas e alunos">
        Turmas em que você leciona. Na turma você cadastra alunos (já vinculados), gera senhas novas e acompanha as
        atividades.
      </Cabecalho>
      <Mensagens erro={erro} />
      {!turmas && !erro && <Carregando />}
      {turmas?.length === 0 && (
        <EstadoVazio titulo="Você ainda não leciona em nenhuma turma">
          O administrador da escola vincula os professores às turmas.
        </EstadoVazio>
      )}
      <div className="row g-3">
        {turmas?.map((turma) => (
          <div key={turma.id} className="col-sm-6 col-lg-4">
            <Link to={`/turmas/${turma.id}`} className="card h-100 text-decoration-none text-reset">
              <div className="card-body">
                <h2 className="h5 mb-1">{turma.nome}</h2>
                <div className="text-secondary small mb-3">{rotuloAno(turma.anoEscolar)}</div>
                <span className="btn btn-sm btn-outline-primary">Abrir turma</span>
              </div>
            </Link>
          </div>
        ))}
      </div>
    </>
  );
}
