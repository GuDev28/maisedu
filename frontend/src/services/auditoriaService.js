import { chamarApi, query } from "./http";

/** Registros da instituição do admin, 20 por página, do mais recente para o mais antigo. */
export function listarLogs({ usuarioId, acao, de, ate, pagina = 0 }) {
  return chamarApi(`/auditoria/logs${query({ usuarioId, acao, de, ate, page: pagina })}`);
}
