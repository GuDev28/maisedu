import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listarAprovadas } from "../services/questaoService";
import { Autor } from "../components/Avatar";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { AlternativasComGabarito, EtiquetasQuestao } from "../components/EtiquetasQuestao";
import { EstadoVazio } from "../components/EstadoVazio";
import { FiltrosBanco } from "../components/FiltrosBanco";
import { Icone } from "../components/Icone";
import { Mensagens } from "../components/Mensagens";
import { FILTROS_VAZIOS } from "../components/rotulos";

const ORDEM_DO_NIVEL = { FACIL: 1, MEDIO: 2, DIFICIL: 3 };

const ORDENACOES = {
  recentes: {
    rotulo: "Mais recentes",
    comparar: (a, b) => String(b.criadoEm ?? "").localeCompare(String(a.criadoEm ?? "")) || b.id - a.id,
  },
  topico: {
    rotulo: "Tópico (A–Z)",
    comparar: (a, b) => a.topico.localeCompare(b.topico, "pt-BR") || ORDEM_DO_NIVEL[a.nivelDificuldade] - ORDEM_DO_NIVEL[b.nivelDificuldade],
  },
  nivel: {
    rotulo: "Nível (fácil → difícil)",
    comparar: (a, b) => ORDEM_DO_NIVEL[a.nivelDificuldade] - ORDEM_DO_NIVEL[b.nivelDificuldade] || a.topico.localeCompare(b.topico, "pt-BR"),
  },
};

/** Busca sem acento e sem diferença de maiúsculas ("fracao" acha "Frações"). */
function normalizar(texto) {
  return String(texto ?? "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase();
}

/** Números do banco inteiro (a primeira busca, sem filtros), para o topo da página. */
function panoramaDo(questoes) {
  const escolas = new Set();
  const autores = new Set();
  const topicos = new Set();
  for (const questao of questoes) {
    if (questao.autorInstituicao) {
      escolas.add(questao.autorInstituicao);
    }
    autores.add(questao.autorNome);
    topicos.add(questao.topico);
  }
  return { questoes: questoes.length, escolas: escolas.size, autores: autores.size, topicos: topicos.size };
}

function Panorama({ panorama }) {
  const numeros = [
    { valor: panorama.questoes, rotulo: "questões aprovadas" },
    { valor: panorama.escolas, rotulo: panorama.escolas === 1 ? "escola contribuindo" : "escolas contribuindo" },
    { valor: panorama.autores, rotulo: panorama.autores === 1 ? "professor autor" : "professores autores" },
    { valor: panorama.topicos, rotulo: "tópicos cobertos" },
  ];
  return (
    <section className="me-panorama mb-4" aria-label="O banco em números">
      <div className="me-panorama-texto">
        <span className="me-panorama-icone">
          <Icone nome="banco" tamanho={26} />
        </span>
        <div>
          <h2 className="h5 mb-1">Um banco feito em rede</h2>
          <p className="mb-0">Cada questão foi aprovada por 3 professores antes de entrar aqui.</p>
        </div>
      </div>
      <dl className="me-panorama-numeros">
        {numeros.map((numero) => (
          <div key={numero.rotulo}>
            <dt>{numero.rotulo}</dt>
            <dd>{numero.valor.toLocaleString("pt-BR")}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

function CartaoQuestao({ questao, aberta, aoAlternar }) {
  const idAlternativas = `alternativas-${questao.id}`;
  return (
    <article className="card me-questao">
      <div className="card-body">
        <EtiquetasQuestao questao={questao} />
        <p className="me-questao-enunciado">{questao.enunciado}</p>
        {aberta && (
          <div id={idAlternativas} className="mb-3">
            <AlternativasComGabarito alternativas={questao.alternativas} />
          </div>
        )}
        <div className="me-questao-rodape">
          <Autor nome={questao.autorNome} escola={questao.autorInstituicao} />
          <button
            type="button"
            className={`btn btn-sm me-botao-gabarito ${aberta ? "aberto" : ""}`}
            aria-expanded={aberta}
            aria-controls={idAlternativas}
            onClick={aoAlternar}
          >
            {aberta ? "Ocultar gabarito" : "Ver gabarito"}
            <Icone nome="abaixo" tamanho={16} />
          </button>
        </div>
      </div>
    </article>
  );
}

/**
 * Banco compartilhado (RF15): questões APROVADAS por professores de todas as escolas, com autor e
 * escola de origem. No topo, o banco em números; os filtros de tópico, nível e ano vão ao servidor, e
 * a busca por texto e a ordenação acontecem aqui mesmo, no que já veio.
 */
export default function BancoQuestoes() {
  const [filtros, setFiltros] = useState(FILTROS_VAZIOS);
  const [questoes, setQuestoes] = useState(null);
  const [panorama, setPanorama] = useState(null);
  const [busca, setBusca] = useState("");
  const [ordem, setOrdem] = useState("recentes");
  const [buscando, setBuscando] = useState(true);
  const [aberta, setAberta] = useState(null);
  const [erro, setErro] = useState(null);

  async function buscar(filtrosDaBusca = filtros) {
    setBuscando(true);
    setErro(null);
    try {
      setQuestoes(await listarAprovadas(filtrosDaBusca));
    } catch (err) {
      setErro(err.message);
    } finally {
      setBuscando(false);
    }
  }

  useEffect(() => {
    // na abertura, a busca sem filtros também dá os números do banco inteiro; depois, o botão Filtrar
    listarAprovadas(FILTROS_VAZIOS)
      .then((lista) => {
        setQuestoes(lista);
        setPanorama(panoramaDo(lista));
      })
      .catch((err) => setErro(err.message))
      .finally(() => setBuscando(false));
  }, []);

  function limparTudo() {
    setFiltros(FILTROS_VAZIOS);
    setBusca("");
    buscar(FILTROS_VAZIOS);
  }

  const termo = normalizar(busca.trim());
  const visiveis = (questoes ?? [])
    .filter(
      (questao) =>
        !termo ||
        normalizar(questao.enunciado).includes(termo) ||
        normalizar(questao.autorNome).includes(termo) ||
        normalizar(questao.autorInstituicao).includes(termo)
    )
    .sort(ORDENACOES[ordem].comparar);
  const filtrando = Boolean(termo || filtros.topicoId || filtros.nivel || filtros.anoEscolar);

  return (
    <>
      <Cabecalho
        titulo="Banco de questões"
        acoes={
          <>
            <Link to="/questoes/nova" className="btn btn-outline-primary">
              Criar questão
            </Link>
            <Link to="/atividades" className="btn btn-primary">
              Montar atividade
            </Link>
          </>
        }
      >
        Questões aprovadas por professores de todas as escolas do MaisEdu. Use nas atividades das suas turmas.
      </Cabecalho>

      {panorama && panorama.questoes > 0 && <Panorama panorama={panorama} />}

      <div className="card me-filtros mb-4">
        <div className="card-body">
          <label htmlFor="busca-banco" className="visually-hidden">
            Buscar no enunciado, no autor ou na escola
          </label>
          <div className="me-campo-busca mb-3">
            <Icone nome="busca" tamanho={18} />
            <input
              id="busca-banco"
              type="search"
              className="form-control"
              placeholder="Buscar no enunciado, no autor ou na escola"
              value={busca}
              onChange={(e) => setBusca(e.target.value)}
            />
          </div>
          <FiltrosBanco filtros={filtros} aoMudar={setFiltros} aoBuscar={() => buscar()} buscando={buscando} className="mb-0" />
        </div>
      </div>

      <Mensagens erro={erro} />
      {!questoes && !erro && <Carregando />}

      {questoes && (
        <>
          <div className="d-flex flex-wrap gap-2 align-items-center justify-content-between mb-3">
            <p className="mb-0" role="status">
              <strong>{visiveis.length}</strong> {visiveis.length === 1 ? "questão encontrada" : "questões encontradas"}
              {filtrando && (
                <button type="button" className="btn btn-link btn-sm align-baseline ms-1" onClick={limparTudo}>
                  Limpar filtros
                </button>
              )}
            </p>
            <div className="d-flex align-items-center gap-2">
              <label htmlFor="ordem-banco" className="small text-secondary text-nowrap">
                Ordenar por
              </label>
              <select id="ordem-banco" className="form-select form-select-sm" value={ordem} onChange={(e) => setOrdem(e.target.value)}>
                {Object.entries(ORDENACOES).map(([chave, ordenacao]) => (
                  <option key={chave} value={chave}>
                    {ordenacao.rotulo}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {visiveis.length === 0 ? (
            <EstadoVazio
              titulo="Nenhuma questão aprovada com esses filtros"
              acao={
                <button type="button" className="btn btn-outline-primary" onClick={limparTudo}>
                  Limpar filtros
                </button>
              }
            >
              Tente outro tópico, nível ou ano, ou busque por outra palavra.
            </EstadoVazio>
          ) : (
            <div className="row g-3 align-items-start">
              {visiveis.map((questao) => (
                <div key={questao.id} className="col-12 col-xl-6">
                  <CartaoQuestao
                    questao={questao}
                    aberta={aberta === questao.id}
                    aoAlternar={() => setAberta(aberta === questao.id ? null : questao.id)}
                  />
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </>
  );
}
