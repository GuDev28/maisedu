import { useState } from "react";
import { cadastrarAluno } from "../services/usuarioService";

export default function CadastrarAluno() {
  const [nome, setNome] = useState("");
  const [login, setLogin] = useState("");
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);
  const [resultado, setResultado] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setErro(null);
    setResultado(null);
    setCarregando(true);

    try {
      const novo = await cadastrarAluno(nome, login);
      setResultado(novo);
      setNome("");
      setLogin("");
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div style={{ maxWidth: 420, margin: "2rem auto" }}>
      <h2>Cadastrar aluno</h2>

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 12 }}>
          <label>
            Nome:{" "}
            <input type="text" value={nome} onChange={(e) => setNome(e.target.value)} required autoFocus />
          </label>
        </div>

        <div style={{ marginBottom: 12 }}>
          <label>
            Login (ex.: matrícula):{" "}
            <input type="text" value={login} onChange={(e) => setLogin(e.target.value)} required />
          </label>
        </div>

        <button type="submit" disabled={carregando}>
          {carregando ? "Cadastrando..." : "Cadastrar"}
        </button>
      </form>

      {erro && <p style={{ color: "red" }}>{erro}</p>}

      {resultado && (
        <div style={{ marginTop: 16, padding: 12, border: "1px solid #cde", borderRadius: 4 }}>
          <p>
            <strong>{resultado.nome}</strong> cadastrado(a) com sucesso (login: {resultado.identificadorDeAcesso}).
          </p>
          <p>
            Senha provisória (repasse ao aluno fora do sistema, ele não tem e-mail cadastrado):{" "}
            <code>{resultado.senhaProvisoria}</code>
          </p>
        </div>
      )}
    </div>
  );
}
