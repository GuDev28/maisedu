import { FAIXAS_CALOR } from "./escalaCalor";

/*
 * Peças visuais do mapa de calor: a legenda das faixas e a dica (tooltip). A escala de cores fica em
 * escalaCalor.js e o hook da dica em useDica.js, para este arquivo exportar só componentes.
 */

/** Legenda das faixas, com notas extras opcionais (ex.: o que significa "*" ou "poucos dados"). */
export function LegendaCalor({ notas = [] }) {
  return (
    <div className="d-flex flex-wrap gap-3 align-items-center mt-2">
      <div className="me-calor-legenda">
        {FAIXAS_CALOR.map((faixa) => (
          <span key={faixa.classe} style={{ "--cor": faixa.cor }}>
            {faixa.rotulo}
          </span>
        ))}
      </div>
      {notas.map((nota) => (
        <span key={nota} className="small text-secondary">
          {nota}
        </span>
      ))}
    </div>
  );
}

export function Dica({ dica }) {
  if (!dica) {
    return null;
  }
  return (
    <div className="grafico-dica" style={{ left: dica.x, top: dica.y }} role="tooltip">
      {dica.conteudo}
    </div>
  );
}
