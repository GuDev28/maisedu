package br.com.maisedu.app.model;

/**
 * Situação do último e-mail com dados de acesso (senha provisória) enviado ao usuário ou, no caso do
 * aluno, ao responsável. Mostrada ao professor e ao admin para que um envio que falhou não passe
 * despercebido (o aluno ficaria sem acesso).
 */
public enum StatusEmailAcesso {
    /** O Resend aceitou o e-mail. */
    ENVIADO,
    /** O Resend recusou ou não respondeu (domínio não verificado, endereço inválido, rede...). */
    FALHOU,
    /** Ambiente sem RESEND_API_KEY: nada foi enviado (no perfil dev, o conteúdo vai para o log). */
    NAO_CONFIGURADO
}
