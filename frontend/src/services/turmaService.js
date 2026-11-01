import { chamarApi } from "./http";

export function cadastrarTurma(nome, anoEscolar) {
  return chamarApi("/turmas", { method: "POST", body: JSON.stringify({ nome, anoEscolar }) });
}

/** Todas as turmas da instituição, inclusive inativas (admin). */
export function listarTurmasParaGestao() {
  return chamarApi("/turmas/gestao");
}

/** Turmas ativas em que o usuário atua (admin: todas; professor: as que leciona). */
export function listarMinhasTurmas() {
  return chamarApi("/turmas");
}

export function desativarTurma(turmaId) {
  return chamarApi(`/turmas/${turmaId}/desativar`, { method: "POST" });
}

export function reativarTurma(turmaId) {
  return chamarApi(`/turmas/${turmaId}/reativar`, { method: "POST" });
}
