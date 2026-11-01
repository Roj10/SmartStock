import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Configuração do Vite (a ferramenta que roda o frontend no computador e gera o build para o servidor).
//
// "proxy": no modo de desenvolvimento (npm run dev), o site abre em http://localhost:5173 e tudo o que for
// para /api, /arquivos ou para a documentação (Swagger) é repassado ao backend em http://localhost:8080.
// Assim o frontend usa sempre endereços relativos (/api/...) e funciona igual no computador e no servidor,
// onde quem faz esse repasse é o Nginx.
const BACKEND = process.env.SMARTSTOCK_BACKEND || 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': BACKEND,
      '/arquivos': BACKEND,
      '/swagger-ui': BACKEND,
      '/v3/api-docs': BACKEND,
      '/openapi.yaml': BACKEND,
    },
  },
})
