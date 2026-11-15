/**
 * Menu lateral por perfil, no mesmo formato do antigo GRUPOS, agora com rotas. O admin também tem
 * o papel PROFESSOR: as seções de professor aparecem para ele com o título "Ensino", e itens com
 * {@code somenteSemRole: "ADMIN"} ficam de fora (a tela Turmas dele já cobre todas as turmas).
 * O servidor confere o papel em toda requisição; o menu só esconde o que o perfil não usa.
 */
export const MENU = [
  { secao: null, itens: [{ rota: "/", label: "Início", icone: "inicio", roles: ["ADMIN", "PROFESSOR", "ALUNO"] }] },
  {
    secao: "Gestão",
    itens: [
      { rota: "/turmas", label: "Turmas", icone: "turmas", roles: ["ADMIN"] },
      { rota: "/usuarios", label: "Usuários", icone: "usuarios", roles: ["ADMIN"] },
      { rota: "/auditoria", label: "Auditoria", icone: "auditoria", roles: ["ADMIN"] },
    ],
  },
  {
    secao: "Minhas turmas",
    secaoAdmin: "Ensino",
    itens: [
      { rota: "/turmas", label: "Turmas e alunos", icone: "turmas", roles: ["PROFESSOR"], somenteSemRole: "ADMIN" },
      { rota: "/atividades", label: "Atividades", icone: "atividades", roles: ["PROFESSOR"] },
      { rota: "/desempenho", label: "Desempenho", icone: "desempenho", roles: ["PROFESSOR"] },
    ],
  },
  {
    secao: "Questões",
    secaoAdmin: "Ensino",
    itens: [
      { rota: "/questoes/banco", label: "Banco de questões", icone: "banco", roles: ["PROFESSOR"] },
      { rota: "/questoes/minhas", label: "Minhas questões", icone: "questoes", roles: ["PROFESSOR"] },
      { rota: "/questoes/avaliar", label: "Avaliar questões", icone: "avaliar", roles: ["PROFESSOR"], contador: "fila" },
    ],
  },
  {
    secao: "Estudos",
    itens: [
      { rota: "/minhas-atividades", label: "Minhas atividades", icone: "atividades", roles: ["ALUNO"] },
      { rota: "/praticar", label: "Praticar agora", icone: "praticar", roles: ["ALUNO"] },
      { rota: "/meu-desempenho", label: "Meu desempenho", icone: "desempenho", roles: ["ALUNO"] },
    ],
  },
];

/** Rodapé fixo do menu, igual para todos os perfis (Sair é um botão à parte). */
export const MENU_RODAPE = [
  { rota: "/conta", label: "Minha conta", icone: "conta" },
  { rota: "/como-funciona", label: "Como funciona", icone: "ajuda" },
  { rota: "/termos", label: "Termos de Uso", icone: "documento" },
  { rota: "/privacidade", label: "Privacidade", icone: "documento" },
];

/** Seções do menu para os papéis informados, juntando as que têm o mesmo título (ex.: "Ensino" do admin). */
export function secoesDoMenu(roles) {
  const ehAdmin = roles.includes("ADMIN");
  const secoes = [];
  for (const secao of MENU) {
    const itens = secao.itens.filter(
      (item) => item.roles.some((role) => roles.includes(role)) && !(item.somenteSemRole && roles.includes(item.somenteSemRole))
    );
    if (itens.length === 0) {
      continue;
    }
    const titulo = ehAdmin && secao.secaoAdmin ? secao.secaoAdmin : secao.secao;
    const anterior = secoes[secoes.length - 1];
    if (anterior && anterior.titulo === titulo && titulo !== null) {
      anterior.itens.push(...itens);
    } else {
      secoes.push({ titulo, itens: [...itens] });
    }
  }
  return secoes;
}

export function podeAcessar(roles, ...necessarias) {
  return necessarias.some((role) => roles.includes(role));
}
