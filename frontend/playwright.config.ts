import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright configuration for the campus venue booking SPA.
 *
 * The e2e run builds the demo bundle (VITE_DEMO=1), which swaps the axios
 * adapter for an in-memory implementation of the same API — same endpoints,
 * same envelopes, same status codes — so the whole booking and approval flow
 * can be exercised without the Spring Boot backend.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:4173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'npm run build:demo && npm run preview -- --port 4173 --strictPort',
    url: 'http://localhost:4173',
    reuseExistingServer: true,
    timeout: 180_000,
  },
});
