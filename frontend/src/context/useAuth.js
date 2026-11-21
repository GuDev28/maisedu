import { useContext } from "react";
import { AuthContext } from "./contextos";

/** Usuário logado e as ações da sessão (login, logout, troca de senha e aceite dos termos). */
export function useAuth() {
  const contexto = useContext(AuthContext);
  if (!contexto) {
    throw new Error("useAuth precisa ser usado dentro de um AuthProvider");
  }
  return contexto;
}
