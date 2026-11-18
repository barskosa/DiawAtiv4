import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  // Em `npm run dev` e `npm run preview`, as chamadas a /api são encaminhadas ao Spring Boot.
  const apiProxy = {
    '/api': {
      target: env.API_PROXY_TARGET || 'http://127.0.0.1:8080',
      changeOrigin: true,
    },
  }

  return {
    plugins: [react()],
    base: './',
    // host: true escuta em IPv4 e IPv6, então tanto http://localhost:5173 quanto
    // http://127.0.0.1:5173 funcionam (antes só 127.0.0.1 respondia).
    server: { host: true, port: 5173, proxy: apiProxy },
    preview: { host: true, port: 4173, proxy: apiProxy },
  }
})
