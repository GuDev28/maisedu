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
    return <p className="text-secondary">Nenhuma resposta registrada ainda.</p>;
  }

  return (
    <div>
      <div className="row g-3 mb-4">
        <div className="col-4">
          <div className="card h-100">
            <div className="card-body">
              <div className="me-numero">{desempenho.totalRespostas}</div>
              <div className="small text-secondary">respostas</div>
            </div>
          </div>
        </div>
        <div className="col-4">
          <div className="card h-100">
            <div className="card-body">
              <div className="me-numero">{desempenho.totalAcertos}</div>
              <div className="small text-secondary">acertos</div>
            </div>
          </div>
        </div>
        <div className="col-4">
          <div className="card h-100">
            <div className="card-body">
              <BarraPercentual valor={desempenho.percentualGeral} destaque />
              <div className="small text-secondary mt-1">acerto geral</div>
            </div>
          </div>
        </div>
      </div>

      <h2 className="h6 text-secondary text-uppercase">Por tópico</h2>
      <div className="card mb-4">
        <div className="table-responsive">
          <table className="table align-middle mb-0">
            <thead>
              <tr>
                <th>Tópico</th>
                <th className="text-end">Respostas</th>
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
                      <span className="badge text-bg-warning ms-2">precisa de atenção</span>
                    )}
                  </td>
                  <td className="text-end">{topico.totalRespostas}</td>
                  <td>
                    <BarraPercentual valor={topico.percentual} />
                  </td>
                  <td className="small">
                    {topico.niveis.map((nivel) => (
                      <div key={nivel.nivelDificuldade}>
                        {rotuloNivel(nivel.nivelDificuldade)}: {nivel.totalAcertos}/{nivel.totalRespostas}
                        {nivel.dominado && <span className="badge text-bg-success ms-1">dominado</span>}
                      </div>
                    ))}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <h2 className="h6 text-secondary text-uppercase">Evolução</h2>
      <div className="card">
        <div className="card-body">
          <GraficoEvolucao pontos={desempenho.evolucao} />
        </div>
      </div>
    </div>
  );
}
