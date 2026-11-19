/** Marca do MaisEdu (logo da equipe, em azul #195ABE). */
export function MarcaMaisEdu({ tamanho = 32 }) {
  return (
    <img
      src="/logo-maisedu-192.png"
      width={tamanho}
      height={tamanho}
      alt=""
      aria-hidden="true"
      className="me-logo-imagem"
    />
  );
}

export function Logo({ tamanho = 32 }) {
  return (
    <span className="me-logo">
      <MarcaMaisEdu tamanho={tamanho} />
      <span>MaisEdu</span>
    </span>
  );
}
