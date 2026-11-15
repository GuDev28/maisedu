import { useAuth } from "../context/AuthContext";
import InicioAdmin from "./inicio/InicioAdmin";
import InicioAluno from "./inicio/InicioAluno";
import InicioProfessor from "./inicio/InicioProfessor";

/** Página Início: uma visão geral por perfil, no lugar de cair direto numa lista. */
export default function Inicio() {
  const { usuario } = useAuth();
  if (usuario.roles.includes("ADMIN")) {
    return <InicioAdmin />;
  }
  if (usuario.roles.includes("PROFESSOR")) {
    return <InicioProfessor />;
  }
  return <InicioAluno />;
}
