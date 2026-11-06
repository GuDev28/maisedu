import { BarraPercentual } from "./BarraPercentual";
import { CartaoNumero } from "./CartaoNumero";
import { GraficoBarras } from "./GraficoBarras";
import { GraficoEvolucao } from "./GraficoEvolucao";
import { formatarPercentual, rotuloNivel } from "./rotulos";

/**
 * Painel de desempenho de um aluno (RF19): indicadores, acerto por tópico, evolução semanal e o detalhe
 * por tópico e nível. Os números são os mesmos que o motor adaptativo usa (RN05). Tópicos do menor para o
 * maior percentual.
 */
export function PainelDesempenho({ desempenho }) {
  if (!desempenho) {
    return null;
  }
  if (desempenho.totalRespostas === 0) {
    return <p>Nenhuma resposta registrada ainda.</p>;
  }

  let niveisDominados = 0;
  let niveisComRespostas = 0;
  for (const topico of desempenho.topicos) {
    for (const nivel of topico.niveis) {
      niveisComRespostas++;
      if (nivel.dominado) {
        niveisDominados++;
      }
    }
  }
  const maisFraco = desempenho.topicos[0];

  const barras = desempenho.topicos.map((topico) => ({
    chave: topico.topicoId,
    rotulo: topico.topico,
    valor: topico.percentual,
    detalhe: `${topico.totalAcertos} acertos em ${topico.totalRespostas} respostas`,
  }));

  return (
    <div>
      <div className="resumo-numeros">
        <CartaoNumero valor={desempenho.totalRespostas} rotulo="respostas" />
        <CartaoNumero valor={desempenho.totalAcertos} rotulo="acertos" />
        <CartaoNumero rotulo="acerto geral">
          <BarraPercentual valor={desempenho.percentualGeral} destaque />
        </CartaoNumero>
        <CartaoNumero valor={`${niveisDominados}/${niveisComRespostas}`} rotulo="níveis dominados"
                      detalhe="5+ respostas e 70%+ de acerto" />
        {maisFraco && desempenho.topicos.length > 1 && (
          <CartaoNumero valor={maisFraco.topico} rotulo="precisa de atenção" texto
                        detalhe={`${formatarPercentual(maisFraco.percentual)} de acerto`} />
        )}
      </div>

      <div className="painel-graficos">
        <section>
          <h3>Acerto por tópico</h3>
          <GraficoBarras itens={barras} descricao="Percentual de acerto por tópico, do mais fraco para o mais forte" />
        </section>
        <section>
          <h3>Evolução</h3>
          <GraficoEvolucao pontos={desempenho.evolucao} />
        </section>
      </div>

      <h3>Por tópico e nível</h3>
      <table className="table table-hover align-middle tabela">
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
    </div>
  );
}
