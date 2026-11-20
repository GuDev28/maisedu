/**
 * Círculo com as iniciais da pessoa (sem foto: o MaisEdu não guarda imagem de ninguém). A cor sai
 * do nome, então a mesma pessoa tem sempre a mesma cor; as cinco combinações vêm da paleta da
 * plataforma e têm contraste para texto pequeno.
 */
const CORES = ["me-avatar-azul", "me-avatar-violeta", "me-avatar-verde", "me-avatar-ambar", "me-avatar-cinza"];

function iniciais(nome = "") {
  const partes = nome.trim().split(/\s+/).filter(Boolean);
  if (partes.length === 0) {
    return "?";
  }
  const primeira = partes[0][0];
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : "";
  return (primeira + ultima).toUpperCase();
}

function corDoNome(nome = "") {
  let soma = 0;
  for (let i = 0; i < nome.length; i++) {
    soma = (soma * 31 + nome.charCodeAt(i)) % 1000003;
  }
  return CORES[soma % CORES.length];
}

export function Avatar({ nome, tamanho = 32 }) {
  return (
    <span
      className={`me-avatar ${corDoNome(nome)}`}
      style={{ width: tamanho, height: tamanho, fontSize: Math.round(tamanho * 0.4) }}
      aria-hidden="true"
    >
      {iniciais(nome)}
    </span>
  );
}

/** Autor de uma questão: iniciais, nome e, quando houver, a escola de origem. */
export function Autor({ nome, escola, prefixo }) {
  return (
    <span className="me-autor">
      <Avatar nome={nome} tamanho={32} />
      <span className="me-autor-texto">
        <span className="me-autor-nome">
          {prefixo && <span className="text-secondary fw-normal">{prefixo} </span>}
          {nome}
        </span>
        {escola && <span className="me-autor-escola">{escola}</span>}
      </span>
    </span>
  );
}
