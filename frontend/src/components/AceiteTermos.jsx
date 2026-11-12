import { Link } from "react-router-dom";

/** Caixa "Li e aceito" com os links para os documentos (abrem em outra aba para não perder o formulário). */
export function AceiteTermos({ marcado, aoMudar }) {
  return (
    <div className="form-check mb-3">
      <input
        id="aceite-termos"
        type="checkbox"
        className="form-check-input"
        checked={marcado}
        onChange={(e) => aoMudar(e.target.checked)}
        required
      />
      <label htmlFor="aceite-termos" className="form-check-label">
        Li e aceito os{" "}
        <Link to="/termos" target="_blank" rel="noopener">
          Termos de Uso
        </Link>{" "}
        e a{" "}
        <Link to="/privacidade" target="_blank" rel="noopener">
          Política de Privacidade
        </Link>
      </label>
    </div>
  );
}
