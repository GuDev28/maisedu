import { useState } from "react";
import { cadastrarProfessor } from "../services/usuarioService";
import { Mensagens } from "../components/Mensagens";

/** RF04: o administrador cadastra um professor; a senha provisória vai por e-mail (Resend). */
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

    if (!nome.trim()) {
      setErro("Informe o nome");
      return;
    }

    setCarregando(true);
    try {
      const novo = await cadastrarProfessor(nome.trim(), email.trim());
      setSucesso(`${novo.nome} cadastrado(a). A senha provisória foi enviada por e-mail para ${email.trim()}.`);
      setNome("");
      setEmail("");
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div className="pagina pagina-estreita">
      <h2>Cadastrar professor</h2>

      <form onSubmit={handleSubmit} className="formulario">
        <label>
          Nome
          <input className="form-control" type="text" value={nome} onChange={(e) => setNome(e.target.value)} required maxLength={255}
                 autoFocus />
        </label>

        <label>
          E-mail
          <input className="form-control" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required maxLength={255} />
          <span className="dica">A senha provisória é enviada para este e-mail e não aparece na tela.</span>
        </label>

        <button className="btn btn-primary" type="submit" disabled={carregando}>
          {carregando ? "Cadastrando..." : "Cadastrar"}
        </button>
      </form>

      <Mensagens erro={erro} sucesso={sucesso} />
    </div>
  );
}
