export function Carregando({ texto = "Carregando..." }) {
  return (
    <div className="d-flex align-items-center gap-2 text-secondary py-4" role="status">
      <span className="spinner-border spinner-border-sm" aria-hidden="true" />
      <span>{texto}</span>
    </div>
  );
}
