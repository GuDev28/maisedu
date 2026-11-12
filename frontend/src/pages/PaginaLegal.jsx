import { Link, useNavigate } from "react-router-dom";
import { Logo } from "../components/Logo";
import TermosUso from "./TermosUso";
import PoliticaPrivacidade from "./PoliticaPrivacidade";

/**
 * Termos de Uso e Política de Privacidade. Fora do sistema (login, primeiro acesso) a página abre
 * sozinha, com "Voltar"; logado, abre dentro do menu.
 */
export function PaginaLegal({ documento, dentroDoMenu = false }) {
  const navigate = useNavigate();
  const Documento = documento === "termos" ? TermosUso : PoliticaPrivacidade;

  if (dentroDoMenu) {
    return (
      <div className="me-largura-md">
        <Documento />
      </div>
    );
  }

  return (
    <div className="container py-4 me-largura-md">
      <div className="d-flex align-items-center justify-content-between mb-4">
        <Link to="/" className="text-decoration-none">
          <Logo />
        </Link>
        <button type="button" className="btn btn-outline-secondary btn-sm" onClick={() => navigate(-1)}>
          ← Voltar
        </button>
      </div>
      <div className="card">
        <div className="card-body p-4">
          <Documento />
        </div>
      </div>
    </div>
  );
}
