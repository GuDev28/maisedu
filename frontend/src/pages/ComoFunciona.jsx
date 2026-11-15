import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { Cabecalho } from "../components/Cabecalho";

const PASSOS = [
  {
    quem: "Administrador",
    titulo: "Cria as turmas e cadastra os professores",
    texto: "Cada escola tem o seu administrador. Ele cadastra as turmas (6º ao 9º ano), os professores e diz quem leciona em cada turma.",
    link: { para: "/turmas", rotulo: "Turmas", roles: ["ADMIN"] },
  },
  {
    quem: "Professor",
    titulo: "Cadastra os alunos na turma",
    texto: "Na tela da turma, o professor cadastra o aluno já vinculado, informando o e-mail do responsável: o login e a senha provisória vão para esse e-mail. O aluno não tem e-mail no sistema.",
    link: { para: "/turmas", rotulo: "Turmas e alunos", roles: ["PROFESSOR"] },
  },
  {
    quem: "Professor",
    titulo: "Escreve questões",
    texto: "Cada questão tem 5 alternativas e uma correta. Enquanto é rascunho, pode ser editada; depois vai para a validação.",
    link: { para: "/questoes/nova", rotulo: "Nova questão", roles: ["PROFESSOR"] },
  },
  {
    quem: "Professores de outras escolas",
    titulo: "Votam na questão",
    texto: "Três votos iguais decidem: 3 aprovações levam a questão para o banco compartilhado; 3 rejeições a barram, com o motivo para o autor. O autor não vota.",
    link: { para: "/questoes/avaliar", rotulo: "Avaliar questões", roles: ["PROFESSOR"] },
  },
  {
    quem: "Professor",
    titulo: "Monta atividades com o banco",
    texto: "Escolhe questões aprovadas de qualquer escola, define a ordem e publica para a turma. Acompanha quem começou e quem terminou.",
    link: { para: "/atividades", rotulo: "Atividades", roles: ["PROFESSOR"] },
  },
  {
    quem: "Aluno",
    titulo: "Responde e vê a correção na hora",
    texto: "As questões ficam fixas quando ele começa, e cada questão aceita uma única resposta. O desempenho por tópico e nível é atualizado a cada resposta.",
    link: { para: "/minhas-atividades", rotulo: "Minhas atividades", roles: ["ALUNO"] },
  },
  {
    quem: "Aluno",
    titulo: "Pratica no ritmo dele",
    texto: "A prática adaptativa começa pelos tópicos com menor acerto, no nível que ele ainda não domina. Quem ainda não respondeu nada faz uma prática diagnóstica.",
    link: { para: "/praticar", rotulo: "Praticar agora", roles: ["ALUNO"] },
  },
];

/** Página estática com o fluxo do MaisEdu por perfil (também serve de roteiro na apresentação). */
export default function ComoFunciona() {
  const { usuario } = useAuth();
  return (
    <div className="me-largura-md">
      <Cabecalho titulo="Como funciona">
        Do cadastro da turma à prática adaptativa: quem faz cada passo no MaisEdu.
      </Cabecalho>
      <ol className="list-unstyled">
        {PASSOS.map((passo, indice) => {
          const podeAbrir = passo.link.roles.some((role) => usuario.roles.includes(role));
          return (
            <li key={passo.titulo} className="d-flex gap-3 mb-4">
              <span className="me-passo" aria-hidden="true">
                {indice + 1}
              </span>
              <div>
                <div className="small text-uppercase fw-semibold text-secondary">{passo.quem}</div>
                <h2 className="h6 mb-1">{passo.titulo}</h2>
                <p className="mb-1 text-secondary">{passo.texto}</p>
                {podeAbrir && (
                  <Link to={passo.link.para} className="small">
                    Abrir {passo.link.rotulo} →
                  </Link>
                )}
              </div>
            </li>
          );
        })}
      </ol>
      <div className="card bg-light border-0">
        <div className="card-body small text-secondary">
          Segurança e LGPD: cada ação fica registrada na auditoria da escola; o aluno tem só login (sem e-mail); qualquer
          pessoa baixa os próprios dados em Minha conta, e o administrador atende correção, bloqueio e anonimização.
        </div>
      </div>
    </div>
  );
}
