import { Link } from "react-router-dom";

export default function NaoEncontrada() {
  return (
    <div className="text-center py-5">
      <h1 className="me-titulo">Página não encontrada</h1>
      <p className="text-secondary">O endereço pode ter mudado ou a página não existe para o seu perfil.</p>
      <Link to="/" className="btn btn-primary">
        Voltar para o Início
      </Link>
    </div>
  );
}
