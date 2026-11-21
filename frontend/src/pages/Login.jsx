import { useState } from "react";
import { useAuth } from "../context/useAuth";
import { Icone } from "../components/Icone";
import { Mensagens } from "../components/Mensagens";
import { RodapeLegal } from "../components/RodapeLegal";

/** Ângulo com o arco marcado: o detalhe "de caderno de Matemática" do painel da marca. */
function Angulo() {
  return (
    <svg className="me-login-angulo" width="90" height="90" viewBox="0 0 76 76" fill="none" aria-hidden="true" focusable="false">
      <path d="M8 6v62h62" stroke="currentColor" strokeWidth="5" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M8 34a34 34 0 0 1 34 34" stroke="currentColor" strokeWidth="5" strokeLinecap="round" />
    </svg>
  );
}

/**
 * Tela de entrada: à esquerda, o painel da marca (a logo centralizada na metade de cima; embaixo, o
 * ângulo e a frase da proposta); à direita, o formulário. No celular, o painel vira uma faixa no topo. Não existe recuperação de senha por
 * e-mail: "Esqueci minha senha" explica com quem falar (o professor gera uma senha provisória para o
 * aluno; o administrador, para o professor).
 */
export default function Login() {
  const { login, avisoSessao } = useAuth();
  const [identificador, setIdentificador] = useState("");
  const [senha, setSenha] = useState("");
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [ajudaSenha, setAjudaSenha] = useState(false);
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
    <div className="me-login">
      <aside className="me-login-marca">
        <div className="me-login-topo">
          <img src="/logo-maisedu-branco.png" alt="MaisEdu" className="me-login-logo" width="422" height="374" />
        </div>
        <div className="me-login-base">
          <Angulo />
          <p className="me-login-frase">“Cada um tem um jeito de aprender. A gente se adapta ao seu.”</p>
        </div>
      </aside>

      <main className="me-login-area">
        <div className="me-login-caixa">
          <h1 className="me-login-titulo">Entrar</h1>
          <p className="me-login-subtitulo">Acesse sua conta para continuar</p>
          <Mensagens erro={erro} aviso={!erro ? avisoSessao : null} />

          <form onSubmit={handleSubmit}>
            <div className="mb-4">
              <label htmlFor="identificador" className="form-label me-login-rotulo">
                E-mail ou login
              </label>
              <input
                id="identificador"
                type="text"
                className="form-control me-login-campo"
                value={identificador}
                onChange={(e) => setIdentificador(e.target.value)}
                placeholder="professor@escola.com ou a482913"
                autoComplete="username"
                aria-describedby="dica-identificador"
                autoFocus
                required
              />
              <div id="dica-identificador" className="form-text">
                Professores entram com o e-mail; alunos, com o login que chegou no e-mail do responsável.
              </div>
            </div>

            <div className="mb-2">
              <label htmlFor="senha" className="form-label me-login-rotulo">
                Senha
              </label>
              <div className="me-login-senha">
                <input
                  id="senha"
                  type={mostrarSenha ? "text" : "password"}
                  className="form-control me-login-campo"
                  value={senha}
                  onChange={(e) => setSenha(e.target.value)}
                  autoComplete="current-password"
                  required
                />
                <button
                  type="button"
                  className="me-login-olho"
                  onClick={() => setMostrarSenha(!mostrarSenha)}
                  aria-label={mostrarSenha ? "Ocultar a senha" : "Mostrar a senha"}
                  aria-pressed={mostrarSenha}
                  aria-controls="senha"
                >
                  <Icone nome={mostrarSenha ? "olho" : "olhoFechado"} tamanho={22} />
                </button>
              </div>
            </div>

            <div className="text-end mb-4">
              <button
                type="button"
                className="btn btn-link btn-sm p-0 me-login-esqueci"
                aria-expanded={ajudaSenha}
                aria-controls="ajuda-senha"
                onClick={() => setAjudaSenha(!ajudaSenha)}
              >
                Esqueci minha senha
              </button>
            </div>
            {ajudaSenha && (
              <div id="ajuda-senha" className="me-login-ajuda mb-4" role="note">
                <p className="mb-1">
                  <strong>Aluno:</strong> peça uma senha nova ao seu professor. Ela chega no e-mail do seu responsável.
                </p>
                <p className="mb-0">
                  <strong>Professor:</strong> fale com o administrador da sua escola.
                </p>
              </div>
            )}

            <button type="submit" className="btn btn-primary w-100 me-login-botao" disabled={carregando}>
              {carregando ? "Entrando..." : "Entrar"}
            </button>
          </form>

          <RodapeLegal />
        </div>
      </main>
    </div>
  );
}
