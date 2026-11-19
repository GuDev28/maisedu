import { chamarApi } from "./http";

/** Com {@code turmaId}, o professor já sai vinculado à turma (cadastro e vínculo na mesma operação). */
export function cadastrarProfessor(nome, email, turmaId) {
  return chamarApi("/professores", {
    method: "POST",
    body: JSON.stringify({ nome, email, turmaId: turmaId || null }),
  });
}

/**
 * O aluno não tem e-mail: o login é gerado pelo MaisEdu e, com a senha provisória, vai para o e-mail
 * do responsável. A resposta traz o login gerado e se o e-mail saiu (statusEmailAcesso); a senha não
 * volta para quem cadastrou.
 */
export function cadastrarAluno(nome, emailResponsavel, turmaId) {
  return chamarApi("/alunos", {
    method: "POST",
    body: JSON.stringify({ nome, emailResponsavel, turmaId: turmaId || null }),
  });
}
