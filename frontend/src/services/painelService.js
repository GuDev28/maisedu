import { chamarApi } from "./http";

/** Números da página Início do administrador. */
export function resumoDaInstituicao() {
  return chamarApi("/painel/instituicao");
}
