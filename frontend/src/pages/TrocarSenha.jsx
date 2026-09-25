import { useState } from "react";
import { useAuth } from "../context/AuthContext";

export default function TrocarSenha() {
  const { trocarSenha } = useAuth();
  const [senhaAtual, setSenhaAtual] = useState("");
  const [novaSenha, setNovaSenha] = useState("");
  const [confirmacao, setConfirmacao] = useState("");
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setErro(null);

    if (novaSenha !== confirmacao) {
      setErro("A confirmação não é igual à nova senha.");
      return;
    }

    setCarregando(true);
    try {
      await trocarSenha(senhaAtual, novaSenha);
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div style={{ maxWidth: 360, margin: "4rem auto" }}>
      <h2>Troque sua senha provisória</h2>
      <p>Por segurança, é preciso definir uma senha própria antes de continuar.</p>

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 12 }}>
          <label>
            Senha provisória atual:{" "}
            <input
              type="password"
              value={senhaAtual}
              onChange={(e) => setSenhaAtual(e.target.value)}
              autoFocus
              required
            />
          </label>
        </div>

        <div style={{ marginBottom: 12 }}>
          <label>
            Nova senha (mín. 8 caracteres):{" "}
            <input
              type="password"
              value={novaSenha}
              onChange={(e) => setNovaSenha(e.target.value)}
              minLength={8}
              required
            />
          </label>
        </div>

        <div style={{ marginBottom: 12 }}>
          <label>
            Confirme a nova senha:{" "}
            <input
              type="password"
              value={confirmacao}
              onChange={(e) => setConfirmacao(e.target.value)}
              minLength={8}
              required
            />
          </label>
        </div>

        <button type="submit" disabled={carregando}>
          {carregando ? "Salvando..." : "Trocar senha"}
        </button>
      </form>

      {erro && <p style={{ color: "red" }}>{erro}</p>}
    </div>
  );
}
