/** Marca do MaisEdu: o "+" de "Mais" num quadrado verde, com um ponto âmbar. */
export function MarcaMaisEdu({ tamanho = 32 }) {
  return (
    <svg width={tamanho} height={tamanho} viewBox="0 0 64 64" aria-hidden="true" focusable="false">
      <rect width="64" height="64" rx="14" fill="#0f766e" />
      <path d="M27 16h10v11h11v10H37v11H27V37H16V27h11z" fill="#ffffff" />
      <circle cx="49" cy="15" r="6" fill="#f59e0b" />
    </svg>
  );
}

export function Logo({ tamanho = 32 }) {
  return (
    <span className="me-logo">
      <MarcaMaisEdu tamanho={tamanho} />
      <span>
        Mais<span className="text-primary">Edu</span>
      </span>
    </span>
  );
}
