import { useEffect, useState } from "react";

/**
 * Largura atual (em px) do elemento que recebe o {@code ref} devolvido. Os gráficos em SVG usam essa
 * largura no viewBox, para o texto ficar sempre no mesmo tamanho (11 px), em vez de crescer ou
 * encolher junto com o desenho.
 */
export function useLargura(padrao = 600) {
  const [elemento, setElemento] = useState(null);
  const [largura, setLargura] = useState(padrao);

  useEffect(() => {
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
  }, [elemento, padrao]);

  return [setElemento, largura];
}
