import { chamarApi } from "./http";

/** {@code prazo}: data e hora locais no formato do input datetime-local (ex.: 2026-11-30T23:59). */
export function criarAtividade(turmaId, titulo, prazo, questaoIds) {
  return chamarApi("/atividades", {
    method: "POST",
    body: JSON.stringify({ turmaId, titulo, prazo: comSegundos(prazo), questaoIds }),
  });
}

/** Estende ou antecipa o prazo (só antes do prazo atual terminar). */
export function alterarPrazo(atividadeId, prazo) {
  return chamarApi(`/atividades/${atividadeId}/prazo`, {
    method: "PUT",
    body: JSON.stringify({ prazo: comSegundos(prazo) }),
  });
}

/** O datetime-local não traz segundos; o back-end recebe LocalDateTime completo. */
function comSegundos(valor) {
  return valor && valor.length === 16 ? `${valor}:00` : valor;
}

export function listarAtividadesDaTurma(turmaId) {
  return chamarApi(`/turmas/${turmaId}/atividades`);
}

export function resultadosDaAtividade(atividadeId) {
  return chamarApi(`/atividades/${atividadeId}/resultados`);
}

export function desempenhoDaTurma(turmaId) {
  return chamarApi(`/turmas/${turmaId}/desempenho`);
}

export function desempenhoDoAlunoNaTurma(turmaId, alunoId) {
  return chamarApi(`/turmas/${turmaId}/alunos/${alunoId}/desempenho`);
}
