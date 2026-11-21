import { useContext } from "react";
import { InterfaceContext } from "./contextos";

/** Avisos que somem sozinhos ({@code avisar}) e confirmações em modal ({@code confirmar}). */
export function useInterface() {
  const contexto = useContext(InterfaceContext);
  if (!contexto) {
    throw new Error("useInterface precisa ser usado dentro de um InterfaceProvider");
  }
  return contexto;
}
