import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright configuration optimized for multiplayer WebSocket testing.
 *
 * Prerequisites before running tests:
 * 1. Start backend: cd backend && mvn spring-boot:run (port 8080)
 * 2. Start frontend: cd frontend && npm run dev (port 5173)
 * 3. Run tests: npm run test:e2e
 *
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: './e2e',

  // Test timeout - 30 seconds for multiplayer scenarios
  timeout: 30000,

  // Expect timeout for assertions
  expect: {
    timeout: 10000,
  },

  // Run tests sequentially for WebSocket stability
  fullyParallel: false,

  // Retry on first failure for flaky network conditions
  retries: 1,

  // Single worker to avoid WebSocket conflicts
  workers: 1,

  // Reporter configuration
  reporter: [
    ['html', { open: 'never' }],
    ['list'],
  ],

  // Shared settings for all tests
  use: {
    // Base URL for frontend
    baseURL: 'http://localhost:5173',

    // Collect trace on first retry for debugging
    trace: 'on-first-retry',

    // Record video on first retry
    video: 'on-first-retry',

    // Screenshot on failure
    screenshot: 'only-on-failure',

    // Action timeout
    actionTimeout: 10000,

    // Navigation timeout
    navigationTimeout: 15000,
  },

  // Project configuration - only Chromium for now
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
