import { chamarApi } from "./http";

/** Voto na validação colaborativa (RN02). Ao rejeitar, o motivo é obrigatório: é o que o autor lê para corrigir. */
export function avaliarQuestao(questaoId, voto, comentario) {
  return chamarApi(`/questoes/${questaoId}/avaliacoes`, {
    method: "POST",
    body: JSON.stringify({ voto, comentario: comentario || null }),
  });
}
