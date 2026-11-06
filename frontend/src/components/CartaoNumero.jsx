/**
 * Indicador em destaque (big number) do painel: o valor grande, o que ele mede e um detalhe opcional.
 * Use `texto` quando o valor for um nome (ex.: o tópico mais fraco), para uma fonte um pouco menor.
 */
export function CartaoNumero({ valor, rotulo, detalhe, texto = false, children }) {
  return (
    <div className="cartao-numero">
      {children ?? <span className={texto ? "numero numero-texto" : "numero"}>{valor}</span>}
      <span className="cartao-numero-rotulo">{rotulo}</span>
      {detalhe && <span className="texto-suave">{detalhe}</span>}
    </div>
  );
}
