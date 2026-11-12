import { Link } from "react-router-dom";

/** Número de destaque da página Início. Com {@code para}, o cartão inteiro vira um link. */
export function CartaoNumero({ rotulo, valor, detalhe, para, tom = "" }) {
  const conteudo = (
    <div className={`card h-100 ${tom ? `border-${tom}` : ""}`}>
      <div className="card-body">
        <div className="text-secondary small">{rotulo}</div>
        <div className={`me-numero ${tom ? `text-${tom}` : ""}`}>{valor}</div>
        {detalhe && <div className="small text-secondary mt-1">{detalhe}</div>}
      </div>
    </div>
  );
  if (!para) {
    return conteudo;
  }
  return (
    <Link to={para} className="text-decoration-none text-reset d-block h-100">
      {conteudo}
    </Link>
  );
}
