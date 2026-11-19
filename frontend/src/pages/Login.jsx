import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { Logo } from "../components/Logo";
import { Mensagens } from "../components/Mensagens";
import { RodapeLegal } from "../components/RodapeLegal";

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
      await login(identificador, senha);
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div className="me-tela-publica">
      <div className="w-100 me-largura-sm">
        <div className="text-center mb-4">
          <Logo tamanho={44} />
          <p className="text-secondary mt-2 mb-0">Matemática do 6º ao 9º ano, no ritmo de cada aluno</p>
        </div>
        <div className="card shadow-sm">
          <div className="card-body p-4">
            <h1 className="h5 mb-3">Entrar</h1>
            <Mensagens erro={erro} aviso={!erro ? avisoSessao : null} />
            <form onSubmit={handleSubmit} noValidate={false}>
              <div className="mb-3">
                <label htmlFor="identificador" className="form-label">
                  E-mail ou login
                </label>
                <input
                  id="identificador"
                  type="text"
                  className="form-control"
                  value={identificador}
                  onChange={(e) => setIdentificador(e.target.value)}
                  autoComplete="username"
                  autoFocus
                  required
                />
                <div className="form-text">
                  Professores entram com o e-mail; alunos, com o login que chegou no e-mail do responsável (ex.: a482913).
                </div>
              </div>
              <div className="mb-3">
                <label htmlFor="senha" className="form-label">
                  Senha
                </label>
                <input
                  id="senha"
                  type="password"
                  className="form-control"
                  value={senha}
                  onChange={(e) => setSenha(e.target.value)}
                  autoComplete="current-password"
                  required
                />
              </div>
              <button type="submit" className="btn btn-primary w-100" disabled={carregando}>
                {carregando ? "Entrando..." : "Entrar"}
              </button>
            </form>
            <p className="small text-secondary mt-3 mb-0">
              Esqueceu a senha? Aluno: peça uma senha nova ao professor; ela chega no e-mail do seu responsável.
              Professor: fale com o administrador da escola.
            </p>
          </div>
        </div>
        <RodapeLegal />
      </div>
    </div>
  );
}
