import { chamarApi } from "./http";

export function criarAtividade(turmaId, titulo, questaoIds) {
  return chamarApi("/atividades", {
    method: "POST",
    body: JSON.stringify({ turmaId, titulo, questaoIds }),
  });
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
