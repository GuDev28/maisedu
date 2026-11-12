import { chamarApi } from "./http";

export function listarMinhasAtividades() {
  return chamarApi("/aluno/atividades");
}

export function iniciarAtividade(atividadeId) {
  return chamarApi(`/aluno/atividades/${atividadeId}/iniciar`, { method: "POST" });
}

export function abrirFolha(atividadeAlunoId) {
  return chamarApi(`/aluno/folhas/${atividadeAlunoId}`);
}

/** Guarda a alternativa escolhida sem confirmar (não é resposta; pode ser trocada). */
export function marcarAlternativa(atividadeAlunoId, atividadeAlunoQuestaoId, alternativaId) {
  return chamarApi(`/aluno/folhas/${atividadeAlunoId}/questoes/${atividadeAlunoQuestaoId}/marcacao`, {
    method: "PUT",
    body: JSON.stringify({ alternativaId }),
  });
}

export function responderQuestao(atividadeAlunoId, atividadeAlunoQuestaoId, alternativaId) {
  return chamarApi(`/aluno/folhas/${atividadeAlunoId}/questoes/${atividadeAlunoQuestaoId}/resposta`, {
    method: "POST",
    body: JSON.stringify({ alternativaId }),
  });
}

export function gerarPraticaAdaptativa() {
  return chamarApi("/aluno/praticas-adaptativas", { method: "POST" });
}

export function meuDesempenho() {
  return chamarApi("/aluno/desempenho");
}
