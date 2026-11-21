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
