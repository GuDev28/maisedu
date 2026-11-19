import { chamarApi, query } from "./http";

/**
 * Lista paginada: {conteudo, pagina, tamanho, total, totalPaginas}. {@code pagina} começa em 0.
 * Admin vê a escola (filtro opcional por papel); professor vê os alunos das suas turmas.
 */
export function listarUsuarios({ papel, busca, pagina = 0, tamanho = 20 } = {}) {
  return chamarApi(`/usuarios${query({ papel, busca, pagina, tamanho })}`);
}

export function redefinirSenha(usuarioId) {
  return chamarApi(`/usuarios/${usuarioId}/redefinir-senha`, { method: "POST" });
}

/** Só alunos: admin ou professor de uma turma do aluno. */
export function corrigirEmailResponsavel(usuarioId, emailResponsavel) {
  return chamarApi(`/usuarios/${usuarioId}/email-responsavel`, {
    method: "PUT",
    body: JSON.stringify({ emailResponsavel }),
  });
}

export function corrigirNome(usuarioId, nome) {
  return chamarApi(`/usuarios/${usuarioId}`, { method: "PATCH", body: JSON.stringify({ nome }) });
}

export function desativarUsuario(usuarioId) {
  return chamarApi(`/usuarios/${usuarioId}/desativar`, { method: "POST" });
}

export function reativarUsuario(usuarioId) {
  return chamarApi(`/usuarios/${usuarioId}/reativar`, { method: "POST" });
}

export function exportarDadosPessoais(usuarioId) {
  return chamarApi(`/usuarios/${usuarioId}/dados-pessoais`);
}

export function anonimizarUsuario(usuarioId) {
  return chamarApi(`/usuarios/${usuarioId}/anonimizar`, { method: "POST" });
}
