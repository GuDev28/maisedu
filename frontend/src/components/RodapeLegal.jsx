import { Link } from "react-router-dom";

/** Links para os documentos legais nas telas fora do menu (login, primeiro acesso, novo aceite). */
export function RodapeLegal() {
  return (
    <p className="text-center small text-secondary mt-4 mb-0">
      <Link to="/termos">Termos de Uso</Link>
      {" · "}
      <Link to="/privacidade">Política de Privacidade</Link>
    </p>
  );
}
