import { defineConfig } from '@playwright/test'

if (!/^supermarket_inventory_[a-z0-9_]*test$/.test(process.env.E2E_TEST_DATABASE ?? '')) {
  throw new Error('Set E2E_TEST_DATABASE to the isolated supermarket_inventory_*test database used by the backend before running UI writes.')
}

export default defineConfig({
  testDir: '../tests/e2e',
  fullyParallel: false,
  workers: 1,
  expect: { timeout: 15000 },
  reporter: 'list',
  outputDir: '../test-results',
  use: {
    actionTimeout: 15000,
    channel: process.env.E2E_BROWSER_CHANNEL ?? 'chrome',
    baseURL: process.env.E2E_BASE_URL ?? 'http://127.0.0.1:5173',
    viewport: { width: 1366, height: 900 },
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    launchOptions: { slowMo: Number(process.env.E2E_SLOW_MO_MS ?? 0) },
  },
})
