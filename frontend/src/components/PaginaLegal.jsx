import { Link, useNavigate } from "react-router";

/**
 * Moldura dos Termos de Uso e da Política de Privacidade (RF14): abrem com ou sem login, inclusive em
 * outra aba a partir da troca de senha, sem perder o que o usuário já digitou.
 */
export function PaginaLegal({ children }) {
  const navigate = useNavigate();

  function voltar() {
    if (window.history.length > 1) {
      navigate(-1);
    } else {
      navigate("/");
    }
  }

  return (
    <div className="min-vh-100">
      <nav className="navbar bg-primary shadow-sm" data-bs-theme="dark">
        <div className="container">
          <Link className="navbar-brand fw-semibold" to="/">MaisEdu</Link>
        </div>
      </nav>
      <div className="container py-4" style={{ maxWidth: 860 }}>
        <button type="button" className="btn btn-link px-0 mb-2" onClick={voltar}>← Voltar</button>
        <div className="card shadow-sm">
          <div className="card-body p-4">{children}</div>
        </div>
      </div>
    </div>
  );
}
