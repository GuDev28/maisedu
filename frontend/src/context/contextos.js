import { createContext } from "react";

/**
 * Os contextos ficam num arquivo só deles, separados dos provedores (AuthContext.jsx e
 * InterfaceContext.jsx) e dos hooks (useAuth.js e useInterface.js): assim cada arquivo .jsx exporta
 * só componentes, e o recarregamento rápido do Vite (fast refresh) continua funcionando.
 */
export const AuthContext = createContext(null);
export const InterfaceContext = createContext(null);
