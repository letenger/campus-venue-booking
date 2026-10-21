import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// The Spring Boot backend serves the REST API under /api and runs on :8080.
// The dev server proxies /api there so the SPA can call it same-origin in dev.
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
})
