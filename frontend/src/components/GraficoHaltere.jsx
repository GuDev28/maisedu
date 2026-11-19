import { Dica, useDica } from "./MapaDeCalor";
import { formatarPercentual } from "./rotulos";

/**
 * "Haltere" antes → depois: um par de pontos por linha na mesma escala de 0 a 100%, ligados por um
 * traço. Usado no efeito da prática adaptativa (acerto nas atividades antes e depois da primeira
 * prática do tópico). Os números aparecem escritos ao lado; a cor só reforça.
 * {@code linhas}: [{chave, rotulo, detalhe, antes, depois, dica}].
 */
export function GraficoHaltere({ linhas, rotuloAntes, rotuloDepois }) {
  const { ref, dica, alvo, esconder } = useDica();

  return (
    <div ref={ref} className="position-relative" onMouseLeave={esconder}>
      <div className="d-flex flex-wrap gap-3 small text-secondary mb-3">
        <span>
          <span className="me-chave me-haltere-antes" />
          {rotuloAntes}
        </span>
        <span>
          <span className="me-chave me-haltere-depois" />
          {rotuloDepois}
        </span>
        <span>
          <span className="me-chave-linha" />
          70%: domínio
        </span>
      </div>
      <div className="me-haltere-grade">
        {linhas.map((linha) => {
          const antes = Number(linha.antes);
          const depois = Number(linha.depois);
          const diferenca = depois - antes;
          return (
            <div key={linha.chave} className="me-haltere-linha" {...alvo(linha.dica)}>
              <div className="me-haltere-rotulo">
                <span className="fw-semibold small">{linha.rotulo}</span>
                {linha.detalhe && <span className="small text-secondary">{linha.detalhe}</span>}
              </div>
              <div className="me-haltere" aria-hidden="true">
                <span className="me-haltere-trilho" />
                <span className="me-haltere-meta" style={{ left: "70%" }} />
                <span
                  className="me-haltere-ligacao"
                  style={{ left: `${Math.min(antes, depois)}%`, width: `${Math.abs(diferenca)}%` }}
                />
                <span className="me-haltere-ponto me-haltere-antes" style={{ left: `${antes}%` }} />
                <span className="me-haltere-ponto me-haltere-depois" style={{ left: `${depois}%` }} />
              </div>
              <div className="me-haltere-numeros small">
                {formatarPercentual(antes)} → <strong>{formatarPercentual(depois)}</strong>
                <span className={`ms-1 ${diferenca > 0 ? "text-success" : diferenca < 0 ? "text-danger" : "text-secondary"}`}>
                  ({diferenca > 0 ? "+" : ""}
                  {diferenca.toLocaleString("pt-BR", { maximumFractionDigits: 1 })} p.p.)
                </span>
              </div>
            </div>
          );
        })}
        <div className="me-haltere-linha me-haltere-eixo" aria-hidden="true">
          <div className="me-haltere-escala small text-secondary">
            <span style={{ left: 0 }}>0%</span>
            <span style={{ left: "70%", transform: "translateX(-50%)" }}>70%</span>
            <span style={{ right: 0 }}>100%</span>
          </div>
          <span />
        </div>
      </div>
      <Dica dica={dica} />
    </div>
  );
}
