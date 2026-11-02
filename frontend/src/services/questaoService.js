import { chamarApi, query } from "./http";

export function listarTopicos() {
  return chamarApi("/topicos");
}

export function criarQuestao({ enunciado, topicoId, nivelDificuldade, anoEscolar, alternativas, indiceCorreta }) {
  return chamarApi("/questoes", {
    method: "POST",
    body: JSON.stringify({ enunciado, topicoId, nivelDificuldade, anoEscolar, alternativas, indiceCorreta }),
  });
}

export function enviarParaValidacao(questaoId) {
  return chamarApi(`/questoes/${questaoId}/enviar-para-validacao`, { method: "POST" });
}

export function listarMinhasQuestoes() {
  return chamarApi("/questoes/minhas");
}

export function listarQuestoesPendentes() {
  return chamarApi("/questoes/pendentes");
}

/** Banco compartilhado: questões aprovadas de qualquer instituição. */
export function listarAprovadas({ topicoId, nivel, anoEscolar } = {}) {
  return chamarApi(`/questoes/aprovadas${query({ topicoId, nivel, anoEscolar })}`);
}
