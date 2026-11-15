/** Nova senha + confirmação, usados no primeiro acesso e em Minha conta. */
export function CamposSenhaNova({ novaSenha, confirmacao, aoMudarSenha, aoMudarConfirmacao }) {
  const diferente = confirmacao.length > 0 && confirmacao !== novaSenha;
  return (
    <>
      <div className="mb-3">
        <label htmlFor="nova-senha" className="form-label">
          Nova senha
        </label>
        <input
          id="nova-senha"
          type="password"
          className="form-control"
          value={novaSenha}
          onChange={(e) => aoMudarSenha(e.target.value)}
          minLength={8}
          autoComplete="new-password"
          required
        />
        <div className="form-text">Pelo menos 8 caracteres.</div>
      </div>
      <div className="mb-3">
        <label htmlFor="confirmacao-senha" className="form-label">
          Confirme a nova senha
        </label>
        <input
          id="confirmacao-senha"
          type="password"
          className={`form-control ${diferente ? "is-invalid" : ""}`}
          value={confirmacao}
          onChange={(e) => aoMudarConfirmacao(e.target.value)}
          minLength={8}
          autoComplete="new-password"
          required
        />
        {diferente && <div className="invalid-feedback">A confirmação não é igual à nova senha.</div>}
      </div>
    </>
  );
}
