import { Icone } from "./Icone";
import { STATUS_QUESTAO, rotuloAno, rotuloNivel } from "./rotulos";

/** Quantas bolinhas cheias cada nível mostra (o texto do nível vem junto: a bolinha só reforça). */
const PONTOS_DO_NIVEL = { FACIL: 1, MEDIO: 2, DIFICIL: 3 };

function PontosNivel({ nivel }) {
  const cheios = PONTOS_DO_NIVEL[nivel] ?? 0;
  return (
    <span className="me-nivel-pontos" aria-hidden="true">
      {[1, 2, 3].map((ponto) => (
        <span key={ponto} className={ponto <= cheios ? "cheio" : ""} />
      ))}
    </span>
  );
}

/** Tópico, nível e ano da questão, e o status quando informado. */
export function EtiquetasQuestao({ questao, mostrarStatus = false }) {
  const status = STATUS_QUESTAO[questao.status];
  return (
    <span className="me-etiquetas">
      {mostrarStatus && status && (
        <span className={`me-chip me-chip-status me-chip-${questao.status.toLowerCase()}`}>{status.rotulo}</span>
      )}
      <span className="me-chip me-chip-topico">{questao.topico}</span>
      <span className="me-chip">
        <PontosNivel nivel={questao.nivelDificuldade} />
        {rotuloNivel(questao.nivelDificuldade)}
      </span>
      {questao.anoEscolar && <span className="me-chip">{rotuloAno(questao.anoEscolar)}</span>}
    </span>
  );
}

const LETRAS = ["A", "B", "C", "D", "E"];

/** Alternativas com a correta destacada (para quem vê o gabarito: autor, avaliador e professor). */
export function AlternativasComGabarito({ alternativas }) {
  return (
    <ol className="me-alternativas">
      {alternativas.map((alternativa, indice) => (
        <li key={alternativa.id ?? alternativa.letra ?? indice} className={alternativa.correta ? "me-alternativa-correta" : ""}>
          <span className="me-alternativa-letra">{alternativa.letra ?? LETRAS[indice]}</span>
          <span className="me-alternativa-texto">{alternativa.texto}</span>
          {alternativa.correta && (
            <span className="me-alternativa-gabarito">
              <Icone nome="check" tamanho={16} />
              correta
            </span>
          )}
        </li>
      ))}
    </ol>
  );
}
