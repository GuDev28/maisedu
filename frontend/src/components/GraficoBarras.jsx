import { classeFaixa, formatarPercentual } from "./rotulos";

// A marca de 70% é a linha tracejada; o rótulo dela ficaria colado ao de 50% em telas estreitas.
const MARCAS = [0, 50, 100];

/**
 * Gráfico de barras horizontais de 0 a 100%, com a linha de 70% (domínio, RN06).
 * Feito em HTML/CSS, sem biblioteca: os nomes longos dos tópicos quebram linha em telas estreitas.
 * Cada item: { chave, rotulo, valor, detalhe }.
 */
export function GraficoBarras({ itens, descricao }) {
  if (!itens || itens.length === 0) {
    return <p className="texto-suave">Ainda não há respostas para montar o gráfico.</p>;
  }

  return (
    <figure className="grafico-barras">
      <ul aria-label={descricao}>
        {itens.map((item) => {
          const valor = Math.max(0, Math.min(100, Number(item.valor) || 0));
          return (
            <li key={item.chave} className="gb-linha" title={item.detalhe}>
              <span className="gb-rotulo">{item.rotulo}</span>
              <span className="gb-trilho" aria-hidden="true">
                <span className={`gb-barra ${classeFaixa(valor)}`} style={{ width: `${valor}%` }} />
                <span className="gb-meta" />
              </span>
              <span className="gb-valor">{formatarPercentual(valor)}</span>
            </li>
          );
        })}
      </ul>
      <div className="gb-linha gb-eixo" aria-hidden="true">
        <span />
        <span className="gb-marcas">
          {MARCAS.map((marca) => (
            <span key={marca} style={{ left: `${marca}%` }}>{marca}%</span>
          ))}
        </span>
        <span />
      </div>
      <figcaption className="texto-suave">
        Percentual de acerto. Linha tracejada: 70% (domínio). Passe o mouse sobre uma barra para ver os totais.
      </figcaption>
    </figure>
  );
}
