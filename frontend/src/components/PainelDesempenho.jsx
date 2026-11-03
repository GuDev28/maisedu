import { BarraPercentual } from "./BarraPercentual";
import { GraficoEvolucao } from "./GraficoEvolucao";
import { rotuloNivel } from "./rotulos";

/**
 * Desempenho de um aluno por tópico e nível (RF19). Os números são os mesmos que o motor
 * adaptativo usa (RN05). Tópicos do menor para o maior percentual.
 */
export function PainelDesempenho({ desempenho }) {
  if (!desempenho) {
    return null;
  }
  if (desempenho.totalRespostas === 0) {
    return <p>Nenhuma resposta registrada ainda.</p>;
  }

  return (
    <div>
      <div className="resumo-numeros">
        <div className="cartao-numero">
          <span className="numero">{desempenho.totalRespostas}</span>
          <span className="texto-suave">respostas</span>
        </div>
        <div className="cartao-numero">
          <span className="numero">{desempenho.totalAcertos}</span>
          <span className="texto-suave">acertos</span>
        </div>
        <div className="cartao-numero">
          <BarraPercentual valor={desempenho.percentualGeral} destaque />
          <span className="texto-suave">acerto geral</span>
        </div>
      </div>

      <h3>Por tópico</h3>
      <table className="tabela">
        <thead>
          <tr>
            <th>Tópico</th>
            <th>Respostas</th>
            <th>Acerto</th>
            <th>Por nível</th>
          </tr>
        </thead>
        <tbody>
          {desempenho.topicos.map((topico, indice) => (
            <tr key={topico.topicoId}>
              <td>
                {topico.topico}
                {indice === 0 && desempenho.topicos.length > 1 && (
                  <span className="etiqueta etiqueta-alerta">precisa de atenção</span>
                )}
              </td>
              <td>{topico.totalRespostas}</td>
              <td>
                <BarraPercentual valor={topico.percentual} />
              </td>
              <td>
                {topico.niveis.map((nivel) => (
                  <div key={nivel.nivelDificuldade} className="linha-nivel">
                    {rotuloNivel(nivel.nivelDificuldade)}: {nivel.totalAcertos}/{nivel.totalRespostas}
                    {nivel.dominado && <span className="etiqueta etiqueta-ok">dominado</span>}
                  </div>
                ))}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <h3>Evolução</h3>
      <GraficoEvolucao pontos={desempenho.evolucao} />
    </div>
  );
}
