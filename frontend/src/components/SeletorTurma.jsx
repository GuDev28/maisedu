import { useEffect, useState } from "react";
import { listarMinhasTurmas } from "../services/turmaService";
import { rotuloAno } from "./rotulos";

/**
 * Seletor das turmas em que o usuário atua (admin: todas as ativas; professor: as que leciona).
 * Já escolhe a primeira turma, ou a informada em {@code turmaInicial}.
 */
export function SeletorTurma({ valor, aoMudar, turmaInicial, rotulo = "Turma" }) {
  const [turmas, setTurmas] = useState(null);
  const [erro, setErro] = useState(null);

  // só na montagem: a lista de turmas não muda enquanto a tela está aberta
  useEffect(() => {
    let ativo = true;
    listarMinhasTurmas()
      .then((lista) => ativo && setTurmas(lista))
      .catch((err) => ativo && setErro(err.message));
    return () => {
      ativo = false;
    };
  }, []);

  // Com a lista na mão e nenhuma turma escolhida, escolhe a informada em turmaInicial (ou a primeira).
  useEffect(() => {
    if (!valor && turmas?.length > 0) {
      const inicial = turmas.find((turma) => String(turma.id) === String(turmaInicial)) ?? turmas[0];
      aoMudar(inicial.id);
    }
  }, [turmas, valor, turmaInicial, aoMudar]);

  if (erro) {
    return <div className="alert alert-danger py-2">{erro}</div>;
  }
  if (turmas === null) {
    return <div className="placeholder-glow"><span className="placeholder col-4" /></div>;
  }
  if (turmas.length === 0) {
    return (
      <div className="alert alert-info py-2 mb-0">
        Você ainda não está vinculado a nenhuma turma ativa. O administrador da escola faz esse vínculo.
      </div>
    );
  }
  return (
    <div className="d-flex align-items-center gap-2">
      <label htmlFor="seletor-turma" className="form-label mb-0 text-secondary small">
        {rotulo}
      </label>
      <select
        id="seletor-turma"
        className="form-select form-select-sm w-auto"
        value={valor ?? ""}
        onChange={(evento) => aoMudar(Number(evento.target.value))}
      >
        {turmas.map((turma) => (
          <option key={turma.id} value={turma.id}>
            {turma.nome} · {rotuloAno(turma.anoEscolar)}
          </option>
        ))}
      </select>
    </div>
  );
}
