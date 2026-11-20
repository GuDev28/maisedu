import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useInterface } from "../context/useInterface";
import { enviarParaValidacao, excluirQuestao, listarMinhasQuestoes } from "../services/questaoService";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { AlternativasComGabarito, EtiquetasQuestao } from "../components/EtiquetasQuestao";
import { EstadoVazio } from "../components/EstadoVazio";
import { Icone } from "../components/Icone";
import { Mensagens } from "../components/Mensagens";
import { STATUS_QUESTAO, formatarData } from "../components/rotulos";

const VOTOS_PARA_DECIDIR = 3;
const ABAS = [
  { chave: "", rotulo: "Todas" },
  { chave: "RASCUNHO", rotulo: "Rascunhos" },
  { chave: "PENDENTE", rotulo: "Em validação" },
  { chave: "APROVADA", rotulo: "Aprovadas" },
  { chave: "REJEITADA", rotulo: "Rejeitadas" },
];

/** Três casas por lado: cada voto preenche uma; três iguais decidem a questão. */
function Casas({ quantidade, tipo }) {
  return (
    <span className="me-casas" aria-hidden="true">
      {[1, 2, 3].map((casa) => (
        <span key={casa} className={casa <= quantidade ? `me-casa-${tipo}` : ""} />
      ))}
    </span>
  );
}

/** Placar da votação: as casas mostram o andamento e a frase diz o mesmo em palavras. */
function Placar({ questao }) {
  const aprovacoes = questao.aprovacoes ?? 0;
  const rejeicoes = questao.rejeicoes ?? 0;
  const faltam = VOTOS_PARA_DECIDIR - Math.max(aprovacoes, rejeicoes);
  let resumo;
  if (questao.status === "APROVADA") {
    resumo = "Aprovada: já está no banco compartilhado.";
  } else if (questao.status === "REJEITADA") {
    resumo = "Rejeitada pelos avaliadores.";
  } else if (faltam > 0) {
    resumo = `Decide com mais ${faltam} ${faltam === 1 ? "voto igual" : "votos iguais"}.`;
  } else {
    resumo = "Aguardando a decisão.";
  }
  return (
    <div className="me-placar">
      <div className="me-placar-lado">
        <Casas quantidade={aprovacoes} tipo="sim" />
        <span>
          <strong>{aprovacoes}</strong> de {VOTOS_PARA_DECIDIR} aprovações
        </span>
      </div>
      <div className="me-placar-lado">
        <Casas quantidade={rejeicoes} tipo="nao" />
        <span>
          <strong>{rejeicoes}</strong> {rejeicoes === 1 ? "rejeição" : "rejeições"}
        </span>
      </div>
      <span className="me-placar-resumo">{resumo}</span>
    </div>
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
    listarMinhasQuestoes()
      .then(setQuestoes)
      .catch((err) => setErro(err.message));
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
          <div className="me-contadores mb-4" role="group" aria-label="Filtrar por status">
            {ABAS.map((aba) => (
              <button
                key={aba.chave}
                type="button"
                className={`me-contador me-contador-${(aba.chave || "todas").toLowerCase()} ${filtro === aba.chave ? "ativo" : ""}`}
                aria-pressed={filtro === aba.chave}
                onClick={() => setFiltro(aba.chave)}
              >
                <span className="me-contador-numero">{aba.chave ? contagem[aba.chave] ?? 0 : questoes.length}</span>
                <span className="me-contador-rotulo">{aba.rotulo}</span>
              </button>
            ))}
          </div>

          <div className="d-flex flex-column gap-3">
            {visiveis.map((questao) => {
              const idAlternativas = `alternativas-${questao.id}`;
              return (
                <article key={questao.id} className={`card me-questao me-questao-${questao.status.toLowerCase()}`}>
                  <div className="card-body">
                    <div className="d-flex flex-wrap gap-2 justify-content-between align-items-center">
                      <EtiquetasQuestao questao={questao} mostrarStatus />
                      <span className="small text-secondary d-inline-flex align-items-center gap-1">
                        <Icone nome="relogio" tamanho={15} />
                        Criada em {formatarData(questao.criadoEm)}
                      </span>
                    </div>
                    <p className="me-questao-enunciado">{questao.enunciado}</p>

                    {questao.status === "RASCUNHO" ? (
                      <p className="small text-secondary mb-0">Ainda não enviada: só você vê este rascunho.</p>
                    ) : (
                      <Placar questao={questao} />
                    )}

                    {questao.status === "REJEITADA" && questao.motivosRejeicao?.length > 0 && (
                      <div className="me-aviso me-aviso-erro mt-3">
                        <div className="fw-semibold mb-1">Por que foi rejeitada</div>
                        <ul className="mb-1 ps-3">
                          {questao.motivosRejeicao.map((motivo, indice) => (
                            <li key={indice}>{motivo}</li>
                          ))}
                        </ul>
                        <div className="text-secondary">Corrija e cadastre de novo como uma questão nova.</div>
                      </div>
                    )}
                    {questao.status === "PENDENTE" && questao.motivosRejeicao?.length > 0 && (
                      <div className="me-aviso me-aviso-atencao mt-3">
                        <span className="fw-semibold">Comentários de quem rejeitou: </span>
                        {questao.motivosRejeicao.join(" · ")}
                      </div>
                    )}

                    {aberta === questao.id && (
                      <div id={idAlternativas} className="mt-3">
                        <AlternativasComGabarito alternativas={questao.alternativas} />
                      </div>
                    )}

                    <div className="me-questao-acoes">
                      <button
                        type="button"
                        className={`btn btn-sm me-botao-gabarito ${aberta === questao.id ? "aberto" : ""}`}
                        aria-expanded={aberta === questao.id}
                        aria-controls={idAlternativas}
                        onClick={() => setAberta(aberta === questao.id ? null : questao.id)}
                      >
                        {aberta === questao.id ? "Ocultar alternativas" : "Ver alternativas"}
                        <Icone nome="abaixo" tamanho={16} />
                      </button>
                      {questao.status === "RASCUNHO" && (
                        <div className="d-flex flex-wrap gap-2">
                          <button type="button" className="btn btn-sm btn-outline-danger me-botao-icone" onClick={() => excluir(questao)}>
                            <Icone nome="lixeira" tamanho={16} />
                            Excluir
                          </button>
                          <Link to={`/questoes/${questao.id}/editar`} className="btn btn-sm btn-outline-secondary me-botao-icone">
                            <Icone nome="editar" tamanho={16} />
                            Editar
                          </Link>
                          <button type="button" className="btn btn-sm btn-primary me-botao-icone" onClick={() => enviar(questao)}>
                            <Icone nome="enviar" tamanho={16} />
                            Enviar para validação
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                </article>
              );
            })}
            {visiveis.length === 0 && (
              <p className="text-secondary">Nenhuma questão com o status {STATUS_QUESTAO[filtro]?.rotulo.toLowerCase()}.</p>
            )}
          </div>
        </>
      )}
    </>
  );
}
