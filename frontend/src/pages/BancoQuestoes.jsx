import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listarAprovadas } from "../services/questaoService";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { AlternativasComGabarito, EtiquetasQuestao } from "../components/EtiquetasQuestao";
import { FILTROS_VAZIOS, FiltrosBanco } from "../components/FiltrosBanco";
import { Mensagens } from "../components/Mensagens";

/**
 * Banco compartilhado (RF15): questões APROVADAS por professores de todas as escolas, com autor e
 * escola de origem. É o diferencial do MaisEdu, agora com tela própria.
 */
export default function BancoQuestoes() {
  const [filtros, setFiltros] = useState(FILTROS_VAZIOS);
  const [questoes, setQuestoes] = useState(null);
  const [buscando, setBuscando] = useState(false);
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
    buscar(FILTROS_VAZIOS);
    // só na abertura; depois, o botão Filtrar
  }, []);

  const porTopico = new Map();
  const escolas = new Set();
  for (const questao of questoes ?? []) {
    porTopico.set(questao.topico, (porTopico.get(questao.topico) ?? 0) + 1);
    if (questao.autorInstituicao) {
      escolas.add(questao.autorInstituicao);
    }
  }

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
        Questões aprovadas por 3 professores, de todas as escolas do MaisEdu. Use em atividades das suas turmas.
      </Cabecalho>

      <FiltrosBanco filtros={filtros} aoMudar={setFiltros} aoBuscar={() => buscar()} buscando={buscando} />
      <Mensagens erro={erro} />
      {!questoes && !erro && <Carregando />}

      {questoes && (
        <>
          <div className="d-flex flex-wrap gap-2 align-items-center mb-3 small">
            <span className="fw-semibold">
              {questoes.length} questão(ões){escolas.size > 0 && ` de ${escolas.size} escola(s)`}
            </span>
            {[...porTopico.entries()].map(([topico, quantidade]) => (
              <span key={topico} className="badge rounded-pill text-bg-light border">
                {topico}: {quantidade}
              </span>
            ))}
          </div>
          {questoes.length === 0 ? (
            <p className="text-secondary">Nenhuma questão aprovada com esses filtros.</p>
          ) : (
            <div className="list-group">
              {questoes.map((questao) => (
                <div key={questao.id} className="list-group-item py-3">
                  <div className="d-flex gap-2 justify-content-between align-items-start">
                    <div>
                      <div className="mb-1">{questao.enunciado}</div>
                      <EtiquetasQuestao questao={questao} />
                      <div className="small text-secondary mt-1">
                        {questao.autorNome}
                        {questao.autorInstituicao && ` · ${questao.autorInstituicao}`}
                      </div>
                    </div>
                    <button
                      type="button"
                      className="btn btn-sm btn-link flex-shrink-0"
                      aria-expanded={aberta === questao.id}
                      onClick={() => setAberta(aberta === questao.id ? null : questao.id)}
                    >
                      {aberta === questao.id ? "Ocultar" : "Ver gabarito"}
                    </button>
                  </div>
                  {aberta === questao.id && (
                    <div className="mt-2">
                      <AlternativasComGabarito alternativas={questao.alternativas} />
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </>
  );
}
