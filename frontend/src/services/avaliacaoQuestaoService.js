const API_URL = "http://localhost:8080";

async function tratarResposta(response) {
  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const mensagem = data?.mensagem || "Erro inesperado ao processar a solicitação.";
    throw new Error(mensagem);
  }

  return data;
}

export async function avaliarQuestao(questaoId, professorId, voto) {
  const response = await fetch(`${API_URL}/questoes/${questaoId}/avaliacoes`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ professorId: Number(professorId), voto }),
  });
  return tratarResposta(response);
}
