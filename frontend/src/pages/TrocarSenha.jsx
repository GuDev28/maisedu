import { useState } from "react";
import { Link } from "react-router";
import { useAuth } from "../context/AuthContext";
import { RodapeLegal } from "../components/RodapeLegal";
import { Mensagens } from "../components/Mensagens";

const TAMANHO_MINIMO = 8;
const NOVA_ABA = { target: "_blank", rel: "noopener noreferrer" };

/** RF02: troca obrigatória da senha provisória, com aceite dos Termos de Uso e da Política de Privacidade. */
export default function TrocarSenha() {
  const { trocarSenha, logout } = useAuth();
  const [senhaAtual, setSenhaAtual] = useState("");
  const [novaSenha, setNovaSenha] = useState("");
  const [confirmacao, setConfirmacao] = useState("");
  const [aceiteTermos, setAceiteTermos] = useState(false);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setErro(null);

    // As mesmas regras do RF02 são conferidas de novo no servidor (CA02 e CA03).
    if (novaSenha.length < TAMANHO_MINIMO) {
      setErro(`A nova senha deve ter pelo menos ${TAMANHO_MINIMO} caracteres.`);
      return;
    }
    if (novaSenha === senhaAtual) {
      setErro("A nova senha deve ser diferente da atual.");
      return;
    }
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
      // trocarSenha() encerra a sessão: a rota volta para o login, agora com a senha nova (CA04).
      await trocarSenha(senhaAtual, novaSenha, aceiteTermos);
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div className="tela-central">
      <div className="card shadow-sm cartao-acesso">
        <div className="card-body p-4">
          <h1 className="h4 mb-1">Troque sua senha provisória</h1>
          <p className="text-body-secondary">Por segurança, defina uma senha própria antes de continuar.</p>

          <form onSubmit={handleSubmit}>
            <div className="mb-3">
              <label className="form-label" htmlFor="senha-atual">Senha provisória atual</label>
              <input id="senha-atual" type="password" className="form-control" value={senhaAtual}
                     onChange={(e) => setSenhaAtual(e.target.value)} autoFocus required autoComplete="current-password" />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="nova-senha">Nova senha</label>
              <input id="nova-senha" type="password" className="form-control" value={novaSenha}
                     onChange={(e) => setNovaSenha(e.target.value)} minLength={TAMANHO_MINIMO} required
                     autoComplete="new-password" aria-describedby="dica-senha" />
              <div id="dica-senha" className="form-text">
                Mínimo de {TAMANHO_MINIMO} caracteres, diferente da senha provisória.
              </div>
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="confirmacao">Confirme a nova senha</label>
              <input id="confirmacao" type="password" className="form-control" value={confirmacao}
                     onChange={(e) => setConfirmacao(e.target.value)} minLength={TAMANHO_MINIMO} required
                     autoComplete="new-password" />
            </div>
            <div className="form-check mb-3">
              <input id="aceite-termos" type="checkbox" className="form-check-input" checked={aceiteTermos}
                     onChange={(e) => setAceiteTermos(e.target.checked)} required />
              <label className="form-check-label" htmlFor="aceite-termos">
                Li e aceito os <Link to="/termos" {...NOVA_ABA}>Termos de Uso</Link> e a{" "}
                <Link to="/privacidade" {...NOVA_ABA}>Política de Privacidade</Link>
              </label>
            </div>
            <div className="d-flex gap-2">
              <button type="submit" className="btn btn-primary" disabled={carregando}>
                {carregando ? "Salvando..." : "Trocar senha"}
              </button>
              <button type="button" className="btn btn-outline-secondary" onClick={logout}>Sair</button>
            </div>
          </form>

          <Mensagens erro={erro} />
        </div>
      </div>
      <RodapeLegal novaAba />
    </div>
  );
}
