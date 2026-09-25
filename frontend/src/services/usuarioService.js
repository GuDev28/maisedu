import { chamarApi } from "./http";

export function cadastrarProfessor(nome, email) {
  return chamarApi("/professores", {
    method: "POST",
    body: JSON.stringify({ nome, email }),
  });
}

export function cadastrarAluno(nome, login) {
  return chamarApi("/alunos", {
    method: "POST",
    body: JSON.stringify({ nome, login }),
  });
}
