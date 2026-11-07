import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { RodapeLegal } from "../components/RodapeLegal";
import { Mensagens } from "../components/Mensagens";

/** RF01: entrada única para todos os perfis (e-mail para admin/professor, login para aluno). */
export default function Login() {
  const { login, avisoSessao } = useAuth();
  const [identificador, setIdentificador] = useState("");
  const [senha, setSenha] = useState("");
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setErro(null);
    setCarregando(true);
    try {
      // Ao entrar, a rota de login redireciona sozinha para a tela inicial (ou a que o usuário tentou abrir).
      await login(identificador, senha);
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
          <h1 className="h3 text-center text-primary fw-bold mb-1">MaisEdu</h1>
          <p className="text-center text-body-secondary mb-4">Aprendizagem adaptativa de Matemática</p>

          {avisoSessao && !erro && <div className="alert alert-warning">{avisoSessao}</div>}

          <form onSubmit={handleSubmit}>
            <div className="mb-3">
              <label className="form-label" htmlFor="identificador">E-mail ou login</label>
              <input id="identificador" type="text" className="form-control" value={identificador}
                     onChange={(e) => setIdentificador(e.target.value)} autoComplete="username" autoFocus required />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="senha">Senha</label>
              <input id="senha" type="password" className="form-control" value={senha}
                     onChange={(e) => setSenha(e.target.value)} autoComplete="current-password" required />
            </div>
            <button type="submit" className="btn btn-primary w-100" disabled={carregando}>
              {carregando ? "Entrando..." : "Entrar"}
            </button>
          </form>

          <Mensagens erro={erro} />
        </div>
      </div>
      <RodapeLegal />
    </div>
  );
}
