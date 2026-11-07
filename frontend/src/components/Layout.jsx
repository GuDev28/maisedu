import { useState } from "react";
import { Link, NavLink, Outlet } from "react-router";
import { useAuth } from "../context/AuthContext";
import { gruposDoUsuario } from "../navegacao";
import { RodapeLegal } from "./RodapeLegal";
import { PAPEIS } from "./rotulos";

/** Papel exibido ao lado do nome: o de maior responsabilidade. */
function papelPrincipal(roles) {
  for (const papel of ["ADMIN", "PROFESSOR", "ALUNO"]) {
    if (roles.includes(papel)) {
      return PAPEIS[papel];
    }
  }
  return "";
}

/**
 * Estrutura da área logada: barra superior (marca, usuário, sair), menu lateral por perfil e o conteúdo
 * da rota atual. No celular, o menu abre e fecha pelo botão da barra.
 */
export function Layout() {
  const { usuario, logout } = useAuth();
  const [menuAberto, setMenuAberto] = useState(false);
  const grupos = gruposDoUsuario(usuario.roles);

  return (
    <div className="d-flex flex-column min-vh-100">
      <nav className="navbar bg-primary shadow-sm sticky-top barra-superior" data-bs-theme="dark">
        <div className="container-fluid">
          <button type="button" className="navbar-toggler d-md-none me-2" aria-controls="menu-lateral"
                  aria-expanded={menuAberto} aria-label="Abrir menu" onClick={() => setMenuAberto((aberto) => !aberto)}>
            <span className="navbar-toggler-icon" />
          </button>
          <Link className="navbar-brand fw-semibold me-auto" to="/">MaisEdu</Link>
          <span className="navbar-text d-none d-sm-inline me-3">
            {usuario.nome}
            <span className="badge text-bg-light ms-2">{papelPrincipal(usuario.roles)}</span>
          </span>
          <button type="button" className="btn btn-outline-light btn-sm" onClick={logout}>Sair</button>
        </div>
      </nav>

      <div className="container-fluid flex-grow-1">
        <div className="row h-100">
          <aside id="menu-lateral"
                 className={`col-md-3 col-xl-2 menu-lateral bg-body border-end py-3 ${menuAberto ? "d-block" : "d-none"} d-md-block`}>
            {grupos.map((grupo) => (
              <div key={grupo.titulo} className="mb-3">
                {grupos.length > 1 && <div className="menu-titulo">{grupo.titulo}</div>}
                <nav className="nav nav-pills flex-column" aria-label={`Menu ${grupo.titulo}`}>
                  {grupo.paginas.map((pagina) => (
                    <NavLink key={pagina.caminho} to={pagina.caminho} className="nav-link"
                             onClick={() => setMenuAberto(false)}>
                      {pagina.rotulo}
                    </NavLink>
                  ))}
                </nav>
              </div>
            ))}
          </aside>

          <main className="col-md-9 col-xl-10 py-4 px-3 px-md-4">
            <Outlet />
            <RodapeLegal />
          </main>
        </div>
      </div>
    </div>
  );
}
