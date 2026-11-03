import { formatarData } from "./rotulos";

/**
 * Evolução semanal: linha com o percentual acumulado e pontos com o percentual de cada semana.
 * SVG simples, sem biblioteca de gráficos.
 */
export function GraficoEvolucao({ pontos }) {
  if (!pontos || pontos.length === 0) {
    return <p className="texto-suave">Ainda não há respostas para mostrar a evolução.</p>;
  }

  const largura = 560;
  const altura = 200;
  const margem = { topo: 16, direita: 16, base: 32, esquerda: 40 };
  const areaX = largura - margem.esquerda - margem.direita;
  const areaY = altura - margem.topo - margem.base;
  const x = (i) => margem.esquerda + (pontos.length === 1 ? areaX / 2 : (i * areaX) / (pontos.length - 1));
  const y = (valor) => margem.topo + areaY - (Number(valor) / 100) * areaY;

  const linha = pontos.map((p, i) => `${i === 0 ? "M" : "L"}${x(i)},${y(p.percentualAcumulado)}`).join(" ");

  return (
    <figure className="grafico">
      <svg viewBox={`0 0 ${largura} ${altura}`} role="img"
           aria-label="Percentual de acerto acumulado por semana">
        {[0, 50, 70, 100].map((marca) => (
          <g key={marca}>
            <line x1={margem.esquerda} x2={largura - margem.direita} y1={y(marca)} y2={y(marca)}
                  className={marca === 70 ? "grafico-meta" : "grafico-grade"} />
            <text x={margem.esquerda - 6} y={y(marca) + 4} textAnchor="end" className="grafico-rotulo">
              {marca}%
            </text>
          </g>
        ))}
        <path d={linha} className="grafico-linha" />
        {pontos.map((p, i) => (
          <g key={p.semana}>
            <circle cx={x(i)} cy={y(p.percentualSemana)} r="4" className="grafico-ponto-semana">
              <title>
                Semana de {formatarData(p.semana)}: {p.acertos} de {p.respostas} ({p.percentualSemana}%)
              </title>
            </circle>
            <circle cx={x(i)} cy={y(p.percentualAcumulado)} r="5" className="grafico-ponto">
              <title>Acumulado até {formatarData(p.semana)}: {p.percentualAcumulado}%</title>
            </circle>
            <text x={x(i)} y={altura - 10} textAnchor="middle" className="grafico-rotulo">
              {formatarData(p.semana).slice(0, 5)}
            </text>
          </g>
        ))}
      </svg>
      <figcaption className="texto-suave">
        Linha: acerto acumulado. Pontos claros: acerto de cada semana. Linha tracejada: 70% (domínio).
      </figcaption>
    </figure>
  );
}
