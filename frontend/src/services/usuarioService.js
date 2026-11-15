import { chamarApi } from "./http";

/** Com {@code turmaId}, o professor já sai vinculado à turma (cadastro e vínculo na mesma operação). */
export function cadastrarProfessor(nome, email, turmaId) {
  return chamarApi("/professores", {
    method: "POST",
    body: JSON.stringify({ nome, email, turmaId: turmaId || null }),
  });
}

/**
 * O aluno não tem e-mail: o login e a senha provisória vão para o e-mail do responsável, e não voltam
 * para quem cadastrou.
 */
export function cadastrarAluno(nome, login, emailResponsavel, turmaId) {
  return chamarApi("/alunos", {
    method: "POST",
    body: JSON.stringify({ nome, login, emailResponsavel, turmaId: turmaId || null }),
  });
}
