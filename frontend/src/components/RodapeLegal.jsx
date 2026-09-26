export const linkStyle = {
  background: "none",
  border: "none",
  color: "#0645AD",
  textDecoration: "underline",
  cursor: "pointer",
  padding: 0,
  font: "inherit",
};

export function RodapeLegal({ onAbrirPaginaLegal }) {
  return (
    <footer style={{ textAlign: "center", marginTop: "2rem", fontSize: "0.85rem", color: "#777" }}>
      <button type="button" style={linkStyle} onClick={() => onAbrirPaginaLegal("termos")}>
        Termos de Uso
      </button>
      {" · "}
      <button type="button" style={linkStyle} onClick={() => onAbrirPaginaLegal("privacidade")}>
        Política de Privacidade
      </button>
    </footer>
  );
}
