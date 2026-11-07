import { Link } from "react-router";

/** Endereço inexistente dentro da área logada. */
export default function PaginaNaoEncontrada() {
  return (
    <div className="pagina">
      <h2>Página não encontrada</h2>
      <p>O endereço acessado não existe no MaisEdu.</p>
      <Link className="btn btn-primary" to="/">Ir para a tela inicial</Link>
    </div>
  );
}
