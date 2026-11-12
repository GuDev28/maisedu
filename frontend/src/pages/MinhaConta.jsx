import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useInterface } from "../context/InterfaceContext";
import { meusDados, minhaConta } from "../services/authService";
import { baixarJson } from "../services/http";
import { Cabecalho } from "../components/Cabecalho";
import { CamposSenhaNova } from "../components/CampoSenhaNova";
import { Carregando } from "../components/Carregando";
import { Mensagens } from "../components/Mensagens";
import { PAPEIS, formatarDataHora } from "../components/rotulos";

/** Minha conta (todos os perfis): dados, troca de senha, aceite dos termos e "baixar meus dados" (LGPD). */
export default function MinhaConta() {
  const { trocarSenha } = useAuth();
  const { avisar } = useInterface();
  const [conta, setConta] = useState(null);
  const [erro, setErro] = useState(null);
  const [senhaAtual, setSenhaAtual] = useState("");
  const [novaSenha, setNovaSenha] = useState("");
  const [confirmacao, setConfirmacao] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [erroSenha, setErroSenha] = useState(null);
  const [baixando, setBaixando] = useState(false);

  useEffect(() => {
    minhaConta().then(setConta).catch((err) => setErro(err.message));
  }, []);

  async function alterarSenha(evento) {
    evento.preventDefault();
    setErroSenha(null);
    if (novaSenha !== confirmacao) {
      setErroSenha("A confirmação não é igual à nova senha.");
      return;
    }
    setSalvando(true);
    try {
      // troca voluntária: o aceite dos termos já está registrado, não precisa marcar de novo
      await trocarSenha(senhaAtual, novaSenha, false);
    } catch (err) {
      setErroSenha(err.message);
      setSalvando(false);
    }
  }

  async function baixar() {
    setBaixando(true);
    try {
      baixarJson(await meusDados(), "maisedu-meus-dados.json");
      avisar("Arquivo com os seus dados gerado.");
    } catch (err) {
      setErro(err.message);
    } finally {
      setBaixando(false);
    }
  }

  return (
    <div className="me-largura-md">
      <Cabecalho titulo="Minha conta" />
      <Mensagens erro={erro} />
      {!conta && !erro && <Carregando />}
      {conta && (
        <div className="row g-4">
          <div className="col-12">
            <div className="card">
              <div className="card-body">
                <h2 className="h6 text-secondary text-uppercase mb-3">Dados da conta</h2>
                <dl className="row mb-0">
                  <dt className="col-sm-4 fw-normal text-secondary">Nome</dt>
                  <dd className="col-sm-8">{conta.nome}</dd>
                  <dt className="col-sm-4 fw-normal text-secondary">{conta.roles.includes("ALUNO") ? "Login" : "E-mail"}</dt>
                  <dd className="col-sm-8">
                    <code>{conta.identificadorDeAcesso}</code>
                  </dd>
                  {conta.roles.includes("ALUNO") && (
                    <>
                      <dt className="col-sm-4 fw-normal text-secondary">E-mail do responsável</dt>
                      <dd className="col-sm-8">{conta.emailResponsavel ?? "não cadastrado"}</dd>
                    </>
                  )}
                  <dt className="col-sm-4 fw-normal text-secondary">Escola</dt>
                  <dd className="col-sm-8">{conta.instituicao}</dd>
                  <dt className="col-sm-4 fw-normal text-secondary">Perfil</dt>
                  <dd className="col-sm-8">{conta.roles.map((r) => PAPEIS[r] ?? r).join(", ")}</dd>
                  <dt className="col-sm-4 fw-normal text-secondary">Conta criada em</dt>
                  <dd className="col-sm-8 mb-0">{formatarDataHora(conta.criadoEm)}</dd>
                </dl>
                <p className="small text-secondary mt-3 mb-0">
                  Algum dado errado? Peça a correção ao {conta.roles.includes("ALUNO") ? "seu professor ou ao " : ""}
                  administrador da escola.
                </p>
              </div>
            </div>
          </div>

          <div className="col-md-6">
            <form className="card h-100" onSubmit={alterarSenha}>
              <div className="card-body">
                <h2 className="h6 text-secondary text-uppercase mb-3">Alterar senha</h2>
                <Mensagens erro={erroSenha} />
                <div className="mb-3">
                  <label htmlFor="senha-atual" className="form-label">
                    Senha atual
                  </label>
                  <input
                    id="senha-atual"
                    type="password"
                    className="form-control"
                    value={senhaAtual}
                    onChange={(e) => setSenhaAtual(e.target.value)}
                    autoComplete="current-password"
                    required
                  />
                </div>
                <CamposSenhaNova
                  novaSenha={novaSenha}
                  confirmacao={confirmacao}
                  aoMudarSenha={setNovaSenha}
                  aoMudarConfirmacao={setConfirmacao}
                />
                <p className="small text-secondary">Depois de trocar, você entra de novo com a senha nova.</p>
                <button type="submit" className="btn btn-primary" disabled={salvando}>
                  {salvando ? "Salvando..." : "Alterar senha"}
                </button>
              </div>
            </form>
          </div>

          <div className="col-md-6 d-flex flex-column gap-4">
            <div className="card">
              <div className="card-body">
                <h2 className="h6 text-secondary text-uppercase mb-3">Termos e privacidade</h2>
                <p className="mb-2">
                  Você aceitou a versão <strong>{conta.versaoTermosAceita ?? "—"}</strong>
                  {conta.termosAceitosEm && ` em ${formatarDataHora(conta.termosAceitosEm)}`}.
                </p>
                <p className="small mb-0">
                  <Link to="/termos">Termos de Uso</Link> · <Link to="/privacidade">Política de Privacidade</Link>
                </p>
              </div>
            </div>
            <div className="card">
              <div className="card-body">
                <h2 className="h6 text-secondary text-uppercase mb-3">Meus dados (LGPD)</h2>
                <p className="small text-secondary">
                  Baixe tudo o que o MaisEdu guarda sobre você: cadastro, turmas, respostas, desempenho e registros de
                  auditoria, num arquivo JSON.
                </p>
                <button type="button" className="btn btn-outline-primary" onClick={baixar} disabled={baixando}>
                  {baixando ? "Gerando..." : "Baixar meus dados"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
