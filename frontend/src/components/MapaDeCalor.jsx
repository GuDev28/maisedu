import { useRef, useState } from "react";

/**
 * Escala divergente do mapa de calor, centrada em 70% (o nível de domínio do motor adaptativo):
 * abaixo, tons de vermelho; perto da meta, cinza; acima, tons de azul. O número aparece dentro da
 * célula, então a cor nunca é a única informação.
 */
export const FAIXAS_CALOR = [
  { classe: "me-calor-1", rotulo: "abaixo de 50%", cor: "#c43c3c", ate: 50 },
  { classe: "me-calor-2", rotulo: "50 a 65%", cor: "#f4c7c3", ate: 65 },
  { classe: "me-calor-3", rotulo: "65 a 75% (perto da meta)", cor: "#eceef1", ate: 75 },
  { classe: "me-calor-4", rotulo: "75 a 90%", cor: "#b7d3f6", ate: 90 },
  { classe: "me-calor-5", rotulo: "90% ou mais", cor: "#256abf", ate: Infinity },
];

export function classeCalor(percentual) {
  if (percentual === null || percentual === undefined) {
    return "me-calor-vazio";
  }
  const valor = Number(percentual);
  for (const faixa of FAIXAS_CALOR) {
    if (valor < faixa.ate) {
      return faixa.classe;
    }
  }
  return FAIXAS_CALOR[FAIXAS_CALOR.length - 1].classe;
}

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

/**
 * Dica (tooltip) que acompanha o elemento sob o mouse ou com o foco do teclado. {@code ref} vai no
 * contêiner com position: relative que envolve o gráfico ou a tabela (fora da rolagem horizontal,
 * para a dica não ser cortada).
 */
export function useDica() {
  const ref = useRef(null);
  const [dica, setDica] = useState(null);

  function mostrar(evento, conteudo) {
    if (!ref.current) {
      return;
    }
    const caixa = ref.current.getBoundingClientRect();
    const alvo = evento.currentTarget.getBoundingClientRect();
    setDica({ x: alvo.left + alvo.width / 2 - caixa.left, y: alvo.top - caixa.top, conteudo });
  }

  function esconder() {
    setDica(null);
  }

  /** Atributos para o elemento que mostra a dica: mouse, toque e teclado. */
  function alvo(conteudo) {
    return {
      tabIndex: 0,
      onMouseEnter: (evento) => mostrar(evento, conteudo),
      onFocus: (evento) => mostrar(evento, conteudo),
      onTouchStart: (evento) => mostrar(evento, conteudo),
      onMouseLeave: esconder,
      onBlur: esconder,
    };
  }

  return { ref, dica, alvo, esconder };
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
