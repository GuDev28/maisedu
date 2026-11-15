import { chamarApi } from "./http";

export function listarTurmasParaVinculo() {
  return chamarApi("/turmas");
}

/** Professores e alunos da turma (tela da Turma, aba Pessoas). */
export function listarMembros(turmaId) {
  return chamarApi(`/turmas/${turmaId}/membros`);
}

export function desvincularAluno(turmaId, alunoId) {
  return chamarApi(`/turmas/${turmaId}/alunos/${alunoId}`, { method: "DELETE" });
}

export function desvincularProfessor(turmaId, professorId) {
  return chamarApi(`/turmas/${turmaId}/professores/${professorId}`, { method: "DELETE" });
}

export function listarAlunosDisponiveis(turmaId) {
  return chamarApi(`/turmas/${turmaId}/alunos-disponiveis`);
}

export function listarProfessoresDisponiveis(turmaId) {
  return chamarApi(`/turmas/${turmaId}/professores-disponiveis`);
}

export function vincularAluno(turmaId, alunoId) {
  return chamarApi(`/turmas/${turmaId}/alunos/${alunoId}`, { method: "POST" });
}

export function vincularProfessor(turmaId, professorId) {
  return chamarApi(`/turmas/${turmaId}/professores/${professorId}`, { method: "POST" });
}
