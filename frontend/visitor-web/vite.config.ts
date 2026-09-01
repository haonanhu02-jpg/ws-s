import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  base: '/visitor/',
  plugins: [vue()],
  server: {
    proxy: {
      '/api/visitor': 'http://localhost:8080',
    },
  },
})

