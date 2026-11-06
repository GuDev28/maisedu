import { useEffect, useState } from "react";
import { meuDesempenho } from "../services/alunoService";
import { PainelDesempenho } from "../components/PainelDesempenho";
import { Mensagens } from "../components/Mensagens";

/** RF19 (aluno): painel com o próprio desempenho por tópico e nível e a evolução semanal (CA01). */
export default function MeuDesempenho() {
  const [desempenho, setDesempenho] = useState(null);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    meuDesempenho().then(setDesempenho).catch((err) => setErro(err.message));
  }, []);

  return (
    <div className="pagina">
      <h2>Meu painel de desempenho</h2>
      <Mensagens erro={erro} />
      {!desempenho && !erro && <p>Carregando...</p>}
      <PainelDesempenho desempenho={desempenho} />
    </div>
  );
}
