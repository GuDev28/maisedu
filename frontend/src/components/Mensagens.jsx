/** Mensagens fixas da tela (erro de validação, aviso). Confirmações rápidas usam os avisos que somem sozinhos. */
export function Mensagens({ erro, sucesso, aviso }) {
  return (
    <>
      {erro && (
        <div className="alert alert-danger py-2" role="alert">
          {erro}
        </div>
      )}
      {aviso && (
        <div className="alert alert-warning py-2" role="status">
          {aviso}
        </div>
      )}
      {sucesso && (
        <div className="alert alert-success py-2" role="status">
          {sucesso}
        </div>
      )}
    </>
  );
}
