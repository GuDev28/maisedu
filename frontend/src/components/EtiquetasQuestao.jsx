import { STATUS_QUESTAO, rotuloAno, rotuloNivel } from "./rotulos";

/** Tópico, nível e ano da questão, e o status quando informado. */
export function EtiquetasQuestao({ questao, mostrarStatus = false }) {
  const status = STATUS_QUESTAO[questao.status];
  return (
    <span className="d-inline-flex flex-wrap gap-1 align-items-center small">
      {mostrarStatus && status && <span className={`badge text-bg-${status.cor}`}>{status.rotulo}</span>}
      <span className="badge text-bg-light border">{questao.topico}</span>
      <span className="badge text-bg-light border">{rotuloNivel(questao.nivelDificuldade)}</span>
      {questao.anoEscolar && <span className="badge text-bg-light border">{rotuloAno(questao.anoEscolar)}</span>}
    </span>
  );
}

/** Alternativas com a correta destacada (para quem vê o gabarito: autor, avaliador e professor). */
export function AlternativasComGabarito({ alternativas }) {
  return (
    <ol type="A" className="mb-0 ps-4 small">
      {alternativas.map((alternativa) => (
        <li key={alternativa.id ?? alternativa.letra} className={alternativa.correta ? "fw-semibold text-success" : ""}>
          {alternativa.texto}
          {alternativa.correta && <span className="visually-hidden"> (correta)</span>}
          {alternativa.correta && " ✓"}
        </li>
      ))}
    </ol>
  );
}
