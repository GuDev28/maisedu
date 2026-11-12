import { chamarApi } from "./http";

export function login(identificador, senha) {
  return chamarApi("/auth/login", {
    method: "POST",
    body: JSON.stringify({ identificador, senha }),
  });
}

export function logout() {
  return chamarApi("/auth/logout", { method: "POST" });
}

export function usuarioAtual() {
  return chamarApi("/auth/me");
}

/** Tela "Minha conta". */
export function minhaConta() {
  return chamarApi("/auth/conta");
}

/** Os dados pessoais do próprio usuário, em JSON (LGPD, art. 18). */
export function meusDados() {
  return chamarApi("/auth/meus-dados");
}

export function trocarSenha(senhaAtual, novaSenha, aceiteTermos) {
  return chamarApi("/auth/trocar-senha", {
    method: "POST",
    body: JSON.stringify({ senhaAtual, novaSenha, aceiteTermos }),
  });
}

export function aceitarTermos() {
  return chamarApi("/auth/aceitar-termos", {
    method: "POST",
    body: JSON.stringify({ aceiteTermos: true }),
  });
}
