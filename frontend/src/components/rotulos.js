export const ANOS_ESCOLARES = [
  { valor: "SEXTO_ANO", rotulo: "6º ano" },
  { valor: "SETIMO_ANO", rotulo: "7º ano" },
  { valor: "OITAVO_ANO", rotulo: "8º ano" },
  { valor: "NONO_ANO", rotulo: "9º ano" },
];

export const NIVEIS = [
  { valor: "FACIL", rotulo: "Fácil" },
  { valor: "MEDIO", rotulo: "Médio" },
  { valor: "DIFICIL", rotulo: "Difícil" },
];

export const PAPEIS = { ADMIN: "Administrador", PROFESSOR: "Professor", ALUNO: "Aluno" };

/** Papel principal, para o cabeçalho: o admin também é professor, mas aparece como administrador. */
export function papelPrincipal(roles = []) {
  if (roles.includes("ADMIN")) {
    return PAPEIS.ADMIN;
  }
  if (roles.includes("PROFESSOR")) {
    return PAPEIS.PROFESSOR;
  }
  return PAPEIS.ALUNO;
}

export const STATUS_QUESTAO = {
  RASCUNHO: { rotulo: "Rascunho", cor: "secondary" },
  PENDENTE: { rotulo: "Em validação", cor: "warning" },
  APROVADA: { rotulo: "Aprovada", cor: "success" },
  REJEITADA: { rotulo: "Rejeitada", cor: "danger" },
};

export function rotuloStatus(valor) {
  return STATUS_QUESTAO[valor]?.rotulo ?? valor;
}

export const SITUACOES = {
  NAO_INICIADA: { rotulo: "Não iniciada", cor: "secondary" },
  EM_ANDAMENTO: { rotulo: "Em andamento", cor: "warning" },
  CONCLUIDA: { rotulo: "Concluída", cor: "success" },
};

export function rotuloSituacao(valor) {
  return SITUACOES[valor]?.rotulo ?? valor;
}

/** Ações registradas na auditoria (AcaoAuditoria no back-end). Ação nova sem rótulo aparece com o código. */
export const ACOES_AUDITORIA = {
  LOGIN: "Login",
  LOGOUT: "Logout",
  TROCA_SENHA: "Troca de senha",
  ACEITE_TERMOS: "Aceite dos termos",
  CADASTRO_PROFESSOR: "Cadastro de professor",
  CADASTRO_ALUNO: "Cadastro de aluno",
  VINCULO_ALUNO_TURMA: "Aluno vinculado à turma",
  VINCULO_PROFESSOR_TURMA: "Professor vinculado à turma",
  DESVINCULO_ALUNO_TURMA: "Aluno removido da turma",
  DESVINCULO_PROFESSOR_TURMA: "Professor removido da turma",
  AVALIACAO_QUESTAO: "Voto em questão",
  ENVIO_EMAIL: "Envio de e-mail",
  CRIACAO_QUESTAO: "Criação de questão",
  ENVIO_VALIDACAO_QUESTAO: "Questão enviada para validação",
  EDICAO_QUESTAO: "Edição de questão",
  EXCLUSAO_QUESTAO: "Exclusão de rascunho",
  CADASTRO_TURMA: "Cadastro de turma",
  ALTERACAO_TURMA: "Turma ativada/desativada",
  REDEFINICAO_SENHA: "Senha redefinida",
  CORRECAO_DADOS_USUARIO: "Correção de dados",
  DESATIVACAO_USUARIO: "Conta desativada",
  REATIVACAO_USUARIO: "Conta reativada",
  EXPORTACAO_DADOS_USUARIO: "Exportação de dados (LGPD)",
  ANONIMIZACAO_USUARIO: "Anonimização (LGPD)",
  CRIACAO_ATIVIDADE: "Criação de atividade",
  INICIO_ATIVIDADE: "Atividade iniciada",
  GERACAO_PRATICA_ADAPTATIVA: "Prática adaptativa gerada",
  LIMPEZA_AUDITORIA: "Limpeza da auditoria",
};

export function rotuloAcao(valor) {
  return ACOES_AUDITORIA[valor] ?? valor;
}

export function rotuloAno(valor) {
  return ANOS_ESCOLARES.find((a) => a.valor === valor)?.rotulo ?? (valor || "—");
}

export function rotuloNivel(valor) {
  return NIVEIS.find((n) => n.valor === valor)?.rotulo ?? valor;
}

export function formatarPercentual(valor) {
  if (valor === null || valor === undefined) {
    return "—";
  }
  return `${Number(valor).toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%`;
}

export function formatarDataHora(valor) {
  if (!valor) {
    return "—";
  }
  return new Date(valor).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

export function formatarData(valor) {
  if (!valor) {
    return "—";
  }
  const [ano, mes, dia] = String(valor).slice(0, 10).split("-");
  return `${dia}/${mes}/${ano}`;
}
