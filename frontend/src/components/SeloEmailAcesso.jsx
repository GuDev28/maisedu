import { STATUS_EMAIL_ACESSO, formatarDataHora } from "./rotulos";

/**
 * Selo para quando o e-mail com login e senha provisória não chegou (falhou ou o envio está
 * desativado no ambiente). Quando foi enviado, não mostra nada: é o caso normal.
 */
export function SeloEmailAcesso({ status, quando }) {
  if (!status || status === "ENVIADO") {
    return null;
  }
  const info = STATUS_EMAIL_ACESSO[status];
  const titulo = quando ? `${info?.rotulo ?? status} (${formatarDataHora(quando)})` : info?.rotulo;
  return (
    <span className={`badge text-bg-${info?.cor ?? "secondary"}`} title={titulo}>
      {status === "FALHOU" ? "e-mail de acesso não entregue" : "e-mail de acesso não enviado"}
    </span>
  );
}
