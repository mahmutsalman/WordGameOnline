import { Page, expect } from '@playwright/test';

export type Team = 'BLUE' | 'RED';
export type Role = 'SPYMASTER' | 'OPERATIVE';

/**
 * Creates a new room and returns the room ID.
 * The creating user becomes the admin.
 *
 * @param page - Playwright page instance
 * @param username - Username for the room creator
 * @returns The created room ID
 */
export async function createRoom(page: Page, username: string): Promise<string> {
  // Navigate to home page
  await page.goto('/');

  // Fill in username for creating room
  await page.getByLabel('Your Username').first().fill(username);

  // Click create room button
  await page.getByRole('button', { name: /create room/i }).click();

  // Wait for navigation to room page and extract room ID from URL
  await page.waitForURL(/\/room\/[A-Z0-9-]+$/);

  const url = page.url();
  const roomId = url.split('/room/')[1];

  // Wait for WebSocket connection to be established
  await expect(page.getByText('Connected')).toBeVisible({ timeout: 10000 });

  return roomId;
}

/**
 * Joins an existing room with the given room ID.
 *
 * @param page - Playwright page instance
 * @param roomId - The room ID to join
 * @param username - Username for the joining player
 */
export async function joinRoom(
  page: Page,
  roomId: string,
  username: string
): Promise<void> {
  // Navigate to home page
  await page.goto('/');

  // Fill in username for joining (second form)
  const joinUsernameInput = page.locator('#join-username');
  await joinUsernameInput.fill(username);

  // Fill in room code
  await page.getByLabel('Room Code').fill(roomId);

  // Click join room button
  await page.getByRole('button', { name: /join room/i }).click();

  // Wait for navigation to room page
  await page.waitForURL(`/room/${roomId}`);

  // Wait for WebSocket connection to be established
  await expect(page.getByText('Connected')).toBeVisible({ timeout: 10000 });
}

/**
 * Selects a team and role for the current player.
 * Uses direct button targeting based on background color classes.
 *
 * @param page - Playwright page instance
 * @param team - Team to join ('BLUE' or 'RED')
 * @param role - Role to select ('SPYMASTER' or 'OPERATIVE')
 */
export async function selectTeamRole(
  page: Page,
  team: Team,
  role: Role
): Promise<void> {
  // Map team/role to specific button color classes from RoomPage
  // Blue Operative: bg-blue-500, Blue Spymaster: bg-blue-700
  // Red Operative: bg-red-500, Red Spymaster: bg-red-700
  let buttonClass: string;
  if (team === 'BLUE' && role === 'OPERATIVE') {
    buttonClass = 'bg-blue-500';
  } else if (team === 'BLUE' && role === 'SPYMASTER') {
    buttonClass = 'bg-blue-700';
  } else if (team === 'RED' && role === 'OPERATIVE') {
    buttonClass = 'bg-red-500';
  } else {
    buttonClass = 'bg-red-700';
  }

  // Click the button with the specific class
  await page.locator(`button.${buttonClass}`).click();

  // Wait for the change to be reflected (Current Team should update)
  const expectedTeam = team === 'BLUE' ? 'BLUE' : 'RED';
  await expect(page.locator('.bg-gray-50').getByText(expectedTeam)).toBeVisible({
    timeout: 5000,
  });

  // Small delay for WebSocket propagation
  await page.waitForTimeout(500);
}

/**
 * Waits for all players to be connected to the room.
 * Uses polling to wait for WebSocket updates.
 *
 * @param page - Playwright page instance
 * @param count - Expected number of connected players
 */
export async function waitForAllPlayersConnected(
  page: Page,
  count: number
): Promise<void> {
  // Wait for the players count to match (with regex for flexibility)
  await expect(page.getByText(new RegExp(`Players \\(${count}\\)`))).toBeVisible({
    timeout: 20000,
  });

  // Also verify online count
  await expect(page.getByText(new RegExp(`${count} online`))).toBeVisible({
    timeout: 20000,
  });
}

/**
 * Waits for a specific player to appear in the player list.
 * Includes retry logic for WebSocket propagation delays.
 *
 * @param page - Playwright page instance
 * @param username - Username to wait for
 */
export async function waitForPlayer(page: Page, username: string): Promise<void> {
  // Use a more specific locator - player names appear in player cards
  // Wait with longer timeout for WebSocket sync
  await expect(
    page.locator('span.font-medium', { hasText: username })
  ).toBeVisible({ timeout: 15000 });
}

/**
 * Starts the game (admin only).
 * Waits for the Start Game button to be enabled, then clicks it.
 *
 * @param page - Playwright page instance
 */
export async function startGame(page: Page): Promise<void> {
  // Wait for canStart to be true (button becomes enabled)
  const startButton = page.getByRole('button', { name: /start game/i });
  await expect(startButton).toBeEnabled({ timeout: 15000 });

  // Click start game
  await startButton.click();

  // Wait for navigation to game page
  await page.waitForURL(/\/room\/[A-Z0-9-]+\/game$/);
}

/**
 * Verifies that the game board is visible on the page.
 *
 * @param page - Playwright page instance
 */
export async function verifyGameBoard(page: Page): Promise<void> {
  // Wait for game board to be visible
  await expect(page.locator('[data-testid="game-board"]')).toBeVisible({
    timeout: 10000,
  });

  // Verify we have 25 cards (5x5 grid)
  const cards = page.locator('[data-testid^="card-"]');
  await expect(cards).toHaveCount(25);
}

/**
 * Gets the current turn team from the game board.
 *
 * @param page - Playwright page instance
 * @returns The current team ('BLUE' or 'RED')
 */
export async function getCurrentTurn(page: Page): Promise<Team> {
  const turnIndicator = page.locator('text=Turn:').locator('xpath=following-sibling::div[1]');
  const teamText = await turnIndicator.textContent();

  if (teamText?.includes('BLUE')) return 'BLUE';
  if (teamText?.includes('RED')) return 'RED';

  throw new Error(`Could not determine current turn from: ${teamText}`);
}

/**
 * Verifies the game phase.
 *
 * @param page - Playwright page instance
 * @param phase - Expected phase ('CLUE', 'GUESS', 'GAME_OVER')
 */
export async function verifyPhase(
  page: Page,
  phase: 'CLUE' | 'GUESS' | 'GAME_OVER'
): Promise<void> {
  await expect(page.getByText(phase)).toBeVisible();
}

/**
 * Waits for the start game button to become enabled.
 * This indicates all team requirements are met.
 *
 * @param page - Playwright page instance
 */
export async function waitForCanStart(page: Page): Promise<void> {
  const startButton = page.getByRole('button', { name: /start game/i });
  await expect(startButton).toBeEnabled({ timeout: 20000 });
}

/**
 * Creates multiple browser contexts for multiplayer testing.
 *
 * @param browser - Playwright browser instance
 * @param count - Number of contexts to create
 * @returns Array of Playwright pages
 */
export async function createPlayerContexts(
  browser: import('@playwright/test').Browser,
  count: number
): Promise<Page[]> {
  const contexts = await Promise.all(
    Array.from({ length: count }, () => browser.newContext())
  );

  const pages = await Promise.all(contexts.map((ctx) => ctx.newPage()));

  return pages;
}

/**
 * Closes all player contexts.
 *
 * @param pages - Array of Playwright pages
 */
export async function closeAllContexts(pages: Page[]): Promise<void> {
  await Promise.all(pages.map((page) => page.context().close()));
}
