import { useCallback, useRef, useState } from "react";
import { Modal } from "../components/Modal";
import { InterfaceContext } from "./contextos";

/**
 * Avisos que somem sozinhos (toasts) e confirmações em modal, no lugar de window.alert,
 * window.confirm e window.prompt.
 */

let proximoId = 1;

export function InterfaceProvider({ children }) {
  const [avisos, setAvisos] = useState([]);
  const [confirmacao, setConfirmacao] = useState(null);
  const [textoDigitado, setTextoDigitado] = useState("");
  const resolver = useRef(null);

  const avisar = useCallback((texto, tipo = "success") => {
    const id = proximoId++;
    setAvisos((atuais) => [...atuais, { id, texto, tipo }]);
    setTimeout(() => setAvisos((atuais) => atuais.filter((aviso) => aviso.id !== id)), 4500);
  }, []);

  const confirmar = useCallback((opcoes) => {
    setTextoDigitado("");
    setConfirmacao(opcoes);
    return new Promise((resolve) => {
      resolver.current = resolve;
    });
  }, []);

  function responder(resposta) {
    setConfirmacao(null);
    resolver.current?.(resposta);
    resolver.current = null;
  }

  const bloqueado = confirmacao?.digitarParaConfirmar && textoDigitado.trim() !== confirmacao.digitarParaConfirmar;

  return (
    <InterfaceContext.Provider value={{ avisar, confirmar }}>
      {children}

      <div className="me-avisos d-flex flex-column gap-2" aria-live="polite">
        {avisos.map((aviso) => (
          <div key={aviso.id} className={`toast show align-items-center text-bg-${aviso.tipo} border-0`} role="status">
            <div className="d-flex">
              <div className="toast-body">{aviso.texto}</div>
              <button
                type="button"
                className="btn-close btn-close-white me-2 m-auto"
                aria-label="Fechar"
                onClick={() => setAvisos((atuais) => atuais.filter((a) => a.id !== aviso.id))}
              />
            </div>
          </div>
        ))}
      </div>

      {confirmacao && (
        <Modal
          titulo={confirmacao.titulo}
          aoFechar={() => responder(false)}
          rodape={
            <>
              <button type="button" className="btn btn-outline-secondary" onClick={() => responder(false)}>
                Cancelar
              </button>
              <button
                type="button"
                className={`btn ${confirmacao.perigo ? "btn-danger" : "btn-primary"}`}
                disabled={bloqueado}
                onClick={() => responder(true)}
              >
                {confirmacao.textoConfirmar ?? "Confirmar"}
              </button>
            </>
          }
        >
          <div className="mb-0">{confirmacao.mensagem}</div>
          {confirmacao.digitarParaConfirmar && (
            <div className="mt-3">
              <label className="form-label" htmlFor="texto-confirmacao">
                Para confirmar, digite <strong>{confirmacao.digitarParaConfirmar}</strong>
              </label>
              <input
                id="texto-confirmacao"
                className="form-control"
                value={textoDigitado}
                onChange={(evento) => setTextoDigitado(evento.target.value)}
                autoFocus
              />
            </div>
          )}
        </Modal>
      )}
    </InterfaceContext.Provider>
  );
}
