// Por padrão a API é acessada pela mesma origem do front, no prefixo /api
// (proxy do Vite em desenvolvimento e do nginx no docker compose).
export const API_URL = import.meta.env.VITE_API_URL ?? "/api";

/** Evento disparado quando a sessão expira ou é encerrada no servidor (HTTP 401). */
export const EVENTO_SESSAO_EXPIRADA = "maisedu:sessao-expirada";

const ROTAS_SEM_AVISO_DE_SESSAO = ["/auth/login", "/auth/me"];

async function tratarResposta(response, caminho) {
  if (response.status === 204) {
    return null;
  }
  const data = await response.json().catch(() => null);

  if (!response.ok) {
    if (response.status === 401 && !ROTAS_SEM_AVISO_DE_SESSAO.includes(caminho)) {
      window.dispatchEvent(new CustomEvent(EVENTO_SESSAO_EXPIRADA, { detail: data?.mensagem }));
    }
    // Sem {"mensagem"} a resposta não veio dos handlers do back-end (proxy, CORS, servidor fora do ar):
    // o código HTTP ajuda a descobrir onde parou.
    const mensagem = data?.mensagem || `Erro inesperado ao processar a solicitação (HTTP ${response.status}).`;
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
  return tratarResposta(response, caminho);
}

/** Monta a query string ignorando filtros vazios. */
export function query(parametros) {
  const busca = new URLSearchParams();
  for (const [chave, valor] of Object.entries(parametros)) {
    if (valor !== undefined && valor !== null && valor !== "") {
      busca.append(chave, valor);
    }
  }
  const texto = busca.toString();
  return texto ? `?${texto}` : "";
}

/** Oferece um objeto JSON para download (usado na exportação de dados pessoais). */
export function baixarJson(objeto, nomeArquivo) {
  const blob = new Blob([JSON.stringify(objeto, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = nomeArquivo;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}