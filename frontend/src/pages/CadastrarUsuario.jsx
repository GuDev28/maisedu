import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { cadastrarAluno, cadastrarProfessor } from "../services/usuarioService";


export default function CadastrarUsuario() {
  const { usuario } = useAuth();
  const ehAdmin = usuario.roles.includes("ADMIN");

  const [nome, setNome] = useState("");
  const [identificador, setIdentificador] = useState(""); // e-mail (professor) ou login (aluno)
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);
  const [resultado, setResultado] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setErro(null);
    setResultado(null);
    setCarregando(true);

    try {
      const novo = ehAdmin
        ? await cadastrarProfessor(nome, identificador)
        : await cadastrarAluno(nome, identificador);
      // Guarda o identificador digitado dentro do próprio resultado: o campo
      // do formulário é limpo logo abaixo, e a mensagem de sucesso ainda
      // precisa mostrar pra quem o e-mail foi enviado.
      setResultado({ ...novo, identificadorInformado: identificador });
      setNome("");
      setIdentificador("");
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div style={{ maxWidth: 420, margin: "2rem auto" }}>
      <h2>{ehAdmin ? "Cadastrar professor" : "Cadastrar aluno"}</h2>

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 12 }}>
          <label>
            Nome:{" "}
            <input type="text" value={nome} onChange={(e) => setNome(e.target.value)} required autoFocus />
          </label>
        </div>

        <div style={{ marginBottom: 12 }}>
          <label>
            {ehAdmin ? "E-mail:" : "Login (ex.: matrícula):"}{" "}
            <input
              type={ehAdmin ? "email" : "text"}
              value={identificador}
              onChange={(e) => setIdentificador(e.target.value)}
              required
            />
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
          {resultado.senhaProvisoria ? (
            <p>
              Senha provisória (repasse ao aluno fora do sistema, ele não tem e-mail cadastrado):{" "}
              <code>{resultado.senhaProvisoria}</code>
            </p>
          ) : (
            <p>A senha provisória foi enviada por e-mail para {resultado.identificadorInformado}.</p>
          )}
        </div>
      )}
    </div>
  );
}
