import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../context/useAuth";
import { CartaoNumero } from "../../components/CartaoNumero";
import { Carregando } from "../../components/Carregando";
import { Mensagens } from "../../components/Mensagens";
import { resumoDaInstituicao } from "../../services/painelService";
import { listarQuestoesPendentes } from "../../services/questaoService";

export default function InicioAdmin() {
  const { usuario } = useAuth();
  // o admin pode também lecionar (exceção); só então participa da validação de questões
  const ehProfessor = usuario.roles.includes("PROFESSOR");
  const [painel, setPainel] = useState(null);
  const [fila, setFila] = useState(null);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    resumoDaInstituicao().then(setPainel).catch((err) => setErro(err.message));
    if (ehProfessor) {
      listarQuestoesPendentes().then((lista) => setFila(lista.length)).catch(() => setFila(null));
    }
  }, [ehProfessor]);

  return (
    <>
      <div className="mb-4">
        <h1 className="me-titulo">Olá, {usuario.nome.split(" ")[0]}</h1>
        <p className="text-secondary mb-0">{painel?.instituicao ?? usuario.instituicao}: visão geral da escola</p>
      </div>
      <Mensagens erro={erro} />
      {!painel && !erro && <Carregando />}
      {painel && (
        <>
          {(painel.contasBloqueadas > 0 || painel.primeiroAcessoPendente > 0) && (
            <div className="alert alert-warning d-flex flex-wrap gap-2 align-items-center" role="status">
              <span className="me-auto">
                {painel.contasBloqueadas > 0 && (
                  <>
                    <strong>{painel.contasBloqueadas}</strong> conta(s) bloqueada(s) por tentativas de senha.{" "}
                  </>
                )}
                {painel.primeiroAcessoPendente > 0 && (
                  <>
                    <strong>{painel.primeiroAcessoPendente}</strong> pessoa(s) ainda não fizeram o primeiro acesso.
                  </>
                )}
              </span>
              <Link to="/usuarios" className="btn btn-sm btn-outline-dark">
                Ver usuários
              </Link>
            </div>
          )}

          <div className="card mb-4 border-primary">
            <div className="card-body d-flex flex-wrap align-items-center gap-3">
              <div className="me-auto">
                <div className="fw-semibold">Painel da escola</div>
                <div className="small text-secondary">
                  Acerto por ano e tópico, comparativo entre turmas, engajamento semanal e uso do banco compartilhado. Só
                  números agregados, sem identificar alunos.
                </div>
              </div>
              <Link to="/painel-escola" className="btn btn-primary">
                Abrir o painel
              </Link>
            </div>
          </div>

          <div className="row g-3 mb-4">
            <div className="col-6 col-lg-3">
              <CartaoNumero
                rotulo="Turmas ativas"
                valor={painel.turmasAtivas}
                detalhe={painel.turmasInativas > 0 ? `${painel.turmasInativas} inativa(s)` : null}
                para="/turmas"
              />
            </div>
            <div className="col-6 col-lg-3">
              <CartaoNumero rotulo="Professores" valor={painel.professores} para="/usuarios" />
            </div>
            <div className="col-6 col-lg-3">
              <CartaoNumero rotulo="Alunos" valor={painel.alunos} para="/usuarios" />
            </div>
            <div className="col-6 col-lg-3">
              <CartaoNumero rotulo="Logins nos últimos 7 dias" valor={painel.loginsUltimos7Dias} para="/auditoria" />
            </div>
          </div>

          <div className="row g-3">
            <div className="col-md-6">
              <CartaoNumero
                rotulo="Questões aprovadas no banco compartilhado"
                valor={painel.questoesNoBancoCompartilhado}
                detalhe="de todas as escolas, prontas para usar em atividades"
                para={ehProfessor ? "/questoes/banco" : "/painel-escola"}
              />
            </div>
            <div className="col-md-6">
              {ehProfessor ? (
                <CartaoNumero
                  rotulo="Questões esperando o seu voto"
                  valor={fila ?? "—"}
                  detalhe="você também é professor e participa da validação"
                  para="/questoes/avaliar"
                  tom={fila > 0 ? "warning" : ""}
                />
              ) : (
                <CartaoNumero
                  rotulo="Contas desativadas"
                  valor={painel.contasDesativadas}
                  detalhe="podem ser reativadas na tela Usuários"
                  para="/usuarios"
                />
              )}
            </div>
          </div>

          <h2 className="h6 text-secondary text-uppercase mt-5 mb-3">Atalhos</h2>
          <div className="d-flex flex-wrap gap-2">
            <Link to="/turmas" className="btn btn-primary">
              Cadastrar turma
            </Link>
            <Link to="/usuarios" className="btn btn-outline-primary">
              Cadastrar professor
            </Link>
            <Link to="/auditoria" className="btn btn-outline-primary">
              Consultar auditoria
            </Link>
          </div>
        </>
      )}
    </>
  );
}
