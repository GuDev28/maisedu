import { BrowserRouter, Navigate, Route, Routes, useLocation } from "react-router";
import { AuthProvider, useAuth } from "./context/AuthContext";
import { Layout } from "./components/Layout";
import { PaginaLegal } from "./components/PaginaLegal";
import { PAGINAS, caminhoInicial, podeAcessar } from "./navegacao";
import Login from "./pages/Login";
import TrocarSenha from "./pages/TrocarSenha";
import AceitarTermos from "./pages/AceitarTermos";
import TermosUso from "./pages/TermosUso";
import PoliticaPrivacidade from "./pages/PoliticaPrivacidade";
import PaginaNaoEncontrada from "./pages/PaginaNaoEncontrada";

function Carregando() {
  return (
    <div className="tela-central">
      <div className="spinner-border text-primary" role="status">
        <span className="visually-hidden">Carregando...</span>
      </div>
    </div>
  );
}

/** Etapa obrigatória antes da área logada: troca da senha provisória (RF02) ou novo aceite dos termos. */
function etapaPendente(usuario) {
  if (usuario.senhaTemporaria) {
    return "/trocar-senha";
  }
  if (usuario.termosPendentes) {
    return "/aceitar-termos";
  }
  return null;
}

/** Tela de login: quem já está autenticado volta para a página que tentou abrir (ou para a inicial). */
function SomenteVisitante({ children }) {
  const { usuario, carregandoSessao } = useAuth();
  const location = useLocation();
  if (carregandoSessao) {
    return <Carregando />;
  }
  if (usuario) {
    return <Navigate to={location.state?.de ?? "/"} replace />;
  }
  return children;
}

/** Troca de senha e aceite dos termos: só abrem quando são a etapa pendente do usuário. */
function EtapaObrigatoria({ etapa, children }) {
  const { usuario, carregandoSessao } = useAuth();
  if (carregandoSessao) {
    return <Carregando />;
  }
  if (!usuario) {
    return <Navigate to="/login" replace />;
  }
  if (etapaPendente(usuario) !== etapa) {
    return <Navigate to="/" replace />;
  }
  return children;
}

/**
 * Área logada: exige sessão e as etapas obrigatórias concluídas. É só conveniência de navegação;
 * a API recusa (401/403) qualquer requisição fora dessas regras.
 */
function AreaAutenticada() {
  const { usuario, carregandoSessao } = useAuth();
  const location = useLocation();
  if (carregandoSessao) {
    return <Carregando />;
  }
  if (!usuario) {
    return <Navigate to="/login" replace state={{ de: location.pathname }} />;
  }
  const pendente = etapaPendente(usuario);
  if (pendente) {
    return <Navigate to={pendente} replace />;
  }
  return <Layout />;
}

/** Página de outro perfil: volta para a tela inicial do usuário. */
function ExigePerfil({ pagina, children }) {
  const { usuario } = useAuth();
  if (!podeAcessar(usuario.roles, pagina)) {
    return <Navigate to="/" replace />;
  }
  return children;
}

function TelaInicial() {
  const { usuario } = useAuth();
  return <Navigate to={caminhoInicial(usuario.roles)} replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<SomenteVisitante><Login /></SomenteVisitante>} />
          {/* RF14: Termos e Política abrem sem login */}
          <Route path="/termos" element={<PaginaLegal><TermosUso /></PaginaLegal>} />
          <Route path="/privacidade" element={<PaginaLegal><PoliticaPrivacidade /></PaginaLegal>} />
          <Route path="/trocar-senha"
                 element={<EtapaObrigatoria etapa="/trocar-senha"><TrocarSenha /></EtapaObrigatoria>} />
          <Route path="/aceitar-termos"
                 element={<EtapaObrigatoria etapa="/aceitar-termos"><AceitarTermos /></EtapaObrigatoria>} />

          <Route element={<AreaAutenticada />}>
            <Route index element={<TelaInicial />} />
            {PAGINAS.map((pagina) => (
              <Route key={pagina.caminho} path={pagina.caminho}
                     element={<ExigePerfil pagina={pagina}><pagina.componente /></ExigePerfil>} />
            ))}
            <Route path="*" element={<PaginaNaoEncontrada />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
