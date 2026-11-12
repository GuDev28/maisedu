/** Lista vazia que já mostra o próximo passo ("Nenhuma turma ainda" + botão para cadastrar). */
export function EstadoVazio({ titulo, children, acao }) {
  return (
    <div className="card border-dashed text-center py-5 px-3">
      <div className="card-body">
        <p className="fw-semibold mb-1">{titulo}</p>
        {children && <p className="text-secondary mb-3">{children}</p>}
        {acao}
      </div>
    </div>
  );
}
