import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { meuDesempenho } from "../services/alunoService";
import { Cabecalho } from "../components/Cabecalho";
import { Carregando } from "../components/Carregando";
import { PainelDesempenho } from "../components/PainelDesempenho";
import { Mensagens } from "../components/Mensagens";

/** RF19 (aluno): o próprio desempenho por tópico e nível, e a evolução semanal. */
export default function MeuDesempenho() {
  const [desempenho, setDesempenho] = useState(null);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    meuDesempenho().then(setDesempenho).catch((err) => setErro(err.message));
  }, []);

  return (
    <>
      <Cabecalho
        titulo="Meu desempenho"
        acoes={
          <Link to="/praticar" className="btn btn-primary">
            Praticar agora
          </Link>
        }
      >
        Acerto por tópico e nível. É com esses números que o MaisEdu escolhe as questões da sua prática.
      </Cabecalho>
      <Mensagens erro={erro} />
      {!desempenho && !erro && <Carregando />}
      <PainelDesempenho desempenho={desempenho} />
    </>
  );
}
