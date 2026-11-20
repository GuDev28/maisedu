import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { useAuth } from "./context/useAuth";
import Layout from "./components/Layout";
import { Carregando } from "./components/Carregando";
import Login from "./pages/Login";
import PrimeiroAcesso from "./pages/PrimeiroAcesso";
import AceitarTermos from "./pages/AceitarTermos";
import { PaginaLegal } from "./pages/PaginaLegal";
import Inicio from "./pages/Inicio";
import Turmas from "./pages/Turmas";
import Turma from "./pages/Turma";
import Usuarios from "./pages/Usuarios";
import Auditoria from "./pages/Auditoria";
import PainelEscola from "./pages/PainelEscola";
import Atividades from "./pages/Atividades";
import Desempenho from "./pages/Desempenho";
import BancoQuestoes from "./pages/BancoQuestoes";
import MinhasQuestoes from "./pages/MinhasQuestoes";
import FormQuestao from "./pages/FormQuestao";
import AvaliarQuestoes from "./pages/AvaliarQuestoes";
import MinhasAtividades from "./pages/MinhasAtividades";
import FolhaAtividade from "./pages/FolhaAtividade";
import Praticar from "./pages/Praticar";
import MeuDesempenho from "./pages/MeuDesempenho";
import MinhaConta from "./pages/MinhaConta";
import ComoFunciona from "./pages/ComoFunciona";
import NaoEncontrada from "./pages/NaoEncontrada";

/** Só renderiza a rota para quem tem um dos papéis; os outros voltam para o Início. */
function SoPara({ roles, children }) {
  const { usuario } = useAuth();
  if (!roles.some((role) => usuario.roles.includes(role))) {
    return <Navigate to="/" replace />;
  }
  return children;
}

/**
 * Rotas do MaisEdu (React Router). Termos e Privacidade abrem sempre, logado ou não, e o botão
 * Voltar do navegador funciona. Antes do menu, a sessão passa por dois portões: troca da senha
 * provisória e aceite de uma versão nova dos Termos/Política.
 */
export default function App() {
  const { usuario, carregandoSessao } = useAuth();
  const location = useLocation();

  if (carregandoSessao) {
    return (
      <div className="me-tela-publica">
        <Carregando texto="Abrindo o MaisEdu..." />
      </div>
    );
  }

  const rotasLegais = (
    <>
      <Route path="/termos" element={<PaginaLegal documento="termos" />} />
      <Route path="/privacidade" element={<PaginaLegal documento="privacidade" />} />
    </>
  );

  if (!usuario) {
    return (
      <Routes>
        {rotasLegais}
        <Route path="/login" element={<Login />} />
        <Route path="*" element={<Navigate to="/login" replace state={{ de: location.pathname }} />} />
      </Routes>
    );
  }

  if (usuario.senhaTemporaria || usuario.termosPendentes) {
    const Portao = usuario.senhaTemporaria ? PrimeiroAcesso : AceitarTermos;
    return (
      <Routes>
        {rotasLegais}
        <Route path="*" element={<Portao />} />
      </Routes>
    );
  }

  const professor = ["PROFESSOR"];
  const admin = ["ADMIN"];
  const aluno = ["ALUNO"];
  const gestaoDeTurmas = ["ADMIN", "PROFESSOR"];

  return (
    <Routes>
      <Route path="/login" element={<Navigate to={location.state?.de ?? "/"} replace />} />
      <Route element={<Layout />}>
        <Route index element={<Inicio />} />

        <Route path="/turmas" element={<SoPara roles={gestaoDeTurmas}><Turmas /></SoPara>} />
        <Route path="/turmas/:turmaId/:aba?" element={<SoPara roles={gestaoDeTurmas}><Turma /></SoPara>} />
        <Route path="/usuarios" element={<SoPara roles={admin}><Usuarios /></SoPara>} />
        <Route path="/auditoria" element={<SoPara roles={admin}><Auditoria /></SoPara>} />
        <Route path="/painel-escola" element={<SoPara roles={admin}><PainelEscola /></SoPara>} />

        <Route path="/atividades" element={<SoPara roles={professor}><Atividades /></SoPara>} />
        <Route path="/desempenho" element={<SoPara roles={professor}><Desempenho /></SoPara>} />
        <Route path="/questoes/banco" element={<SoPara roles={professor}><BancoQuestoes /></SoPara>} />
        <Route path="/questoes/minhas" element={<SoPara roles={professor}><MinhasQuestoes /></SoPara>} />
        <Route path="/questoes/nova" element={<SoPara roles={professor}><FormQuestao /></SoPara>} />
        <Route path="/questoes/:questaoId/editar" element={<SoPara roles={professor}><FormQuestao /></SoPara>} />
        <Route path="/questoes/avaliar" element={<SoPara roles={professor}><AvaliarQuestoes /></SoPara>} />

        <Route path="/minhas-atividades" element={<SoPara roles={aluno}><MinhasAtividades /></SoPara>} />
        <Route path="/minhas-atividades/:atividadeAlunoId" element={<SoPara roles={aluno}><FolhaAtividade /></SoPara>} />
        <Route path="/praticar" element={<SoPara roles={aluno}><Praticar /></SoPara>} />
        <Route path="/meu-desempenho" element={<SoPara roles={aluno}><MeuDesempenho /></SoPara>} />

        <Route path="/conta" element={<MinhaConta />} />
        <Route path="/como-funciona" element={<ComoFunciona />} />
        <Route path="/termos" element={<PaginaLegal documento="termos" dentroDoMenu />} />
        <Route path="/privacidade" element={<PaginaLegal documento="privacidade" dentroDoMenu />} />
        <Route path="*" element={<NaoEncontrada />} />
      </Route>
    </Routes>
  );
}
