import { test, expect } from '@playwright/test';
import {
  createRoom,
  joinRoom,
  selectTeamRole,
  waitForAllPlayersConnected,
  waitForPlayer,
  startGame,
  verifyGameBoard,
  waitForCanStart,
  createPlayerContexts,
  closeAllContexts,
} from './helpers/multiplayer';

/**
 * Multiplayer E2E tests for Codenames game.
 *
 * These tests simulate real multiplayer scenarios with 4 browser contexts
 * representing 4 different players connecting via WebSocket.
 *
 * Prerequisites:
 * 1. Backend running on port 8080: cd backend && mvn spring-boot:run
 * 2. Frontend running on port 5173: cd frontend && npm run dev
 */
test.describe('Multiplayer Room Tests', () => {
  test.describe('Room Creation and Joining', () => {
    test('should create a room and display room code', async ({ page }) => {
      const roomId = await createRoom(page, 'TestAdmin');

      // Verify room code is displayed
      expect(roomId).toBeTruthy();
      expect(roomId).toMatch(/^[A-Z0-9-]+$/);

      // Verify we're on the room page
      await expect(page.getByText('Room Lobby')).toBeVisible();
      await expect(page.getByText(roomId)).toBeVisible();
    });

    test('should join an existing room', async ({ browser }) => {
      // Create two browser contexts
      const [adminPage, playerPage] = await createPlayerContexts(browser, 2);

      try {
        // Admin creates room
        const roomId = await createRoom(adminPage, 'Admin');

        // Small delay to ensure room is fully created
        await adminPage.waitForTimeout(500);

        // Player joins room
        await joinRoom(playerPage, roomId, 'Player2');

        // Verify WebSocket sync by checking player count on both pages
        // This is the most reliable indicator that both players see each other
        await waitForAllPlayersConnected(adminPage, 2);
        await waitForAllPlayersConnected(playerPage, 2);

        // Both players should see the room lobby
        await expect(adminPage.getByText('Room Lobby')).toBeVisible();
        await expect(playerPage.getByText('Room Lobby')).toBeVisible();
      } finally {
        await closeAllContexts([adminPage, playerPage]);
      }
    });
  });

  test.describe('Team Selection', () => {
    test('should allow players to select teams and roles', async ({ browser }) => {
      const [adminPage, playerPage] = await createPlayerContexts(browser, 2);

      try {
        // Setup room
        const roomId = await createRoom(adminPage, 'Admin');
        await adminPage.waitForTimeout(500);

        await joinRoom(playerPage, roomId, 'Player2');

        // Wait for both players to be connected
        await waitForAllPlayersConnected(adminPage, 2);
        await waitForAllPlayersConnected(playerPage, 2);

        // Admin selects Blue Spymaster
        await selectTeamRole(adminPage, 'BLUE', 'SPYMASTER');

        // Player selects Red Operative
        await selectTeamRole(playerPage, 'RED', 'OPERATIVE');

        // Verify team assignments are visible in the "Current Team" display
        await expect(adminPage.locator('.bg-gray-50').getByText('BLUE')).toBeVisible();
        await expect(playerPage.locator('.bg-gray-50').getByText('RED')).toBeVisible();
      } finally {
        await closeAllContexts([adminPage, playerPage]);
      }
    });
  });

  test.describe('4-Player Game Start', () => {
    test('4 players join room, select teams, and start game', async ({ browser }) => {
      // Create 4 browser contexts (simulating 4 different players)
      const pages = await createPlayerContexts(browser, 4);
      const [admin, player2, player3, player4] = pages;

      try {
        // Step 1: Admin creates room
        const roomId = await createRoom(admin, 'Admin');
        console.log(`Room created: ${roomId}`);
        await admin.waitForTimeout(500);

        // Step 2: Other 3 players join SEQUENTIALLY for better WebSocket sync
        await joinRoom(player2, roomId, 'Player2');
        await joinRoom(player3, roomId, 'Player3');
        await joinRoom(player4, roomId, 'Player4');

        // Step 3: Wait for all 4 players to be connected on admin's view
        await waitForAllPlayersConnected(admin, 4);
        console.log('All 4 players connected');

        // Step 4: Assign teams and roles SEQUENTIALLY
        // Blue Team: Admin (Spymaster), Player2 (Operative)
        // Red Team: Player3 (Spymaster), Player4 (Operative)
        await selectTeamRole(admin, 'BLUE', 'SPYMASTER');
        await selectTeamRole(player2, 'BLUE', 'OPERATIVE');
        await selectTeamRole(player3, 'RED', 'SPYMASTER');
        await selectTeamRole(player4, 'RED', 'OPERATIVE');

        console.log('Teams assigned');

        // Step 5: Wait for canStart to become true
        await waitForCanStart(admin);
        console.log('Game can start');

        // Step 6: Admin starts the game
        await startGame(admin);
        console.log('Game started');

        // Step 7: Verify admin sees the game board
        await verifyGameBoard(admin);

        console.log('Admin sees game board - test passed!');

        // NOTE: Other players would need WebSocket game state broadcasting
        // to see the game board. Currently game state is only in admin's store.
        // This is a known limitation to be addressed in future implementation.
      } finally {
        await closeAllContexts(pages);
      }
    });

    test('start button should be disabled without full teams', async ({ browser }) => {
      const [admin, player2] = await createPlayerContexts(browser, 2);

      try {
        // Create room and join with 2 players
        const roomId = await createRoom(admin, 'Admin');
        await admin.waitForTimeout(500);

        await joinRoom(player2, roomId, 'Player2');

        // Wait for both players to be connected
        await waitForAllPlayersConnected(admin, 2);

        // Select teams but not enough players (need 4 with operatives)
        await selectTeamRole(admin, 'BLUE', 'SPYMASTER');
        await selectTeamRole(player2, 'RED', 'SPYMASTER');

        // Button shows "Waiting for Teams..." when disabled, "Start Game" when enabled
        // Look for either text pattern
        const startButton = admin.getByRole('button', { name: /waiting for teams|start game/i });
        await expect(startButton).toBeVisible();
        await expect(startButton).toBeDisabled();

        // Verify helper text
        await expect(
          admin.getByText(/Need at least 4 players with 1 Spymaster per team/i)
        ).toBeVisible();
      } finally {
        await closeAllContexts([admin, player2]);
      }
    });
  });

  test.describe('Game Board Display', () => {
    test('spymaster sees all card colors with semi-transparent styling', async ({
      browser,
    }) => {
      const pages = await createPlayerContexts(browser, 4);
      const [admin, player2, player3, player4] = pages;

      try {
        // Setup and start game
        const roomId = await createRoom(admin, 'Admin');
        await admin.waitForTimeout(500);

        // Join sequentially for better WebSocket sync
        await joinRoom(player2, roomId, 'Player2');
        await joinRoom(player3, roomId, 'Player3');
        await joinRoom(player4, roomId, 'Player4');

        await waitForAllPlayersConnected(admin, 4);

        // Assign roles sequentially
        await selectTeamRole(admin, 'BLUE', 'SPYMASTER'); // Admin is spymaster
        await selectTeamRole(player2, 'BLUE', 'OPERATIVE');
        await selectTeamRole(player3, 'RED', 'SPYMASTER');
        await selectTeamRole(player4, 'RED', 'OPERATIVE');

        await waitForCanStart(admin);
        await startGame(admin);
        await verifyGameBoard(admin);

        // Spymaster should see card colors (opacity-60 for unrevealed)
        const spymasterCards = admin.locator('[data-testid^="card-"]');
        const firstCard = spymasterCards.first();

        // Spymaster's card should have opacity-60 class (semi-transparent unrevealed)
        const spymasterCardClass = await firstCard.getAttribute('class');
        expect(spymasterCardClass).toContain('opacity-60');

        // Verify game info bar shows correct role
        await expect(admin.getByText('SPYMASTER')).toBeVisible();

        // NOTE: Operative view testing requires WebSocket game state broadcasting
        // Currently game state is only available in admin's local store
      } finally {
        await closeAllContexts(pages);
      }
    });
  });

  test.describe('Connection Status', () => {
    test('should show connection status indicator', async ({ page }) => {
      await createRoom(page, 'TestUser');

      // Should show connected status
      await expect(page.getByText('Connected')).toBeVisible();

      // Green indicator should be visible
      const statusIndicator = page.locator('.bg-green-500');
      await expect(statusIndicator).toBeVisible();
    });
  });
});

test.describe('Edge Cases', () => {
  test('should handle rapid team changes', async ({ browser }) => {
    const [page] = await createPlayerContexts(browser, 1);

    try {
      await createRoom(page, 'RapidChanger');

      // Change teams multiple times
      await selectTeamRole(page, 'BLUE', 'SPYMASTER');
      await selectTeamRole(page, 'RED', 'OPERATIVE');
      await selectTeamRole(page, 'BLUE', 'OPERATIVE');
      await selectTeamRole(page, 'RED', 'SPYMASTER');

      // Final state should be Red Spymaster (verify in Current Team display)
      await expect(page.locator('.bg-gray-50').getByText('RED')).toBeVisible();
      await expect(page.locator('.bg-gray-50').getByText('SPYMASTER')).toBeVisible();
    } finally {
      await closeAllContexts([page]);
    }
  });

  test('should display room code in URL and page', async ({ page }) => {
    const roomId = await createRoom(page, 'Admin');

    // URL should contain room ID
    expect(page.url()).toContain(roomId);

    // Page should display room code
    await expect(page.getByText(roomId)).toBeVisible();
  });
});
