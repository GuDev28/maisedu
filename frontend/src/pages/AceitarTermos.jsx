import { useState } from "react";
import { useAuth } from "../context/useAuth";
import { AceiteTermos } from "../components/AceiteTermos";
import { Logo } from "../components/Logo";
import { Mensagens } from "../components/Mensagens";
import { RodapeLegal } from "../components/RodapeLegal";

/** Exibida quando há uma versão nova dos Termos de Uso / Política de Privacidade ainda não aceita. */
export default function AceitarTermos() {
  const { aceitarTermos, logout } = useAuth();
  const [aceite, setAceite] = useState(false);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    if (!aceite) {
      setErro("É preciso aceitar os Termos de Uso e a Política de Privacidade para continuar.");
      return;
    }
    setCarregando(true);
    setErro(null);
    try {
      await aceitarTermos();
    } catch (err) {
      setErro(err.message);
      setCarregando(false);
    }
  }

  return (
    <div className="me-tela-publica">
      <div className="w-100 me-largura-sm">
        <div className="text-center mb-4">
          <Logo tamanho={40} />
        </div>
        <div className="card shadow-sm">
          <div className="card-body p-4">
            <h1 className="h5">Documentos atualizados</h1>
            <p className="text-secondary">
              Os Termos de Uso e a Política de Privacidade do MaisEdu mudaram. Leia as novas versões e confirme o
              aceite para continuar usando o sistema.
            </p>
            <Mensagens erro={erro} />
            <form onSubmit={handleSubmit}>
              <AceiteTermos marcado={aceite} aoMudar={setAceite} />
              <div className="d-flex gap-2">
                <button type="submit" className="btn btn-primary flex-grow-1" disabled={carregando}>
                  {carregando ? "Salvando..." : "Continuar"}
                </button>
                <button type="button" className="btn btn-outline-secondary" onClick={logout}>
                  Sair
                </button>
              </div>
            </form>
          </div>
        </div>
        <RodapeLegal />
      </div>
    </div>
  );
}
