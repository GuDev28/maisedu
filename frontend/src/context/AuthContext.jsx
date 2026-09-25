import { createContext, useContext, useEffect, useState } from "react";
import * as authService from "../services/authService";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null);
  // null = ainda não sabemos (checando o cookie); depois vira true/false
  const [carregandoSessao, setCarregandoSessao] = useState(true);

  useEffect(() => {
    authService
      .usuarioAtual()
      .then(setUsuario)
      .catch(() => setUsuario(null))
      .finally(() => setCarregandoSessao(false));
  }, []);

  async function login(identificador, senha) {
    const usuarioLogado = await authService.login(identificador, senha);
    setUsuario(usuarioLogado);
    return usuarioLogado;
  }

  async function logout() {
    try {
      await authService.logout();
    } finally {
      setUsuario(null);
    }
  }

  async function trocarSenha(senhaAtual, novaSenha) {
    await authService.trocarSenha(senhaAtual, novaSenha);
    // o backend já invalidou o cookie antigo: força um novo login
    setUsuario(null);
  }

  return (
    <AuthContext.Provider value={{ usuario, carregandoSessao, login, logout, trocarSenha }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const contexto = useContext(AuthContext);
  if (!contexto) {
    throw new Error("useAuth precisa ser usado dentro de um AuthProvider");
  }
  return contexto;
}
