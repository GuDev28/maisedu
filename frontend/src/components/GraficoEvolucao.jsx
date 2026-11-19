import { useState } from "react";
import { formatarData, formatarPercentual } from "./rotulos";
import { useLargura } from "./useLargura";

const ALTURA = 220;
const MARGEM = { topo: 18, direita: 18, base: 30, esquerda: 40 };
const AREA_Y = ALTURA - MARGEM.topo - MARGEM.base;
const META = 70;

/**
 * Evolução semanal do acerto: a linha é o percentual acumulado e os pontos claros, o acerto de cada
 * semana. Ao passar o mouse (ou o dedo) numa semana aparecem a guia e os números; a tabela embaixo
 * traz os mesmos dados. SVG simples, sem biblioteca de gráficos.
 * {@code detalhe(ponto)}: linha extra opcional na dica (ex.: quantos alunos responderam na semana).
 */
export function GraficoEvolucao({ pontos, detalhe, rotulo = "Acerto acumulado por semana" }) {
  const [ativo, setAtivo] = useState(null);
  const [refLargura, LARGURA] = useLargura();

  if (!pontos || pontos.length === 0) {
    return <p className="text-secondary mb-0">Ainda não há respostas corrigidas para mostrar a evolução.</p>;
  }

  const AREA_X = LARGURA - MARGEM.esquerda - MARGEM.direita;

  const passo = pontos.length === 1 ? 0 : AREA_X / (pontos.length - 1);
  const x = (i) => MARGEM.esquerda + (pontos.length === 1 ? AREA_X / 2 : i * passo);
  const y = (valor) => MARGEM.topo + AREA_Y - (Number(valor) / 100) * AREA_Y;
  const larguraFaixa = pontos.length === 1 ? AREA_X : passo;
  // cada rótulo de data ocupa uns 44 px: em gráfico estreito, mostra um a cada N semanas
  const pularRotulos = Math.max(1, Math.ceil((pontos.length * 44) / AREA_X));

  const linha = pontos.map((p, i) => `${i === 0 ? "M" : "L"}${x(i)},${y(p.percentualAcumulado)}`).join(" ");
  const pontoAtivo = ativo === null ? null : pontos[ativo];

  return (
    <figure className="grafico mb-0">
      <div className="d-flex flex-wrap gap-3 small text-secondary mb-2" aria-hidden="true">
        <span>
          <span className="me-chave" style={{ background: "var(--me-grafico)" }} />
          Acerto acumulado
        </span>
        <span>
          <span className="me-chave grafico-chave-semana" />
          Acerto da semana
        </span>
        <span>
          <span className="me-chave-linha" />
          70%: domínio
        </span>
      </div>
      <div ref={refLargura} className="position-relative" onMouseLeave={() => setAtivo(null)}>
        <svg viewBox={`0 0 ${LARGURA} ${ALTURA}`} role="img" aria-label={`${rotulo}. Os números estão na tabela abaixo.`}>
          {[0, 25, 50, 75, 100].map((marca) => (
            <g key={marca}>
              <line x1={MARGEM.esquerda} x2={LARGURA - MARGEM.direita} y1={y(marca)} y2={y(marca)} className="grafico-grade" />
              <text x={MARGEM.esquerda - 6} y={y(marca) + 4} textAnchor="end" className="grafico-rotulo">
                {marca}%
              </text>
            </g>
          ))}
          <line x1={MARGEM.esquerda} x2={LARGURA - MARGEM.direita} y1={y(META)} y2={y(META)} className="grafico-meta" />
          <text x={LARGURA - MARGEM.direita} y={y(META) - 4} textAnchor="end" className="grafico-rotulo-meta">
            70%
          </text>

          {pontoAtivo && (
            <line x1={x(ativo)} x2={x(ativo)} y1={MARGEM.topo} y2={MARGEM.topo + AREA_Y} className="grafico-guia" />
          )}

          <path d={linha} className="grafico-linha" />
          {pontos.map((p, i) => (
            <g key={p.semana}>
              <circle cx={x(i)} cy={y(p.percentualSemana)} r={ativo === i ? 5 : 4} className="grafico-ponto-semana" />
              <circle cx={x(i)} cy={y(p.percentualAcumulado)} r={ativo === i ? 6 : 4.5} className="grafico-ponto" />
              {i % pularRotulos === 0 && (
                <text x={x(i)} y={ALTURA - 10} textAnchor="middle" className="grafico-rotulo">
                  {formatarData(p.semana).slice(0, 5)}
                </text>
              )}
            </g>
          ))}

          {/* faixas invisíveis, maiores que os pontos, para o mouse e o toque */}
          {pontos.map((p, i) => (
            <rect
              key={`alvo-${p.semana}`}
              x={x(i) - larguraFaixa / 2}
              y={MARGEM.topo}
              width={larguraFaixa}
              height={AREA_Y}
              className="grafico-alvo"
              onMouseEnter={() => setAtivo(i)}
              onTouchStart={() => setAtivo(i)}
            />
          ))}
        </svg>

        {pontoAtivo && (
          <div
            className="grafico-dica"
            style={{
              left: `${(x(ativo) / LARGURA) * 100}%`,
              top: `${(Math.min(y(pontoAtivo.percentualAcumulado), y(pontoAtivo.percentualSemana)) / ALTURA) * 100}%`,
            }}
          >
            <div className="fw-semibold">Semana de {formatarData(pontoAtivo.semana)}</div>
            <div>Acumulado: {formatarPercentual(pontoAtivo.percentualAcumulado)}</div>
            <div>
              Na semana: {formatarPercentual(pontoAtivo.percentualSemana)} ({pontoAtivo.acertos} de {pontoAtivo.respostas})
            </div>
            {detalhe && <div>{detalhe(pontoAtivo)}</div>}
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
                <th className="text-end">Respostas</th>
                <th className="text-end">Acertos</th>
                <th className="text-end">Na semana</th>
                <th className="text-end">Acumulado</th>
                {detalhe && <th>Observação</th>}
              </tr>
            </thead>
            <tbody>
              {pontos.map((p) => (
                <tr key={p.semana}>
                  <td>{formatarData(p.semana)}</td>
                  <td className="text-end">{p.respostas}</td>
                  <td className="text-end">{p.acertos}</td>
                  <td className="text-end">{formatarPercentual(p.percentualSemana)}</td>
                  <td className="text-end">{formatarPercentual(p.percentualAcumulado)}</td>
                  {detalhe && <td>{detalhe(p)}</td>}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </details>
    </figure>
  );
}
