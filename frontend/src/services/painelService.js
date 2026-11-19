import { chamarApi } from "./http";

/** Números da página Início do administrador. */
export function resumoDaInstituicao() {
  return chamarApi("/painel/instituicao");
}

/** Painel pedagógico da escola (admin): só dados agregados, sem identificar alunos. */
export function painelDaEscola() {
  return chamarApi("/painel/escola");
}
