import { chamarApi } from "./http";


export function avaliarQuestao(questaoId, voto) {
  return chamarApi(`/questoes/${questaoId}/avaliacoes`, {
    method: "POST",
    body: JSON.stringify({ voto }),
  });
}
