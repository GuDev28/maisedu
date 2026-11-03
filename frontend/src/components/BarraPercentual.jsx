/** Barra horizontal de 0 a 100% com o valor escrito ao lado (a cor nunca é a única informação). */
export function BarraPercentual({ valor, destaque = false }) {
  const numero = Math.max(0, Math.min(100, Number(valor) || 0));
  const classe = numero < 50 ? "barra-baixa" : numero < 70 ? "barra-media" : "barra-alta";
  return (
    <span className={`barra ${destaque ? "barra-destaque" : ""}`}>
      <span className="barra-trilho" aria-hidden="true">
        <span className={`barra-preenchimento ${classe}`} style={{ width: `${numero}%` }} />
      </span>
      <span className="barra-valor">{numero.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%</span>
    </span>
  );
}
