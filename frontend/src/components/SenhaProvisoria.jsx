import { Modal } from "./Modal";

/** Senha provisória recém-gerada (cadastro de aluno ou redefinição): aparece uma única vez. */
export function SenhaProvisoria({ dados, aoFechar }) {
  return (
    <Modal
      titulo="Senha provisória gerada"
      aoFechar={aoFechar}
      rodape={
        <button type="button" className="btn btn-primary" onClick={aoFechar}>
          Já anotei
        </button>
      }
    >
      <p className="mb-2">
        <strong>{dados.nome}</strong> entra com o login <code>{dados.identificadorDeAcesso}</code> e a senha:
      </p>
      <p className="fs-3 font-monospace text-center bg-light border rounded py-2 user-select-all">{dados.senhaProvisoria}</p>
      <p className="small text-secondary mb-0">
        Entregue ao aluno. No primeiro acesso ele troca por uma senha própria. Esta senha não será mostrada de novo.
      </p>
    </Modal>
  );
}
