import { useEffect, useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { MENU_RODAPE, secoesDoMenu } from "../navegacao";
import { listarQuestoesPendentes } from "../services/questaoService";
import { Icone } from "./Icone";
import { Logo } from "./Logo";
import { papelPrincipal } from "./rotulos";

/** Disparado depois de um voto, para o contador de "Avaliar questões" se atualizar na hora. */
export const EVENTO_FILA_ATUALIZADA = "maisedu:fila-atualizada";

function useFilaDeAvaliacao(ativo) {
  const [quantidade, setQuantidade] = useState(null);
  const { pathname } = useLocation();

  useEffect(() => {
    if (!ativo) {
      return undefined;
    }
    let vivo = true;
    function carregar() {
      listarQuestoesPendentes()
        .then((fila) => vivo && setQuantidade(fila.length))
        .catch(() => vivo && setQuantidade(null));
    }
    carregar();
    window.addEventListener(EVENTO_FILA_ATUALIZADA, carregar);
    return () => {
      vivo = false;
      window.removeEventListener(EVENTO_FILA_ATUALIZADA, carregar);
    };
  }, [ativo, pathname]);

  return quantidade;
}

function ItemMenu({ item, contador, aoNavegar }) {
  return (
    <li className="nav-item">
      <NavLink to={item.rota} end={item.rota === "/"} className="nav-link" onClick={aoNavegar}>
        <Icone nome={item.icone} />
        <span className="flex-grow-1">{item.label}</span>
        {contador > 0 && (
          <span className="badge rounded-pill text-bg-warning" title="Questões esperando o seu voto">
            {contador}
            <span className="visually-hidden"> questões esperando o seu voto</span>
          </span>
        )}
      </NavLink>
    </li>
  );
}

/**
 * Estrutura das telas logadas: menu lateral fixo no computador e gaveta no celular
 * (offcanvas-lg do Bootstrap, controlado pelo React), com a página da rota ao lado.
 */
export default function Layout() {
  const { usuario, logout } = useAuth();
  const [menuAberto, setMenuAberto] = useState(false);
  const { pathname } = useLocation();
  const secoes = secoesDoMenu(usuario.roles);
  const fila = useFilaDeAvaliacao(usuario.roles.includes("PROFESSOR"));

  // trocar de página fecha a gaveta no celular
  useEffect(() => {
    setMenuAberto(false);
  }, [pathname]);

  const fechar = () => setMenuAberto(false);

  return (
    <div className="me-app">
      <a href="#conteudo" className="visually-hidden-focusable position-absolute top-0 start-0 p-2 bg-white">
        Pular para o conteúdo
      </a>

      <header className="me-barra-topo d-flex d-lg-none align-items-center justify-content-between px-3 py-2 sticky-top">
        <Link to="/" className="text-decoration-none">
          <Logo tamanho={28} />
        </Link>
        <button
          type="button"
          className="btn btn-outline-secondary btn-sm"
          aria-controls="menu-lateral"
          aria-expanded={menuAberto}
          onClick={() => setMenuAberto(true)}
        >
          <Icone nome="menu" />
          <span className="visually-hidden">Abrir menu</span>
        </button>
      </header>

      <nav
        id="menu-lateral"
        className={`me-menu offcanvas-lg offcanvas-start ${menuAberto ? "show" : ""}`}
        tabIndex="-1"
        aria-label="Menu principal"
      >
        <div className="offcanvas-body">
          <div className="me-menu-topo">
            <div className="d-flex align-items-center justify-content-between">
              <Link to="/" className="text-decoration-none" onClick={fechar}>
                <Logo />
              </Link>
              <button type="button" className="btn-close d-lg-none" aria-label="Fechar menu" onClick={fechar} />
            </div>
            <div className="me-menu-usuario mt-3">
              <div className="fw-semibold text-truncate" title={usuario.nome}>
                {usuario.nome}
              </div>
              <div className="text-secondary text-truncate" title={usuario.instituicao}>
                {papelPrincipal(usuario.roles)}
                {usuario.instituicao ? ` · ${usuario.instituicao}` : ""}
              </div>
            </div>
          </div>

          {secoes.map((secao) => (
            <div key={secao.titulo ?? "inicio"}>
              {secao.titulo && <div className="me-menu-secao">{secao.titulo}</div>}
              <ul className={`nav flex-column ${secao.titulo ? "" : "mt-2"}`}>
                {secao.itens.map((item) => (
                  <ItemMenu
                    key={item.rota + item.label}
                    item={item}
                    contador={item.contador === "fila" ? fila : null}
                    aoNavegar={fechar}
                  />
                ))}
              </ul>
            </div>
          ))}

          <div className="me-menu-rodape">
            <ul className="nav flex-column">
              {MENU_RODAPE.map((item) => (
                <ItemMenu key={item.rota} item={item} aoNavegar={fechar} />
              ))}
              <li className="nav-item">
                <button type="button" className="nav-link w-100 border-0 bg-transparent" onClick={logout}>
                  <Icone nome="sair" />
                  <span>Sair</span>
                </button>
              </li>
            </ul>
          </div>
        </div>
      </nav>
      {menuAberto && <div className="offcanvas-backdrop fade show d-lg-none" onClick={fechar} />}

      <main id="conteudo" className="me-conteudo">
        <div className="me-pagina">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
