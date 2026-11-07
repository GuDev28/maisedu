import { Link } from "react-router";

/**
 * Links para os Termos de Uso e a Política de Privacidade em todas as telas (RF14, CA02).
 * Com `novaAba`, abre em outra aba (usado nas telas com formulário, para não perder o que foi digitado).
 */
export function RodapeLegal({ novaAba = false }) {
  const alvo = novaAba ? { target: "_blank", rel: "noopener noreferrer" } : {};
  return (
    <footer className="text-center small text-body-secondary mt-5 pt-3 border-top">
      <Link to="/termos" {...alvo}>Termos de Uso</Link>
      {" · "}
      <Link to="/privacidade" {...alvo}>Política de Privacidade</Link>
    </footer>
  );
}
