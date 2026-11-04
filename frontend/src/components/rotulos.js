export const ANOS_ESCOLARES = [
  { valor: "SEXTO_ANO", rotulo: "6º ano" },
  { valor: "SETIMO_ANO", rotulo: "7º ano" },
  { valor: "OITAVO_ANO", rotulo: "8º ano" },
  { valor: "NONO_ANO", rotulo: "9º ano" },
];

export const NIVEIS = [
  { valor: "FACIL", rotulo: "Fácil" },
  { valor: "MEDIO", rotulo: "Médio" },
  { valor: "DIFICIL", rotulo: "Difícil" },
];

export const PAPEIS = { ADMIN: "Administrador", PROFESSOR: "Professor", ALUNO: "Aluno" };

export function rotuloAno(valor) {
  return ANOS_ESCOLARES.find((a) => a.valor === valor)?.rotulo ?? (valor || "—");
}

export function rotuloNivel(valor) {
  return NIVEIS.find((n) => n.valor === valor)?.rotulo ?? valor;
}

export function formatarPercentual(valor) {
  if (valor === null || valor === undefined) {
    return "—";
  }
  return `${Number(valor).toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%`;
}

export function formatarDataHora(valor) {
  if (!valor) {
    return "—";
  }
  return new Date(valor).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

export function formatarData(valor) {
  if (!valor) {
    return "—";
  }
  const [ano, mes, dia] = String(valor).slice(0, 10).split("-");
  return `${dia}/${mes}/${ano}`;
}
