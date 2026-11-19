import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useInterface } from "../context/InterfaceContext";
import { cadastrarAluno, cadastrarProfessor } from "../services/usuarioService";
import { redefinirSenha } from "../services/gestaoUsuarioService";
import {
  desvincularAluno,
  desvincularProfessor,
  listarAlunosDisponiveis,
  listarMembros,
  listarProfessoresDisponiveis,
  vincularAluno,
  vincularProfessor,
} from "../services/vinculoTurmaService";
import { Carregando } from "../components/Carregando";
import { Mensagens } from "../components/Mensagens";
import { Modal } from "../components/Modal";
import { SenhaProvisoria } from "../components/SenhaProvisoria";
import { EmailDoResponsavel } from "../components/EmailDoResponsavel";
import { SeloEmailAcesso, resultadoDoEnvio } from "../components/SeloEmailAcesso";
import { rotuloAno } from "../components/rotulos";
import { AtividadesDaTurma } from "./Atividades";
import { DesempenhoDaTurma } from "./Desempenho";

/**
 * Tela da Turma: junta o que antes eram quatro itens de menu (cadastrar aluno, vincular aluno,
 * vincular professor, meus alunos). Abas: Pessoas, Atividades e Desempenho. Quem pode mexer em
 * cada coisa é decidido no back-end (RN01); a tela só esconde o que o perfil não usa.
 */
export default function Turma() {
  const { turmaId, aba = "pessoas" } = useParams();
  const { usuario } = useAuth();
  const [turma, setTurma] = useState(null);
  const [erro, setErro] = useState(null);

  const carregar = useCallback(async () => {
    try {
      setTurma(await listarMembros(turmaId));
    } catch (err) {
      setErro(err.message);
    }
  }, [turmaId]);

  useEffect(() => {
    carregar();
  }, [carregar]);

  if (erro && !turma) {
    return (
      <>
        <Mensagens erro={erro} />
        <Link to="/turmas">← Voltar para as turmas</Link>
      </>
    );
  }
  if (!turma) {
    return <Carregando />;
  }

  const ehAdmin = usuario.roles.includes("ADMIN");
  const leciona = turma.professores.some((professor) => professor.id === usuario.id);
  const abas = [
    { chave: "pessoas", rotulo: `Pessoas (${turma.professores.length + turma.alunos.length})` },
    { chave: "atividades", rotulo: "Atividades" },
    { chave: "desempenho", rotulo: "Desempenho" },
  ];

  return (
    <>
      <Link to="/turmas" className="small">
        ← {ehAdmin ? "Turmas" : "Turmas e alunos"}
      </Link>
      <div className="d-flex flex-wrap align-items-center gap-2 mt-2 mb-3">
        <h1 className="me-titulo mb-0">{turma.nome}</h1>
        <span className="badge text-bg-light border">{rotuloAno(turma.anoEscolar)}</span>
        {!turma.ativo && <span className="badge text-bg-secondary">Inativa</span>}
      </div>
      {!turma.ativo && (
        <div className="alert alert-secondary py-2">Turma inativa: não aceita novos vínculos nem atividades.</div>
      )}

      <ul className="nav nav-tabs mb-4">
        {abas.map((item) => (
          <li key={item.chave} className="nav-item">
            <Link
              to={item.chave === "pessoas" ? `/turmas/${turmaId}` : `/turmas/${turmaId}/${item.chave}`}
              className={`nav-link ${aba === item.chave ? "active" : ""}`}
              aria-current={aba === item.chave ? "page" : undefined}
            >
              {item.rotulo}
            </Link>
          </li>
        ))}
      </ul>

      {aba === "pessoas" && <AbaPessoas turma={turma} ehAdmin={ehAdmin} aoMudar={carregar} />}
      {aba === "atividades" && <AtividadesDaTurma turmaId={Number(turmaId)} podeCriar={leciona && turma.ativo} />}
      {aba === "desempenho" && <DesempenhoDaTurma turmaId={Number(turmaId)} />}
    </>
  );
}

function AbaPessoas({ turma, ehAdmin, aoMudar }) {
  const { avisar, confirmar } = useInterface();
  const [adicionando, setAdicionando] = useState(null);
  const [senhaGerada, setSenhaGerada] = useState(null);
  const [editandoEmail, setEditandoEmail] = useState(null);
  const [cadastroConcluido, setCadastroConcluido] = useState(null);
  const [erro, setErro] = useState(null);

  async function remover(pessoa, tipo) {
    setErro(null);
    const ok = await confirmar({
      titulo: `Tirar ${pessoa.nome} da turma?`,
      mensagem:
        tipo === "aluno"
          ? "O aluno deixa de ver as atividades desta turma. As respostas e o desempenho dele continuam guardados."
          : "O professor deixa de lecionar nesta turma. As atividades que ele criou continuam aqui.",
      textoConfirmar: "Tirar da turma",
      perigo: true,
    });
    if (!ok) {
      return;
    }
    try {
      if (tipo === "aluno") {
        await desvincularAluno(turma.id, pessoa.id);
      } else {
        await desvincularProfessor(turma.id, pessoa.id);
      }
      avisar(`${pessoa.nome} saiu da turma ${turma.nome}.`);
      await aoMudar();
    } catch (err) {
      setErro(err.message);
    }
  }

  async function novaSenha(aluno) {
    setErro(null);
    const destino = aluno.emailResponsavel
      ? `A senha nova vai para o e-mail do responsável (${aluno.emailResponsavel}).`
      : "Este aluno não tem e-mail do responsável cadastrado: a senha nova aparece aqui para você entregar.";
    const ok = await confirmar({
      titulo: `Gerar uma senha nova para ${aluno.nome}?`,
      mensagem: `${destino} A senha atual deixa de valer e as sessões abertas dessa conta são encerradas.`,
      textoConfirmar: "Gerar senha",
    });
    if (!ok) {
      return;
    }
    try {
      const resultado = await redefinirSenha(aluno.id);
      if (resultado.enviadaPorEmail) {
        const envio = resultadoDoEnvio(resultado.statusEmailAcesso, "o responsável");
        avisar(
          envio.tipo === "success" ? `Senha nova de ${aluno.nome} enviada para o e-mail do responsável.` : envio.texto,
          envio.tipo
        );
      } else {
        setSenhaGerada(resultado);
      }
      await aoMudar();
    } catch (err) {
      setErro(err.message);
    }
  }

  /** {@code cadastro}: pessoa nova (mostra o login e se o e-mail saiu); senão, só a mensagem. */
  async function aoAdicionar(mensagem, cadastro = null) {
    setAdicionando(null);
    if (cadastro) {
      setCadastroConcluido(cadastro);
    } else {
      avisar(mensagem);
    }
    await aoMudar();
  }

  async function aoSalvarEmail(mensagem, tipo = "success") {
    setEditandoEmail(null);
    avisar(mensagem, tipo);
    await aoMudar();
  }

  return (
    <>
      <Mensagens erro={erro} />
      <div className="row g-4">
        <div className="col-lg-5">
          <div className="card h-100">
            <div className="card-header d-flex justify-content-between align-items-center bg-white">
              <h2 className="h6 mb-0">Professores ({turma.professores.length})</h2>
              {ehAdmin && turma.ativo && (
                <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => setAdicionando("professor")}>
                  Adicionar professor
                </button>
              )}
            </div>
            <ul className="list-group list-group-flush">
              {turma.professores.length === 0 && (
                <li className="list-group-item text-secondary">
                  Nenhum professor ainda.{ehAdmin ? " Adicione quem vai lecionar nesta turma." : ""}
                </li>
              )}
              {turma.professores.map((professor) => (
                <li key={professor.id} className="list-group-item d-flex align-items-center gap-2">
                  <div className="flex-grow-1">
                    <div>{professor.nome}</div>
                    <div className="small text-secondary">{professor.identificadorDeAcesso}</div>
                  </div>
                  {professor.senhaTemporaria && <span className="badge text-bg-light border">1º acesso pendente</span>}
                  {professor.senhaTemporaria && <SeloEmailAcesso status={professor.statusEmailAcesso} quando={professor.emailAcessoEm} />}
                  {ehAdmin && (
                    <button type="button" className="btn btn-sm btn-link text-danger" onClick={() => remover(professor, "professor")}>
                      Tirar
                    </button>
                  )}
                </li>
              ))}
            </ul>
          </div>
        </div>

        <div className="col-lg-7">
          <div className="card h-100">
            <div className="card-header d-flex justify-content-between align-items-center bg-white">
              <h2 className="h6 mb-0">Alunos ({turma.alunos.length})</h2>
              {turma.ativo && (
                <button type="button" className="btn btn-sm btn-primary" onClick={() => setAdicionando("aluno")}>
                  Adicionar aluno
                </button>
              )}
            </div>
            <ul className="list-group list-group-flush">
              {turma.alunos.length === 0 && (
                <li className="list-group-item text-secondary">
                  Nenhum aluno ainda. Use “Adicionar aluno” para cadastrar e já vincular à turma.
                </li>
              )}
              {turma.alunos.map((aluno) => (
                <li key={aluno.id} className="list-group-item d-flex flex-wrap align-items-center gap-2">
                  <div className="flex-grow-1">
                    <div>{aluno.nome}</div>
                    <div className="small text-secondary">
                      login <code>{aluno.identificadorDeAcesso}</code>
                      {aluno.emailResponsavel && <> · responsável: {aluno.emailResponsavel}</>}
                    </div>
                  </div>
                  {!aluno.ativo && <span className="badge text-bg-secondary">desativada</span>}
                  {aluno.senhaTemporaria && aluno.ativo && <span className="badge text-bg-light border">1º acesso pendente</span>}
                  {aluno.senhaTemporaria && aluno.ativo && (
                    <SeloEmailAcesso status={aluno.statusEmailAcesso} quando={aluno.emailAcessoEm} />
                  )}
                  {!aluno.emailResponsavel && aluno.ativo && (
                    <span className="badge text-bg-warning">sem e-mail do responsável</span>
                  )}
                  <div className="btn-group btn-group-sm">
                    <button type="button" className="btn btn-outline-secondary" onClick={() => setEditandoEmail(aluno)}>
                      E-mail do responsável
                    </button>
                    <button type="button" className="btn btn-outline-secondary" onClick={() => novaSenha(aluno)}>
                      Nova senha
                    </button>
                    <button type="button" className="btn btn-outline-danger" onClick={() => remover(aluno, "aluno")}>
                      Tirar
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>

      {adicionando && (
        <AdicionarPessoa
          tipo={adicionando}
          turma={turma}
          aoAdicionar={aoAdicionar}
          aoFechar={() => setAdicionando(null)}
        />
      )}
      {editandoEmail && (
        <EmailDoResponsavel aluno={editandoEmail} aoSalvar={aoSalvarEmail} aoFechar={() => setEditandoEmail(null)} />
      )}
      {senhaGerada && <SenhaProvisoria dados={senhaGerada} aoFechar={() => setSenhaGerada(null)} />}
      {cadastroConcluido && <CadastroConcluido dados={cadastroConcluido} aoFechar={() => setCadastroConcluido(null)} />}
    </>
  );
}

/**
 * Adicionar pessoa à turma: cadastrar alguém novo (cadastro e vínculo na mesma operação) ou
 * vincular quem já tem conta na escola.
 */
function AdicionarPessoa({ tipo, turma, aoAdicionar, aoFechar }) {
  const ehAluno = tipo === "aluno";
  const [modo, setModo] = useState("novo");
  const [nome, setNome] = useState("");
  const [emailProfessor, setEmailProfessor] = useState("");
  const [emailResponsavel, setEmailResponsavel] = useState("");
  const [disponiveis, setDisponiveis] = useState(null);
  const [escolhido, setEscolhido] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    const listar = ehAluno ? listarAlunosDisponiveis : listarProfessoresDisponiveis;
    listar(turma.id)
      .then(setDisponiveis)
      .catch(() => setDisponiveis([]));
  }, [ehAluno, turma.id]);

  async function salvar(evento) {
    evento.preventDefault();
    setErro(null);
    setSalvando(true);
    try {
      if (modo === "novo") {
        if (ehAluno) {
          const cadastro = await cadastrarAluno(nome, emailResponsavel, turma.id);
          await aoAdicionar(null, { ...cadastro, tipo: "aluno" });
        } else {
          const cadastro = await cadastrarProfessor(nome, emailProfessor, turma.id);
          await aoAdicionar(null, { ...cadastro, tipo: "professor" });
        }
      } else {
        const pessoa = disponiveis.find((p) => String(p.id) === escolhido);
        if (ehAluno) {
          await vincularAluno(turma.id, pessoa.id);
        } else {
          await vincularProfessor(turma.id, pessoa.id);
        }
        await aoAdicionar(`${pessoa.nome} vinculado(a) à turma ${turma.nome}.`);
      }
    } catch (err) {
      setErro(err.message);
      setSalvando(false);
    }
  }

  const podeSalvar =
    modo === "novo" ? nome.trim() && (ehAluno ? emailResponsavel.trim() : emailProfessor.trim()) : escolhido;

  return (
    <Modal titulo={ehAluno ? `Adicionar aluno em ${turma.nome}` : `Adicionar professor em ${turma.nome}`} aoFechar={aoFechar}>
      <form onSubmit={salvar}>
        <div className="btn-group w-100 mb-3" role="group" aria-label="Como adicionar">
          <input type="radio" className="btn-check" id="modo-novo" checked={modo === "novo"} onChange={() => setModo("novo")} />
          <label className="btn btn-outline-primary" htmlFor="modo-novo">
            Cadastrar novo
          </label>
          <input
            type="radio"
            className="btn-check"
            id="modo-existente"
            checked={modo === "existente"}
            onChange={() => setModo("existente")}
          />
          <label className="btn btn-outline-primary" htmlFor="modo-existente">
            Já tem conta na escola
          </label>
        </div>

        <Mensagens erro={erro} />

        {modo === "novo" ? (
          <>
            <div className="mb-3">
              <label htmlFor="nome-pessoa" className="form-label">
                Nome completo
              </label>
              <input id="nome-pessoa" className="form-control" value={nome} onChange={(e) => setNome(e.target.value)} maxLength={255} autoFocus required />
            </div>
            {!ehAluno && (
              <div className="mb-3">
                <label htmlFor="email-professor" className="form-label">
                  E-mail
                </label>
                <input
                  id="email-professor"
                  type="email"
                  className="form-control"
                  value={emailProfessor}
                  onChange={(e) => setEmailProfessor(e.target.value)}
                  placeholder="nome@escola.edu.br"
                  required
                />
                <div className="form-text">O e-mail é o login do professor; a senha provisória vai para ele.</div>
              </div>
            )}
            {ehAluno && (
              <div className="mb-3">
                <label htmlFor="email-responsavel-novo" className="form-label">
                  E-mail do responsável
                </label>
                <input
                  id="email-responsavel-novo"
                  type="email"
                  className="form-control"
                  value={emailResponsavel}
                  onChange={(e) => setEmailResponsavel(e.target.value)}
                  maxLength={255}
                  placeholder="responsavel@exemplo.com"
                  required
                />
                <div className="form-text">
                  O MaisEdu gera o login do aluno (ex.: a482913) e envia o login e a senha provisória para este e-mail. O
                  aluno não tem e-mail no MaisEdu.
                </div>
              </div>
            )}
          </>
        ) : (
          <div className="mb-3">
            <label htmlFor="pessoa-existente" className="form-label">
              {ehAluno ? "Aluno" : "Professor"}
            </label>
            {disponiveis === null ? (
              <Carregando />
            ) : disponiveis.length === 0 ? (
              <p className="text-secondary mb-0">
                Ninguém disponível: todos os {ehAluno ? "alunos" : "professores"} da escola já estão nesta turma.
              </p>
            ) : (
              <select id="pessoa-existente" className="form-select" value={escolhido} onChange={(e) => setEscolhido(e.target.value)} required>
                <option value="">Selecione...</option>
                {disponiveis.map((pessoa) => (
                  <option key={pessoa.id} value={pessoa.id}>
                    {pessoa.nome} ({pessoa.identificadorDeAcesso})
                  </option>
                ))}
              </select>
            )}
          </div>
        )}

        <div className="d-flex justify-content-end gap-2 mt-4">
          <button type="button" className="btn btn-outline-secondary" onClick={aoFechar}>
            Cancelar
          </button>
          <button type="submit" className="btn btn-primary" disabled={salvando || !podeSalvar}>
            {salvando ? "Salvando..." : modo === "novo" ? "Cadastrar e vincular" : "Vincular"}
          </button>
        </div>
      </form>
    </Modal>
  );
}

/** Depois de cadastrar alguém novo: o login (gerado, no caso do aluno) e se o e-mail de acesso saiu. */
function CadastroConcluido({ dados, aoFechar }) {
  const ehAluno = dados.tipo === "aluno";
  const envio = resultadoDoEnvio(dados.statusEmailAcesso, ehAluno ? "o e-mail do responsável" : "o e-mail do professor");
  const classeAlerta = { success: "alert-success", warning: "alert-warning", danger: "alert-danger" }[envio.tipo];
  return (
    <Modal
      titulo={ehAluno ? "Aluno cadastrado" : "Professor cadastrado"}
      aoFechar={aoFechar}
      rodape={
        <button type="button" className="btn btn-primary" onClick={aoFechar}>
          Fechar
        </button>
      }
    >
      <p className="mb-2">
        <strong>{dados.nome}</strong> já está na turma e entra com o login:
      </p>
      <p className="fs-4 font-monospace text-center bg-light border rounded py-2 user-select-all">{dados.identificadorDeAcesso}</p>
      <div className={`alert ${classeAlerta} py-2 mb-0`} role="status">
        {envio.texto}
      </div>
      {ehAluno && (
        <p className="small text-secondary mt-3 mb-0">
          A senha provisória não aparece aqui: ela vai só para o responsável. No primeiro acesso, o aluno troca por uma senha
          própria.
        </p>
      )}
    </Modal>
  );
}
