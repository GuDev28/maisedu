/**
 * Ícones do menu e dos botões, desenhados em SVG (traço de 1,8 px) para não trazer outra
 * dependência. Sempre decorativos: o texto ao lado é que dá o significado.
 */
const CAMINHOS = {
  inicio: "M3 11.5 12 4l9 7.5M5.5 9.5V20h13V9.5",
  turmas: "M4 19.5v-1a4 4 0 0 1 4-4h2a4 4 0 0 1 4 4v1M9 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6M16 5.5a3 3 0 0 1 0 5.5M18 14.6a4 4 0 0 1 2 3.4v1.5",
  usuarios: "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8M4.5 20a7.5 7.5 0 0 1 15 0",
  auditoria: "M9 4h6l1 2h3v14H5V6h3zM9 11h6M9 15h4",
  atividades: "M8 4h8M6 7h12v13H6zM9 11h6M9 15h4",
  desempenho: "M4 20V10M10 20V4M16 20v-7M22 20H2",
  painel: "M4 4h7v7H4zM13 4h7v4h-7zM13 10h7v10h-7zM4 13h7v7H4z",
  banco: "M4 6c0-1.7 3.6-3 8-3s8 1.3 8 3-3.6 3-8 3-8-1.3-8-3M4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3",
  questoes: "M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z",
  avaliar: "M9 12l2 2 4-4M12 3l7 3v5c0 4.5-3 8.5-7 10-4-1.5-7-5.5-7-10V6z",
  praticar: "M13 3 4 14h7l-1 7 9-11h-7z",
  conta: "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8M4 21v-1a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v1",
  ajuda: "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18M9.5 9a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.6V14M12 17.5h.01",
  documento: "M7 3h7l5 5v13H7zM14 3v5h5",
  sair: "M15 4h4v16h-4M10 8l-4 4 4 4M6 12h10",
  menu: "M4 7h16M4 12h16M4 17h16",
  fechar: "M6 6l12 12M18 6 6 18",
  mais: "M12 5v14M5 12h14",
  voltar: "M15 18l-6-6 6-6",
  baixar: "M12 4v11M7 10l5 5 5-5M5 20h14",
  lixeira: "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13",
  editar: "M4 20h4L19 9l-4-4L4 16zM13.5 6.5l4 4",
  enviar: "M4 12 20 4l-6 16-3-7z",
  alerta: "M12 4 2.5 20h19zM12 10v4M12 17h.01",
};

export function Icone({ nome, tamanho = 18, className = "" }) {
  return (
    <svg
      width={tamanho}
      height={tamanho}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      className={className}
    >
      <path d={CAMINHOS[nome] ?? CAMINHOS.documento} />
    </svg>
  );
}
