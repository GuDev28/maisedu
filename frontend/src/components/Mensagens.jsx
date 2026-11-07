/** Mensagens de erro e de sucesso das telas, no padrão de alertas do Bootstrap. */
export function Mensagens({ erro, sucesso }) {
  return (
    <>
      {erro && (
        <div className="alert alert-danger mt-3" role="alert">
          {erro}
        </div>
      )}
      {sucesso && (
        <div className="alert alert-success mt-3" role="status">
          {sucesso}
        </div>
      )}
    </>
  );
}
