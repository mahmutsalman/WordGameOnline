/**
 * Multi-Player Test Environment Setup Script
 *
 * Opens 4 browser windows with mahmut1-mahmut4 all connected to the same room.
 * Keeps windows open for manual testing until Ctrl+C is pressed.
 *
 * Prerequisites:
 * - Backend must be running (./gradlew bootRun)
 * - Frontend must be running (npm run dev)
 *
 * Usage: npm run test:setup-room
 */
import { chromium } from '@playwright/test';
import { createRoom, joinRoom } from './helpers/multiplayer';

const BASE_URL = 'http://localhost:5173';

async function setupTestRoom() {
  console.log('🚀 Starting multi-player test environment setup...\n');

  // Launch browser in headed mode (visible windows)
  const browser = await chromium.launch({
    headless: false,
    slowMo: 50, // Slight delay for visibility
  });

  // Create 4 separate browser contexts (each acts as a separate session)
  console.log('📱 Creating 4 browser contexts...');
  const contexts = await Promise.all([
    browser.newContext({ baseURL: BASE_URL }),
    browser.newContext({ baseURL: BASE_URL }),
    browser.newContext({ baseURL: BASE_URL }),
    browser.newContext({ baseURL: BASE_URL }),
  ]);

  // Create pages for each context
  const pages = await Promise.all(contexts.map((ctx) => ctx.newPage()));

  // Set viewport sizes
  for (const page of pages) {
    await page.setViewportSize({ width: 750, height: 500 });
  }

  // Handle graceful shutdown - register early
  let isShuttingDown = false;
  process.on('SIGINT', async () => {
    if (isShuttingDown) return;
    isShuttingDown = true;
    console.log('\n\n🛑 Shutting down...');
    await browser.close();
    console.log('✅ All windows closed. Goodbye!');
    process.exit(0);
  });

  try {
    // Player 1 (mahmut1) creates the room
    console.log('\n👤 mahmut1: Creating room...');
    const roomId = await createRoom(pages[0], 'mahmut1');
    console.log(`✅ Room created: ${roomId}`);

    // Players 2-4 join the room sequentially to avoid race conditions
    console.log('\n👤 mahmut2: Joining room...');
    await joinRoom(pages[1], roomId, 'mahmut2');
    console.log('✅ mahmut2 joined');

    console.log('\n👤 mahmut3: Joining room...');
    await joinRoom(pages[2], roomId, 'mahmut3');
    console.log('✅ mahmut3 joined');

    console.log('\n👤 mahmut4: Joining room...');
    await joinRoom(pages[3], roomId, 'mahmut4');
    console.log('✅ mahmut4 joined');

    // Give WebSocket a moment to sync all players
    console.log('\n⏳ Waiting for WebSocket sync...');
    await new Promise((resolve) => setTimeout(resolve, 2000));

    console.log('\n' + '='.repeat(50));
    console.log('🎉 TEST ENVIRONMENT READY!');
    console.log('='.repeat(50));
    console.log(`\n📋 Room ID: ${roomId}`);
    console.log('\n👥 Connected Players:');
    console.log('   • mahmut1 (Admin) - Window 1');
    console.log('   • mahmut2 - Window 2');
    console.log('   • mahmut3 - Window 3');
    console.log('   • mahmut4 - Window 4');
    console.log('\n💡 You can now:');
    console.log('   • Select teams and roles for each player');
    console.log('   • Start the game (when requirements met)');
    console.log('   • Test any multiplayer scenarios');
    console.log('\n⛔ Press Ctrl+C to close all windows and exit.\n');

    // Keep the script running indefinitely
    await new Promise(() => {
      // This promise never resolves - keeps script alive until Ctrl+C
    });
  } catch (error) {
    console.error('\n❌ Setup failed:', error);
    if (!isShuttingDown) {
      await browser.close();
    }
    process.exit(1);
  }
}

// Run the setup
setupTestRoom();
