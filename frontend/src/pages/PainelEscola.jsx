import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { painelDaEscola } from "../services/painelService";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { CartaoNumero } from "../components/CartaoNumero";
import { EstadoVazio } from "../components/EstadoVazio";
import { Dica, LegendaCalor } from "../components/MapaDeCalor";
import { classeCalor } from "../components/escalaCalor";
import { useDica } from "../components/useDica";
import { Mensagens } from "../components/Mensagens";
import { ANOS_ESCOLARES, formatarData, formatarPercentual, rotuloAno } from "../components/rotulos";
import { useLargura } from "../components/useLargura";

/** Medidas do gráfico de engajamento: uma por vez, sempre no mesmo eixo (nunca dois eixos). */
const MEDIDAS_ENGAJAMENTO = [
  { chave: "alunosAtivos", rotulo: "Alunos que responderam", curto: "alunos" },
  { chave: "respostas", rotulo: "Respostas", curto: "respostas" },
  { chave: "praticasIniciadas", rotulo: "Práticas iniciadas", curto: "práticas" },
  { chave: "atividadesCriadas", rotulo: "Atividades criadas", curto: "atividades" },
];

const ORDENS_TURMAS = {
  ano: { rotulo: "Ano e turma" },
  acerto: { rotulo: "Menor acerto primeiro" },
  engajamento: { rotulo: "Menos alunos ativos primeiro" },
};

/**
 * RF19 (administrador): painel pedagógico da escola. Minimização (LGPD): só números agregados por ano
 * escolar, turma e semana, sem nomes. Grupos com menos alunos que o mínimo aparecem como "poucos
 * dados", sem percentual, para que um número não revele o resultado de um aluno.
 */
export default function PainelEscola() {
  const [painel, setPainel] = useState(null);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    painelDaEscola()
      .then(setPainel)
      .catch((err) => setErro(err.message));
  }, []);

  return (
    <>
      <Cabecalho titulo="Painel da escola">
        Visão pedagógica da escola por ano, turma e semana. Só números agregados: nenhum aluno é identificado.
      </Cabecalho>
      <Mensagens erro={erro} />
      {!painel && !erro && <Carregando />}
      {painel && (
        <>
          <div className="alert alert-light border small d-flex gap-2 align-items-start" role="note">
            <span aria-hidden="true">🔒</span>
            <span>
              Para proteger os alunos, grupos com menos de {painel.minimoAlunosPorGrupo} alunos aparecem como{" "}
              <em>poucos dados</em>, sem percentual. O detalhe por aluno fica só com os professores de cada turma.
            </span>
          </div>

          <section className="mb-5">
            <h2 className="h5 mb-1">Acerto por ano escolar e tópico</h2>
            <p className="small text-secondary">Respostas corrigidas dos alunos das turmas ativas, somando todas as turmas do ano.</p>
            <div className="card">
              <div className="card-body">
                {painel.mapaAnoTopico.length === 0 ? (
                  <p className="text-secondary mb-0">Ainda não há respostas corrigidas na escola.</p>
                ) : (
                  <MapaAnoTopico celulas={painel.mapaAnoTopico} minimo={painel.minimoAlunosPorGrupo} />
                )}
              </div>
            </div>
          </section>

          <section className="mb-5">
            <ComparativoTurmas turmas={painel.turmas} minimo={painel.minimoAlunosPorGrupo} />
          </section>

          <section className="mb-5">
            <h2 className="h5 mb-1">Engajamento semanal</h2>
            <p className="small text-secondary">Últimas {painel.engajamento.length} semanas, a atual inclusive (semanas começam na segunda-feira).</p>
            <div className="card">
              <div className="card-body">
                <GraficoEngajamento semanas={painel.engajamento} />
              </div>
            </div>
          </section>

          <section>
            <h2 className="h5 mb-1">Banco de questões compartilhado</h2>
            <p className="small text-secondary">Questões criadas pelos professores da escola e o uso delas entre as escolas.</p>
            <BancoCompartilhado banco={painel.banco} />
          </section>
        </>
      )}
    </>
  );
}

/** Mapa de calor ano escolar × tópico. "Poucos dados" quando o grupo tem menos alunos que o mínimo. */
function MapaAnoTopico({ celulas, minimo }) {
  const { ref, dica, alvo, esconder } = useDica();
  const anos = ANOS_ESCOLARES.map((ano) => ano.valor).filter((ano) => celulas.some((c) => c.anoEscolar === ano));
  const topicos = [];
  const vistos = new Set();
  for (const celula of celulas) {
    if (!vistos.has(celula.topicoId)) {
      vistos.add(celula.topicoId);
      topicos.push({ topicoId: celula.topicoId, topico: celula.topico });
    }
  }
  topicos.sort((a, b) => a.topico.localeCompare(b.topico, "pt-BR"));
  const porCelula = new Map();
  for (const celula of celulas) {
    porCelula.set(`${celula.anoEscolar}-${celula.topicoId}`, celula);
  }

  return (
    <div ref={ref} className="position-relative" onMouseLeave={esconder}>
      <div className="table-responsive">
        <table className="me-calor">
          <thead>
            <tr>
              <th scope="col" className="me-calor-fixa">Ano</th>
              {topicos.map((topico) => (
                <th key={topico.topicoId} scope="col" className="me-calor-topico">
                  <span>{topico.topico}</span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {anos.map((ano) => (
              <tr key={ano}>
                <th scope="row" className="me-calor-fixa">
                  {rotuloAno(ano)}
                </th>
                {topicos.map((topico) => {
                  const celula = porCelula.get(`${ano}-${topico.topicoId}`);
                  if (!celula) {
                    return (
                      <td key={topico.topicoId} className="me-calor-celula me-calor-vazio" aria-label="sem respostas">
                        ·
                      </td>
                    );
                  }
                  if (celula.percentual === null || celula.percentual === undefined) {
                    return (
                      <td
                        key={topico.topicoId}
                        className="me-calor-celula me-calor-poucos"
                        {...alvo(
                          <>
                            <div className="fw-semibold">
                              {rotuloAno(ano)} · {topico.topico}
                            </div>
                            <div>Poucos dados: menos de {minimo} alunos responderam</div>
                          </>
                        )}
                      >
                        poucos
                      </td>
                    );
                  }
                  return (
                    <td
                      key={topico.topicoId}
                      className={`me-calor-celula ${classeCalor(celula.percentual)}`}
                      {...alvo(
                        <>
                          <div className="fw-semibold">
                            {rotuloAno(ano)} · {topico.topico}
                          </div>
                          <div>Acerto: {formatarPercentual(celula.percentual)}</div>
                          <div>
                            {celula.alunos} alunos · {celula.respostas} respostas
                          </div>
                        </>
                      )}
                    >
                      {Math.round(Number(celula.percentual))}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <LegendaCalor notas={[`poucos: menos de ${minimo} alunos`, "· tópico sem respostas no ano"]} />
      <Dica dica={dica} />
    </div>
  );
}

/** Turmas ativas lado a lado: acerto médio (barra) e engajamento, com ordenação escolhida. */
function ComparativoTurmas({ turmas, minimo }) {
  const [ordem, setOrdem] = useState("ano");
  const { ref, dica, alvo, esconder } = useDica();

  const posicaoDoAno = (turma) => ANOS_ESCOLARES.findIndex((ano) => ano.valor === turma.anoEscolar);
  const ordenadas = [...turmas];
  if (ordem === "ano") {
    ordenadas.sort((a, b) => posicaoDoAno(a) - posicaoDoAno(b) || a.turma.localeCompare(b.turma, "pt-BR"));
  } else if (ordem === "acerto") {
    // poucos dados no fim: não dá para comparar
    ordenadas.sort((a, b) => (a.percentual ?? Infinity) - (b.percentual ?? Infinity));
  } else if (ordem === "engajamento") {
    ordenadas.sort((a, b) => a.alunosAtivosRecentes / (a.alunos || 1) - b.alunosAtivosRecentes / (b.alunos || 1));
  }

  return (
    <>
      <div className="d-flex flex-wrap justify-content-between align-items-end gap-2 mb-2">
        <div>
          <h2 className="h5 mb-1">Turmas</h2>
          <p className="small text-secondary mb-0">
            Acerto médio nas respostas corrigidas e alunos que responderam algo nos últimos 14 dias.
          </p>
        </div>
        <div>
          <label htmlFor="ordem-turmas" className="form-label small text-secondary mb-1">
            Ordenar por
          </label>
          <select id="ordem-turmas" className="form-select form-select-sm" value={ordem} onChange={(e) => setOrdem(e.target.value)}>
            {Object.entries(ORDENS_TURMAS).map(([chave, opcao]) => (
              <option key={chave} value={chave}>
                {opcao.rotulo}
              </option>
            ))}
          </select>
        </div>
      </div>
      {turmas.length === 0 ? (
        <EstadoVazio titulo="Nenhuma turma ativa">
          <Link to="/turmas">Cadastre as turmas</Link> para ver o comparativo.
        </EstadoVazio>
      ) : (
        <div ref={ref} className="card position-relative" onMouseLeave={esconder}>
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead>
                <tr>
                  <th>Turma</th>
                  <th className="text-end">Alunos</th>
                  <th className="text-end">Ativos (14 dias)</th>
                  <th className="text-end">Atividades</th>
                  <th style={{ minWidth: "14rem" }}>Acerto médio</th>
                </tr>
              </thead>
              <tbody>
                {ordenadas.map((turma) => {
                  const poucos = turma.percentual === null || turma.percentual === undefined;
                  return (
                    <tr key={turma.turmaId}>
                      <td>
                        <Link to={`/turmas/${turma.turmaId}`} className="fw-semibold text-decoration-none">
                          {turma.turma}
                        </Link>
                        <div className="small text-secondary">{rotuloAno(turma.anoEscolar)}</div>
                      </td>
                      <td className="text-end">{turma.alunos}</td>
                      <td className="text-end text-nowrap">
                        {turma.alunosAtivosRecentes}
                        <span className="text-secondary small"> de {turma.alunos}</span>
                      </td>
                      <td className="text-end">{turma.atividades}</td>
                      <td>
                        {poucos ? (
                          <span className="small text-secondary">
                            {turma.respostas === 0 ? "sem respostas corrigidas" : `poucos dados (menos de ${minimo} alunos)`}
                          </span>
                        ) : (
                          <div
                            className="me-comparada"
                            {...alvo(
                              <>
                                <div className="fw-semibold">{turma.turma}</div>
                                <div>Acerto médio: {formatarPercentual(turma.percentual)}</div>
                                <div>
                                  {turma.respostas} respostas de {turma.alunosComRespostas} aluno(s)
                                </div>
                              </>
                            )}
                          >
                            <span className="me-comparada-trilho" aria-hidden="true">
                              <span className="me-comparada-valor" style={{ width: `${Number(turma.percentual)}%` }} />
                              <span className="me-comparada-meta" />
                            </span>
                            <span className="me-comparada-numero">{formatarPercentual(turma.percentual)}</span>
                          </div>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <div className="card-footer bg-white small text-secondary d-flex align-items-center gap-2">
            <span className="me-chave-linha" /> 70%: nível de domínio usado pelo motor adaptativo
          </div>
          <Dica dica={dica} />
        </div>
      )}
    </>
  );
}

/** Colunas por semana para a medida escolhida (uma por vez, mesmo eixo), com dica e tabela. */
function GraficoEngajamento({ semanas }) {
  const [medida, setMedida] = useState(MEDIDAS_ENGAJAMENTO[0]);
  const [ativa, setAtiva] = useState(null);
  const [refLargura, largura] = useLargura();

  const altura = 240;
  const margem = { topo: 16, direita: 12, base: 30, esquerda: 40 };
  const areaX = largura - margem.esquerda - margem.direita;
  const areaY = altura - margem.topo - margem.base;
  const maximo = Math.max(1, ...semanas.map((s) => s[medida.chave]));
  const escala = topoDaEscala(maximo);
  const faixa = areaX / semanas.length;
  const larguraColuna = Math.min(44, faixa * 0.6);
  const y = (valor) => margem.topo + areaY - (valor / escala) * areaY;
  const marcas = [0, escala / 4, escala / 2, (3 * escala) / 4, escala];
  const semanaAtiva = ativa === null ? null : semanas[ativa];

  return (
    <figure className="grafico mb-0">
      <div className="btn-group btn-group-sm flex-wrap mb-3" role="group" aria-label="Medida do gráfico">
        {MEDIDAS_ENGAJAMENTO.map((opcao) => (
          <button
            key={opcao.chave}
            type="button"
            className={`btn ${opcao.chave === medida.chave ? "btn-primary" : "btn-outline-primary"}`}
            aria-pressed={opcao.chave === medida.chave}
            onClick={() => setMedida(opcao)}
          >
            {opcao.rotulo}
          </button>
        ))}
      </div>
      <div ref={refLargura} className="position-relative" onMouseLeave={() => setAtiva(null)}>
        <svg viewBox={`0 0 ${largura} ${altura}`} role="img" aria-label={`${medida.rotulo} por semana. Os números estão na tabela abaixo.`}>
          {marcas.map((marca) => (
            <g key={marca}>
              <line x1={margem.esquerda} x2={largura - margem.direita} y1={y(marca)} y2={y(marca)} className="grafico-grade" />
              <text x={margem.esquerda - 6} y={y(marca) + 4} textAnchor="end" className="grafico-rotulo">
                {Math.round(marca).toLocaleString("pt-BR")}
              </text>
            </g>
          ))}
          {semanas.map((semana, i) => {
            const valor = semana[medida.chave];
            const centro = margem.esquerda + faixa * i + faixa / 2;
            const topo = y(valor);
            return (
              <g key={semana.semana}>
                {valor > 0 && (
                  <path
                    d={colunaArredondada(centro - larguraColuna / 2, topo, larguraColuna, margem.topo + areaY - topo, 4)}
                    className={ativa === i ? "grafico-coluna-ativa" : "grafico-coluna"}
                  />
                )}
                <text x={centro} y={altura - 10} textAnchor="middle" className="grafico-rotulo">
                  {formatarData(semana.semana).slice(0, 5)}
                </text>
                <rect
                  x={centro - faixa / 2}
                  y={margem.topo}
                  width={faixa}
                  height={areaY}
                  className="grafico-alvo"
                  onMouseEnter={() => setAtiva(i)}
                  onTouchStart={() => setAtiva(i)}
                />
              </g>
            );
          })}
        </svg>
        {semanaAtiva && (
          <div
            className="grafico-dica"
            style={{
              left: `${((margem.esquerda + faixa * ativa + faixa / 2) / largura) * 100}%`,
              top: `${(y(semanaAtiva[medida.chave]) / altura) * 100}%`,
            }}
          >
            <div className="fw-semibold">Semana de {formatarData(semanaAtiva.semana)}</div>
            {MEDIDAS_ENGAJAMENTO.map((opcao) => (
              <div key={opcao.chave} className={opcao.chave === medida.chave ? "fw-semibold" : ""}>
                {opcao.rotulo}: {semanaAtiva[opcao.chave].toLocaleString("pt-BR")}
              </div>
            ))}
          </div>
        )}
      </div>
      <details className="mt-2">
        <summary className="small">Ver em tabela</summary>
        <div className="table-responsive">
          <table className="table table-sm small mt-2 mb-0">
            <thead>
              <tr>
                <th>Semana de</th>
                {MEDIDAS_ENGAJAMENTO.map((opcao) => (
                  <th key={opcao.chave} className="text-end">
                    {opcao.rotulo}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {semanas.map((semana) => (
                <tr key={semana.semana}>
                  <td>{formatarData(semana.semana)}</td>
                  {MEDIDAS_ENGAJAMENTO.map((opcao) => (
                    <td key={opcao.chave} className="text-end">
                      {semana[opcao.chave].toLocaleString("pt-BR")}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </details>
    </figure>
  );
}

function BancoCompartilhado({ banco }) {
  return (
    <div className="row g-3">
      <div className="col-6 col-lg-3">
        <CartaoNumero rotulo="Aprovadas no banco" valor={banco.aprovadas} detalhe="criadas pela escola" />
      </div>
      <div className="col-6 col-lg-3">
        <CartaoNumero rotulo="Em validação" valor={banco.pendentes} detalhe="esperando votos de outras escolas" />
      </div>
      <div className="col-6 col-lg-3">
        <CartaoNumero rotulo="Rascunhos" valor={banco.rascunhos} detalhe={`${banco.rejeitadas} rejeitada(s)`} />
      </div>
      <div className="col-6 col-lg-3">
        <CartaoNumero rotulo="Votos dados" valor={banco.votosDados} detalhe="em questões de outras escolas" />
      </div>
      <div className="col-md-4">
        <CartaoNumero
          rotulo="Usos das nossas questões por outras escolas"
          valor={banco.usosPorOutrasEscolas}
          detalhe="vezes em que entraram em atividades de outras escolas"
        />
      </div>
      <div className="col-md-4">
        <CartaoNumero
          rotulo="Usos das nossas questões aqui"
          valor={banco.usosNaPropriaEscola}
          detalhe="vezes em atividades das nossas turmas"
        />
      </div>
      <div className="col-md-4">
        <CartaoNumero
          rotulo="Questões de outras escolas usadas"
          valor={banco.questoesDeOutrasEscolasUsadas}
          detalhe="questões diferentes, em atividades das nossas turmas"
        />
      </div>
    </div>
  );
}

/**
 * Topo do eixo com quatro intervalos de passo "redondo" (1; 2; 2,5; 5 × 10ⁿ, inteiro), para as marcas
 * ficarem com números inteiros e fáceis de ler.
 */
function topoDaEscala(maximo) {
  const passoMinimo = Math.max(1, maximo / 4);
  const potencia = 10 ** Math.floor(Math.log10(passoMinimo));
  for (const fator of [1, 2, 2.5, 5, 10]) {
    const passo = fator * potencia;
    if (passo >= passoMinimo && Number.isInteger(passo)) {
      return passo * 4;
    }
  }
  return 40 * potencia;
}

/** Coluna com as pontas de cima arredondadas e a base reta, apoiada no eixo. */
function colunaArredondada(x, y, largura, altura, raio) {
  const r = Math.min(raio, largura / 2, altura);
  return `M${x},${y + altura} L${x},${y + r} Q${x},${y} ${x + r},${y} L${x + largura - r},${y} Q${x + largura},${y} ${x + largura},${y + r} L${x + largura},${y + altura} Z`;
}
