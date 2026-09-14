const API_URL = "http://localhost:8080";

async function tratarResposta(response) {
  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const mensagem = data?.mensagem || "Erro inesperado ao processar a solicitação.";
    throw new Error(mensagem);
  }

  return data;
}

export async function vincularAluno(turmaId, alunoId) {
  const response = await fetch(`${API_URL}/turmas/${turmaId}/alunos/${alunoId}`, {
    method: "POST",
  });
  return tratarResposta(response);
}

export async function vincularProfessor(turmaId, professorId) {
  const response = await fetch(`${API_URL}/turmas/${turmaId}/professores/${professorId}`, {
    method: "POST",
  });
  return tratarResposta(response);
}
