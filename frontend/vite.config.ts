import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { Agent } from 'node:http'

// Reuse completed responses through a small pool; failed/aborted sockets are discarded by Node.
// The idle cap is below the backend's advertised 60 seconds and above the AI request timeout.
// Never retry business writes after connection errors.
const backendAgent = () => new Agent({
  keepAlive: true,
  maxSockets: 4,
  maxFreeSockets: 4,
  scheduling: 'lifo',
  timeout: 25000,
  agentKeepAliveTimeoutBuffer: 1000,
})
const backendProxy = { target: process.env.BACKEND_PROXY_TARGET ?? 'http://127.0.0.1:8080', agent: backendAgent(), proxyTimeout: 10000, timeout: 12000 }
const proxy = {
  '/api/v1/suggestions': { ...backendProxy, agent: backendAgent(), proxyTimeout: 20000, timeout: 22000 },
  '/api': backendProxy,
  '/media': backendProxy,
}

export default defineConfig({
  plugins: [vue()],
  server: { port: 5173, strictPort: true, proxy },
  preview: { port: 4173, strictPort: true, proxy },
  test: { environment: 'jsdom', include: ['src/**/*.test.ts'] },
})
