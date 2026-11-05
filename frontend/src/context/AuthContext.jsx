import { createContext, useContext, useEffect, useRef, useState } from "react";
import * as authService from "../services/authService";
import { EVENTO_SESSAO_EXPIRADA } from "../services/http";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null);
  // null = ainda não sabemos (checando o cookie); depois vira true/false
  const [carregandoSessao, setCarregandoSessao] = useState(true);
  const [avisoSessao, setAvisoSessao] = useState(null);
  const usuarioRef = useRef(null);

  useEffect(() => {
    usuarioRef.current = usuario;
  }, [usuario]);

  useEffect(() => {
    authService
      .usuarioAtual()
      .then(setUsuario)
      .catch(() => setUsuario(null))
      .finally(() => setCarregandoSessao(false));
  }, []);

  // Token expirado (120 min) ou sessão encerrada no servidor (logout em outra aba, senha
  // redefinida, conta desativada): volta para a tela de login com um aviso.
  useEffect(() => {
    function aoExpirar() {
      if (usuarioRef.current) {
        setAvisoSessao("Sua sessão foi encerrada. Entre novamente.");
      }
      setUsuario(null);
    }
    window.addEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar);
    return () => window.removeEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar);
  }, []);

  async function login(identificador, senha) {
    const usuarioLogado = await authService.login(identificador, senha);
    setAvisoSessao(null);
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

  async function trocarSenha(senhaAtual, novaSenha, aceiteTermos) {
    await authService.trocarSenha(senhaAtual, novaSenha, aceiteTermos);
    // o backend já invalidou o cookie antigo: força um novo login
    setUsuario(null);
  }

  async function aceitarTermos() {
    await authService.aceitarTermos();
    setUsuario(await authService.usuarioAtual());
  }

  return (
    <AuthContext.Provider
      value={{ usuario, carregandoSessao, avisoSessao, login, logout, trocarSenha, aceitarTermos }}
    >
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
