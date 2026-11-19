import { chamarApi, query } from "./http";

export function listarMinhasAtividades() {
  return chamarApi("/aluno/atividades");
}

export function iniciarAtividade(atividadeId) {
  return chamarApi(`/aluno/atividades/${atividadeId}/iniciar`, { method: "POST" });
}

export function abrirFolha(atividadeAlunoId) {
  return chamarApi(`/aluno/folhas/${atividadeAlunoId}`);
}

/** Guarda a alternativa escolhida (não é resposta: pode ser trocada até o envio e dentro do prazo). */
export function marcarAlternativa(atividadeAlunoId, atividadeAlunoQuestaoId, alternativaId) {
  return chamarApi(`/aluno/folhas/${atividadeAlunoId}/questoes/${atividadeAlunoQuestaoId}/marcacao`, {
    method: "PUT",
    body: JSON.stringify({ alternativaId }),
  });
}

/**
 * Entrega única: envia a alternativa escolhida em cada questão. O servidor recusa se faltar alguma
 * questão, se o prazo já terminou ou se a folha já foi enviada. Devolve a folha depois do envio.
 */
export function enviarFolha(atividadeAlunoId, respostas) {
  return chamarApi(`/aluno/folhas/${atividadeAlunoId}/envio`, {
    method: "POST",
    body: JSON.stringify({ respostas }),
  });
}

/** Tela Praticar: a prática recomendada e um card por tópico. */
export function painelDePratica() {
  return chamarApi("/aluno/praticas");
}

/** Gera (ou retoma) a prática recomendada pelo motor ou, com o tópico, a prática desse tópico. */
export function gerarPraticaAdaptativa(topicoId) {
  return chamarApi(`/aluno/praticas-adaptativas${query({ topicoId })}`, { method: "POST" });
}

export function meuDesempenho() {
  return chamarApi("/aluno/desempenho");
}
