import { useState } from "react";
import { cadastrarAluno } from "../services/usuarioService";
import { Mensagens } from "../components/Mensagens";

/** Mesma regra do back-end (CadastroAlunoRequest): 3 a 50 caracteres, sem acento nem espaço. */
const LOGIN_VALIDO = /^[A-Za-z0-9._-]{3,50}$/;
const REGRA_LOGIN = "De 3 a 50 caracteres: letras sem acento, números, ponto, hífen ou sublinhado.";

/** RF05: o professor cadastra um aluno (sem e-mail) e recebe a senha provisória para repassar. */
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

    if (!nome.trim()) {
      setErro("Informe o nome");
      return;
    }
    if (!LOGIN_VALIDO.test(login.trim())) {
      setErro(`Login inválido. ${REGRA_LOGIN}`);
      return;
    }

    setCarregando(true);
    try {
      const novo = await cadastrarAluno(nome.trim(), login.trim());
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
    <div className="pagina pagina-estreita">
      <h2>Cadastrar aluno</h2>

      <form onSubmit={handleSubmit} className="formulario">
        <label>
          Nome
          <input className="form-control" type="text" value={nome} onChange={(e) => setNome(e.target.value)} required maxLength={255}
                 autoFocus />
        </label>

        <label>
          Login (ex.: matrícula)
          <input className="form-control" type="text" value={login} onChange={(e) => setLogin(e.target.value)} required maxLength={50}
                 autoComplete="off" />
          <span className="dica">{REGRA_LOGIN} O aluno não tem e-mail cadastrado.</span>
        </label>

        <button className="btn btn-primary" type="submit" disabled={carregando}>
          {carregando ? "Cadastrando..." : "Cadastrar"}
        </button>
      </form>

      <Mensagens erro={erro} />

      {resultado && (
        <div className="cartao cartao-selecionado" role="status">
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
