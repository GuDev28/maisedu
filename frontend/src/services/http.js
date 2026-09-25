export const API_URL = "http://localhost:8080";

async function tratarResposta(response) {
  if (response.status === 204) {
    return null;
  }
  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const mensagem = data?.mensagem || "Erro inesperado ao processar a solicitação.";
    const erro = new Error(mensagem);
    erro.status = response.status;
    throw erro;
  }

  return data;
}

export async function chamarApi(caminho, opcoes = {}) {
  const response = await fetch(`${API_URL}${caminho}`, {
    credentials: "include",
    headers: opcoes.body ? { "Content-Type": "application/json" } : undefined,
    ...opcoes,
  });
  return tratarResposta(response);
}
