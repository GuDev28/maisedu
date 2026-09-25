import { chamarApi } from "./http";

export function vincularAluno(turmaId, alunoId) {
  return chamarApi(`/turmas/${turmaId}/alunos/${alunoId}`, { method: "POST" });
}

export function vincularProfessor(turmaId, professorId) {
  return chamarApi(`/turmas/${turmaId}/professores/${professorId}`, { method: "POST" });
}
