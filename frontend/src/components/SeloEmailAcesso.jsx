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

/**
 * Frase para avisar o resultado do envio do e-mail de acesso. {@code destino}: "o responsável",
 * "o professor"... Devolve {texto, tipo} para o aviso (tipo do Bootstrap: success, warning, danger).
 */
export function resultadoDoEnvio(status, destino) {
  if (status === "FALHOU") {
    return {
      tipo: "danger",
      texto: `O e-mail para ${destino} não foi entregue. Confira o endereço e use “Nova senha” para enviar de novo.`,
    };
  }
  if (status === "NAO_CONFIGURADO") {
    return {
      tipo: "warning",
      texto: "O envio de e-mail está desativado neste ambiente, então o e-mail não saiu (em desenvolvimento, a senha provisória fica no log do back-end).",
    };
  }
  return { tipo: "success", texto: `Login e senha provisória enviados para ${destino}.` };
}
