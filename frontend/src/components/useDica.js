import { useRef, useState } from "react";

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
