import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
// Em desenvolvimento, o front chama /api/... e o Vite repassa para o Spring Boot (porta 8080).
// Assim front e API ficam na mesma origem: o cookie de sessão (SameSite=Strict) funciona e não é
// preciso CORS. Em produção, o nginx do docker compose faz o mesmo papel.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: process.env.VITE_API_PROXY ?? 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (caminho) => caminho.replace(/^\/api/, ''),
      },
    },
  },
})
