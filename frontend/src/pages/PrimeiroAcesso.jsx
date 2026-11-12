import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { AceiteTermos } from "../components/AceiteTermos";
import { CamposSenhaNova } from "../components/CampoSenhaNova";
import { Logo } from "../components/Logo";
import { Mensagens } from "../components/Mensagens";
import { RodapeLegal } from "../components/RodapeLegal";

/** Primeiro acesso (ou senha redefinida): trocar a senha provisória e aceitar os Termos/Política. */
export default function PrimeiroAcesso() {
  const { usuario, trocarSenha, logout } = useAuth();
  const [senhaAtual, setSenhaAtual] = useState("");
  const [novaSenha, setNovaSenha] = useState("");
  const [confirmacao, setConfirmacao] = useState("");
  const [aceiteTermos, setAceiteTermos] = useState(false);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setErro(null);
    if (novaSenha !== confirmacao) {
      setErro("A confirmação não é igual à nova senha.");
      return;
    }
    if (!aceiteTermos) {
      setErro("É preciso aceitar os Termos de Uso e a Política de Privacidade para continuar.");
      return;
    }
    setCarregando(true);
    try {
      // o back-end encerra a sessão ao trocar a senha: a tela de login volta, agora com a senha nova
      await trocarSenha(senhaAtual, novaSenha, aceiteTermos);
    } catch (err) {
      setErro(err.message);
      setCarregando(false);
    }
  }

  return (
    <div className="me-tela-publica">
      <div className="w-100 me-largura-sm">
        <div className="text-center mb-4">
          <Logo tamanho={40} />
        </div>
        <div className="card shadow-sm">
          <div className="card-body p-4">
            <h1 className="h5">Bem-vindo(a), {usuario.nome.split(" ")[0]}!</h1>
            <p className="text-secondary">
              Por segurança, defina uma senha só sua antes de continuar. Depois você entra de novo com ela.
            </p>
            <Mensagens erro={erro} />
            <form onSubmit={handleSubmit}>
              <div className="mb-3">
                <label htmlFor="senha-atual" className="form-label">
                  Senha provisória
                </label>
                <input
                  id="senha-atual"
                  type="password"
                  className="form-control"
                  value={senhaAtual}
                  onChange={(e) => setSenhaAtual(e.target.value)}
                  autoComplete="current-password"
                  autoFocus
                  required
                />
              </div>
              <CamposSenhaNova
                novaSenha={novaSenha}
                confirmacao={confirmacao}
                aoMudarSenha={setNovaSenha}
                aoMudarConfirmacao={setConfirmacao}
              />
              <AceiteTermos marcado={aceiteTermos} aoMudar={setAceiteTermos} />
              <div className="d-flex gap-2">
                <button type="submit" className="btn btn-primary flex-grow-1" disabled={carregando}>
                  {carregando ? "Salvando..." : "Trocar senha e continuar"}
                </button>
                <button type="button" className="btn btn-outline-secondary" onClick={logout}>
                  Sair
                </button>
              </div>
            </form>
          </div>
        </div>
        <RodapeLegal />
      </div>
    </div>
  );
}
