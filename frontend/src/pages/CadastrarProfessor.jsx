import { useState } from "react";
import { cadastrarProfessor } from "../services/usuarioService";

export default function CadastrarProfessor() {
  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);
  const [sucesso, setSucesso] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setErro(null);
    setSucesso(null);
    setCarregando(true);

    try {
      const novo = await cadastrarProfessor(nome, email);
      setSucesso(`${novo.nome} cadastrado(a). A senha provisória foi enviada por e-mail para ${email}.`);
      setNome("");
      setEmail("");
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div style={{ maxWidth: 420, margin: "2rem auto" }}>
      <h2>Cadastrar professor</h2>

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 12 }}>
          <label>
            Nome:{" "}
            <input type="text" value={nome} onChange={(e) => setNome(e.target.value)} required autoFocus />
          </label>
        </div>

        <div style={{ marginBottom: 12 }}>
          <label>
            E-mail:{" "}
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
        </div>

        <button type="submit" disabled={carregando}>
          {carregando ? "Cadastrando..." : "Cadastrar"}
        </button>
      </form>

      {erro && <p style={{ color: "red" }}>{erro}</p>}
      {sucesso && <p style={{ color: "green" }}>{sucesso}</p>}
    </div>
  );
}
