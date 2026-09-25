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

export function trocarSenha(senhaAtual, novaSenha) {
  return chamarApi("/auth/trocar-senha", {
    method: "POST",
    body: JSON.stringify({ senhaAtual, novaSenha }),
  });
}
