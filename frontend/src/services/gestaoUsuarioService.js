import { chamarApi, query } from "./http";

export function listarUsuarios(papel) {
  return chamarApi(`/usuarios${query({ papel })}`);
}

export function redefinirSenha(usuarioId) {
  return chamarApi(`/usuarios/${usuarioId}/redefinir-senha`, { method: "POST" });
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
