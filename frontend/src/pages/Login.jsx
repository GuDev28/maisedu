import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { RodapeLegal } from "../components/RodapeLegal";

export default function Login({ onAbrirPaginaLegal }) {
  const { login } = useAuth();
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
    <div style={{ maxWidth: 360, margin: "4rem auto" }}>
      <h2>Entrar no MaisEdu</h2>

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 12 }}>
          <label>
            E-mail ou login:{" "}
            <input
              type="text"
              value={identificador}
              onChange={(e) => setIdentificador(e.target.value)}
              autoFocus
              required
            />
          </label>
        </div>

        <div style={{ marginBottom: 12 }}>
          <label>
            Senha:{" "}
            <input
              type="password"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              required
            />
          </label>
        </div>

        <button type="submit" disabled={carregando}>
          {carregando ? "Entrando..." : "Entrar"}
        </button>
      </form>

      {erro && <p style={{ color: "red" }}>{erro}</p>}

      <RodapeLegal onAbrirPaginaLegal={onAbrirPaginaLegal} />
    </div>
  );
}
