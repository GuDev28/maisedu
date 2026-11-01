export function Mensagens({ erro, sucesso }) {
  return (
    <>
      {erro && (
        <p className="mensagem mensagem-erro" role="alert">
          {erro}
        </p>
      )}
      {sucesso && (
        <p className="mensagem mensagem-sucesso" role="status">
          {sucesso}
        </p>
      )}
    </>
  );
}
