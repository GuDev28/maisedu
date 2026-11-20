import { useState } from "react";
import { corrigirEmailResponsavel, redefinirSenha } from "../services/gestaoUsuarioService";
import { Mensagens } from "./Mensagens";
import { Modal } from "./Modal";
import { resultadoDoEnvio } from "./rotulos";

/**
 * Corrige (ou cadastra, para aluno antigo) o e-mail do responsável. Se o e-mail estava errado, o
 * acesso nunca chegou: por isso já oferece mandar uma senha nova para o endereço corrigido.
 */
export function EmailDoResponsavel({ aluno, aoSalvar, aoFechar }) {
  const [email, setEmail] = useState(aluno.emailResponsavel ?? "");
  const [enviarSenha, setEnviarSenha] = useState(Boolean(aluno.senhaTemporaria) || !aluno.emailResponsavel);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState(null);

  async function salvar(evento) {
    evento.preventDefault();
    setErro(null);
    setSalvando(true);
    try {
      await corrigirEmailResponsavel(aluno.id, email);
      if (enviarSenha) {
        const resultado = await redefinirSenha(aluno.id);
        const envio = resultadoDoEnvio(resultado.statusEmailAcesso, email.trim());
        await aoSalvar(
          envio.tipo === "success"
            ? `E-mail do responsável salvo. Login e senha nova de ${aluno.nome} enviados para ${email.trim()}.`
            : `E-mail do responsável salvo, mas a senha nova não chegou. ${envio.texto}`,
          envio.tipo
        );
      } else {
        await aoSalvar(`E-mail do responsável por ${aluno.nome} salvo.`);
      }
    } catch (err) {
      setErro(err.message);
      setSalvando(false);
    }
  }

  return (
    <Modal titulo={`E-mail do responsável por ${aluno.nome}`} aoFechar={aoFechar}>
      <form onSubmit={salvar}>
        <Mensagens erro={erro} />
        <div className="mb-3">
          <label htmlFor="email-responsavel" className="form-label">
            E-mail do responsável
          </label>
          <input
            id="email-responsavel"
            type="email"
            className="form-control"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            maxLength={255}
            placeholder="responsavel@exemplo.com"
            autoFocus
            required
          />
          <div className="form-text">Usado só para enviar o login e as senhas provisórias do aluno.</div>
        </div>
        <div className="form-check">
          <input
            id="enviar-senha-responsavel"
            type="checkbox"
            className="form-check-input"
            checked={enviarSenha}
            onChange={(e) => setEnviarSenha(e.target.checked)}
          />
          <label htmlFor="enviar-senha-responsavel" className="form-check-label">
            Enviar uma senha nova para este e-mail agora
          </label>
          <div className="form-text">
            Marque se o acesso não chegou. A senha atual do aluno deixa de valer.
          </div>
        </div>
        <div className="d-flex justify-content-end gap-2 mt-4">
          <button type="button" className="btn btn-outline-secondary" onClick={aoFechar}>
            Cancelar
          </button>
          <button type="submit" className="btn btn-primary" disabled={salvando || !email.trim()}>
            {salvando ? "Salvando..." : "Salvar"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
