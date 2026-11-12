/** Título da página com descrição e, à direita, as ações principais (ex.: "Nova questão"). */
export function Cabecalho({ titulo, children, acoes }) {
  return (
    <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
      <div className="me-cabecalho-texto">
        <h1 className="me-titulo">{titulo}</h1>
        {children && <p className="text-secondary mb-0">{children}</p>}
      </div>
      {acoes && <div className="d-flex flex-wrap gap-2">{acoes}</div>}
    </div>
  );
}
