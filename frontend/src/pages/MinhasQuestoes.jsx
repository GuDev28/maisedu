import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useInterface } from "../context/InterfaceContext";
import { enviarParaValidacao, excluirQuestao, listarMinhasQuestoes } from "../services/questaoService";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { AlternativasComGabarito, EtiquetasQuestao } from "../components/EtiquetasQuestao";
import { EstadoVazio } from "../components/EstadoVazio";
import { Mensagens } from "../components/Mensagens";
import { STATUS_QUESTAO, formatarDataHora } from "../components/rotulos";

const VOTOS_PARA_DECIDIR = 3;
const ABAS = [
  { chave: "", rotulo: "Todas" },
  { chave: "RASCUNHO", rotulo: "Rascunhos" },
  { chave: "PENDENTE", rotulo: "Em validação" },
  { chave: "APROVADA", rotulo: "Aprovadas" },
  { chave: "REJEITADA", rotulo: "Rejeitadas" },
];

/** Placar da votação em linguagem simples ("2 de 3 aprovações · faltam 1"). */
function Placar({ questao }) {
  if (questao.status === "RASCUNHO") {
    return <span className="text-secondary small">Ainda não enviada para validação.</span>;
  }
  const aprovacoes = questao.aprovacoes ?? 0;
  const rejeicoes = questao.rejeicoes ?? 0;
  const faltam = VOTOS_PARA_DECIDIR - Math.max(aprovacoes, rejeicoes);
  return (
    <span className="small">
      <strong className="text-success">{aprovacoes}</strong> de {VOTOS_PARA_DECIDIR} aprovações ·{" "}
      <strong className="text-danger">{rejeicoes}</strong> rejeição(ões)
      {questao.status === "PENDENTE" && faltam > 0 && (
        <span className="text-secondary"> · decide com mais {faltam} voto(s) iguais</span>
      )}
    </span>
  );
}

/** Questões do professor: placar da votação, motivo das rejeições e ações do rascunho. */
export default function MinhasQuestoes() {
  const { avisar, confirmar } = useInterface();
  const [questoes, setQuestoes] = useState(null);
  const [filtro, setFiltro] = useState("");
  const [aberta, setAberta] = useState(null);
  const [erro, setErro] = useState(null);

  async function carregar() {
    try {
      setQuestoes(await listarMinhasQuestoes());
    } catch (err) {
      setErro(err.message);
    }
  }

  useEffect(() => {
    carregar();
  }, []);

  async function enviar(questao) {
    setErro(null);
    try {
      await enviarParaValidacao(questao.id);
      avisar("Questão enviada para a validação dos professores.");
      await carregar();
    } catch (err) {
      setErro(err.message);
    }
  }

  async function excluir(questao) {
    const ok = await confirmar({
      titulo: "Excluir este rascunho?",
      mensagem: `“${questao.enunciado}” será apagada. Isto não pode ser desfeito.`,
      textoConfirmar: "Excluir",
      perigo: true,
    });
    if (!ok) {
      return;
    }
    setErro(null);
    try {
      await excluirQuestao(questao.id);
      avisar("Rascunho excluído.");
      await carregar();
    } catch (err) {
      setErro(err.message);
    }
  }

  const contagem = {};
  for (const questao of questoes ?? []) {
    contagem[questao.status] = (contagem[questao.status] ?? 0) + 1;
  }
  const visiveis = (questoes ?? []).filter((q) => !filtro || q.status === filtro);

  return (
    <>
      <Cabecalho
        titulo="Minhas questões"
        acoes={
          <Link to="/questoes/nova" className="btn btn-primary">
            Nova questão
          </Link>
        }
      >
        Acompanhe a votação das suas questões. Rascunhos ainda podem ser editados ou excluídos.
      </Cabecalho>
      <Mensagens erro={erro} />
      {!questoes && !erro && <Carregando />}

      {questoes?.length === 0 && (
        <EstadoVazio
          titulo="Você ainda não criou questões"
          acao={
            <Link to="/questoes/nova" className="btn btn-primary">
              Criar a primeira questão
            </Link>
          }
        >
          Questões aprovadas entram no banco compartilhado e podem ser usadas por professores de todas as escolas.
        </EstadoVazio>
      )}

      {questoes?.length > 0 && (
        <>
          <ul className="nav nav-pills mb-3 gap-1">
            {ABAS.map((aba) => (
              <li key={aba.chave} className="nav-item">
                <button
                  type="button"
                  className={`nav-link py-1 ${filtro === aba.chave ? "active" : ""}`}
                  onClick={() => setFiltro(aba.chave)}
                >
                  {aba.rotulo}{" "}
                  <span className="opacity-75">({aba.chave ? contagem[aba.chave] ?? 0 : questoes.length})</span>
                </button>
              </li>
            ))}
          </ul>

          <div className="list-group">
            {visiveis.map((questao) => (
              <div key={questao.id} className="list-group-item py-3">
                <div className="d-flex flex-wrap gap-2 justify-content-between align-items-start">
                  <div className="flex-grow-1">
                    <div className="fw-semibold mb-1">{questao.enunciado}</div>
                    <EtiquetasQuestao questao={questao} mostrarStatus />
                    <div className="mt-2">
                      <Placar questao={questao} />
                    </div>
                  </div>
                  <div className="d-flex flex-wrap gap-1">
                    {questao.status === "RASCUNHO" && (
                      <>
                        <button type="button" className="btn btn-sm btn-primary" onClick={() => enviar(questao)}>
                          Enviar para validação
                        </button>
                        <Link to={`/questoes/${questao.id}/editar`} className="btn btn-sm btn-outline-secondary">
                          Editar
                        </Link>
                        <button type="button" className="btn btn-sm btn-outline-danger" onClick={() => excluir(questao)}>
                          Excluir
                        </button>
                      </>
                    )}
                    <button
                      type="button"
                      className="btn btn-sm btn-link"
                      aria-expanded={aberta === questao.id}
                      onClick={() => setAberta(aberta === questao.id ? null : questao.id)}
                    >
                      {aberta === questao.id ? "Ocultar" : "Ver alternativas"}
                    </button>
                  </div>
                </div>

                {questao.status === "REJEITADA" && questao.motivosRejeicao?.length > 0 && (
                  <div className="alert alert-danger py-2 mt-3 mb-0">
                    <div className="fw-semibold small mb-1">Por que foi rejeitada</div>
                    <ul className="mb-0 small ps-3">
                      {questao.motivosRejeicao.map((motivo, indice) => (
                        <li key={indice}>{motivo}</li>
                      ))}
                    </ul>
                    <div className="small mt-1">Corrija e cadastre de novo como uma questão nova.</div>
                  </div>
                )}
                {questao.status === "PENDENTE" && questao.motivosRejeicao?.length > 0 && (
                  <div className="alert alert-warning py-2 mt-3 mb-0 small">
                    <span className="fw-semibold">Comentários de quem rejeitou: </span>
                    {questao.motivosRejeicao.join(" · ")}
                  </div>
                )}

                {aberta === questao.id && (
                  <div className="mt-3">
                    <AlternativasComGabarito alternativas={questao.alternativas} />
                    <div className="small text-secondary mt-2">Criada em {formatarDataHora(questao.criadoEm)}</div>
                  </div>
                )}
              </div>
            ))}
            {visiveis.length === 0 && (
              <div className="list-group-item text-secondary">Nenhuma questão com o status {STATUS_QUESTAO[filtro]?.rotulo.toLowerCase()}.</div>
            )}
          </div>
        </>
      )}
    </>
  );
}
