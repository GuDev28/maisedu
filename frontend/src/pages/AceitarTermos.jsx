import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { RodapeLegal } from "../components/RodapeLegal";
import { Mensagens } from "../components/Mensagens";

/** Exibida quando há uma versão nova dos Termos de Uso / Política de Privacidade ainda não aceita. */
export default function AceitarTermos({ onAbrirPaginaLegal }) {
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
      await aceitarTermos();
    } catch (err) {
      setErro(err.message);
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div className="pagina pagina-estreita" style={{ marginTop: "4rem" }}>
      <h2>Documentos atualizados</h2>
      <p>
        Os Termos de Uso e a Política de Privacidade do MaisEdu foram atualizados. Leia as novas versões e
        confirme o aceite para continuar usando o sistema.
      </p>
      <form onSubmit={handleSubmit}>
        <p>
          <input id="aceite" type="checkbox" checked={aceite} onChange={(e) => setAceite(e.target.checked)} />{" "}
          <label htmlFor="aceite">Li e aceito os</label>{" "}
          <button type="button" onClick={() => onAbrirPaginaLegal("termos")}>Termos de Uso</button> e a{" "}
          <button type="button" onClick={() => onAbrirPaginaLegal("privacidade")}>Política de Privacidade</button>
        </p>
        <div className="linha-acoes">
          <button type="submit" disabled={carregando}>{carregando ? "Salvando..." : "Continuar"}</button>
          <button type="button" onClick={logout}>Sair</button>
        </div>
      </form>
      <Mensagens erro={erro} />
      <RodapeLegal onAbrirPaginaLegal={onAbrirPaginaLegal} />
    </div>
  );
}
