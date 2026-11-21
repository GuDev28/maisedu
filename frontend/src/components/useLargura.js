import { useCallback, useState } from "react";

/**
 * Largura atual (em px) do elemento que recebe o {@code ref} devolvido. Os gráficos em SVG usam essa
 * largura no viewBox, para o texto ficar sempre no mesmo tamanho (11 px), em vez de crescer ou
 * encolher junto com o desenho.
 *
 * O ref é uma função: mede o elemento quando ele aparece e acompanha as mudanças de tamanho com um
 * ResizeObserver. A função devolvida desliga o observador quando o elemento sai da tela (React 19).
 */
export function useLargura(padrao = 600) {
  const [largura, setLargura] = useState(padrao);

  const ref = useCallback(
    (elemento) => {
      if (!elemento) {
        return undefined;
      }
      setLargura(Math.round(elemento.getBoundingClientRect().width) || padrao);
      if (typeof ResizeObserver === "undefined") {
        return undefined;
      }
      const observador = new ResizeObserver(([entrada]) => {
        const nova = Math.round(entrada.contentRect.width);
        if (nova > 0) {
          setLargura(nova);
        }
      });
      observador.observe(elemento);
      return () => observador.disconnect();
    },
    [padrao]
  );

  return [ref, largura];
}
