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
    server: { proxy: apiProxy },
    preview: { proxy: apiProxy },
  }
})
