import { useEffect, useState } from "react";
import { listarTopicos } from "../services/questaoService";
import { ANOS_ESCOLARES, NIVEIS } from "./rotulos";

/** Filtros do banco compartilhado (tópico, nível e ano), usados no Banco de questões e na Nova atividade. */
export function FiltrosBanco({ filtros, aoMudar, aoBuscar, buscando, className = "mb-3" }) {
  const [topicos, setTopicos] = useState([]);

  useEffect(() => {
    listarTopicos().then(setTopicos).catch(() => setTopicos([]));
  }, []);

  function mudar(campo, valor) {
    aoMudar({ ...filtros, [campo]: valor });
  }

  return (
    <form
      className={`row g-2 align-items-end ${className}`}
      onSubmit={(evento) => {
        evento.preventDefault();
        aoBuscar();
      }}
    >
      <div className="col-sm-4">
        <label htmlFor="filtro-topico" className="form-label small text-secondary mb-1">
          Tópico
        </label>
        <select id="filtro-topico" className="form-select" value={filtros.topicoId} onChange={(e) => mudar("topicoId", e.target.value)}>
          <option value="">Todos</option>
          {topicos.map((topico) => (
            <option key={topico.id} value={topico.id}>
              {topico.nome}
            </option>
          ))}
        </select>
      </div>
      <div className="col-6 col-sm-3">
        <label htmlFor="filtro-nivel" className="form-label small text-secondary mb-1">
          Nível
        </label>
        <select id="filtro-nivel" className="form-select" value={filtros.nivel} onChange={(e) => mudar("nivel", e.target.value)}>
          <option value="">Todos</option>
          {NIVEIS.map((nivel) => (
            <option key={nivel.valor} value={nivel.valor}>
              {nivel.rotulo}
            </option>
          ))}
        </select>
      </div>
      <div className="col-6 col-sm-3">
        <label htmlFor="filtro-ano" className="form-label small text-secondary mb-1">
          Ano
        </label>
        <select id="filtro-ano" className="form-select" value={filtros.anoEscolar} onChange={(e) => mudar("anoEscolar", e.target.value)}>
          <option value="">Todos</option>
          {ANOS_ESCOLARES.map((ano) => (
            <option key={ano.valor} value={ano.valor}>
              {ano.rotulo}
            </option>
          ))}
        </select>
      </div>
      <div className="col-sm-2 d-grid">
        <button type="submit" className="btn btn-outline-primary" disabled={buscando}>
          {buscando ? "Buscando..." : "Filtrar"}
        </button>
      </div>
    </form>
  );
}
