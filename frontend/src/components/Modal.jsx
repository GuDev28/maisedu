import { useEffect } from "react";

/**
 * Modal do Bootstrap controlado pelo React (sem o JavaScript do Bootstrap): aberto quando o
 * componente é renderizado. Esc ou clique fora fecham.
 */
export function Modal({ titulo, children, rodape, aoFechar, tamanho = "" }) {
  useEffect(() => {
    function aoTeclar(evento) {
      if (evento.key === "Escape") {
        aoFechar();
      }
    }
    document.addEventListener("keydown", aoTeclar);
    document.body.classList.add("modal-open");
    return () => {
      document.removeEventListener("keydown", aoTeclar);
      document.body.classList.remove("modal-open");
    };
  }, [aoFechar]);

  return (
    <>
      <div
        className="modal d-block"
        tabIndex="-1"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-titulo"
        onMouseDown={(evento) => evento.target === evento.currentTarget && aoFechar()}
      >
        <div className={`modal-dialog modal-dialog-centered ${tamanho ? `modal-${tamanho}` : ""}`}>
          <div className="modal-content">
            <div className="modal-header">
              <h2 className="modal-title fs-5" id="modal-titulo">
                {titulo}
              </h2>
              <button type="button" className="btn-close" aria-label="Fechar" onClick={aoFechar} />
            </div>
            <div className="modal-body">{children}</div>
            {rodape && <div className="modal-footer">{rodape}</div>}
          </div>
        </div>
      </div>
      <div className="modal-backdrop show" />
    </>
  );
}
