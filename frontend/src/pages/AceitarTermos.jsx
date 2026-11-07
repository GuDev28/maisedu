import { useState } from "react";
import { Link } from "react-router";
import { useAuth } from "../context/AuthContext";
import { RodapeLegal } from "../components/RodapeLegal";
import { Mensagens } from "../components/Mensagens";

const NOVA_ABA = { target: "_blank", rel: "noopener noreferrer" };

/** Exibida quando há uma versão nova dos Termos de Uso / Política de Privacidade ainda não aceita. */
export default function AceitarTermos() {
  const { aceitarTermos, logout } = useAuth();
  const [aceite, setAceite] = useState(false);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    if (!aceite) {
      setErro("É preciso aceitar os Termos de Uso e a Política de Privacidade para continuar.");
      return;
    }
    setCarregando(true);
    setErro(null);
    try {
      // Depois do aceite, a rota libera a área logada e leva à tela inicial.
      await aceitarTermos();
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
          <h1 className="h4 mb-2">Documentos atualizados</h1>
          <p className="text-body-secondary">
            Os Termos de Uso e a Política de Privacidade do MaisEdu foram atualizados. Leia as novas versões e
            confirme o aceite para continuar usando o sistema.
          </p>
          <form onSubmit={handleSubmit}>
            <div className="form-check mb-3">
              <input id="aceite" type="checkbox" className="form-check-input" checked={aceite}
                     onChange={(e) => setAceite(e.target.checked)} />
              <label className="form-check-label" htmlFor="aceite">
                Li e aceito os <Link to="/termos" {...NOVA_ABA}>Termos de Uso</Link> e a{" "}
                <Link to="/privacidade" {...NOVA_ABA}>Política de Privacidade</Link>
              </label>
            </div>
            <div className="d-flex gap-2">
              <button type="submit" className="btn btn-primary" disabled={carregando}>
                {carregando ? "Salvando..." : "Continuar"}
              </button>
              <button type="button" className="btn btn-outline-secondary" onClick={logout}>Sair</button>
            </div>
          </form>
          <Mensagens erro={erro} />
        </div>
      </div>
      <RodapeLegal novaAba />
    </div>
  );
}
