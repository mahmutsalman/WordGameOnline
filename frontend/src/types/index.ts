/**
 * Core type definitions for the Codenames game.
 * These types align with the backend models and game state.
 */

// Team types
export type Team = 'BLUE' | 'RED';

// Player roles
export type Role = 'SPYMASTER' | 'OPERATIVE' | 'SPECTATOR';

// Card colors (only visible to spymasters until revealed)
export type CardColor = 'BLUE' | 'RED' | 'NEUTRAL' | 'ASSASSIN';

// Game phases
export type GamePhase = 'LOBBY' | 'CLUE' | 'GUESS' | 'GAME_OVER';

// Card interface
export interface Card {
  word: string;
  color: CardColor;        // Only visible to spymasters until revealed
  revealed: boolean;
  selectedBy?: string;     // Player ID of selector
}

// Clue interface
export interface Clue {
  word: string;
  number: number;
  team: Team;
}

// Game state interface
export interface GameState {
  board: Card[];
  currentTeam: Team;
  currentClue: Clue | null;
  guessesRemaining: number;
  blueRemaining: number;
  redRemaining: number;
  phase: GamePhase;
  winner: Team | null;
  turnHistory: TurnHistory[];
}

// Turn history interface
export interface TurnHistory {
  team: Team;
  clue: Clue;
  guesses: string[];
}

// Player interface
export interface Player {
  id: string;
  username: string;
  avatar: string;
  team: Team | 'SPECTATOR';
  role: Role;
  isConnected: boolean;
  isAdmin: boolean;
}

// Game settings interface
export interface GameSettings {
  mode: 'classic' | 'duet';
  wordPack: string;
  timerSeconds: number | null;
  spectatorMode: 'can_see_colors' | 'operative_view';
}

// Room interface
export interface Room {
  roomId: string;
  players: Player[];
  settings: GameSettings;
  gameState: GameState | null;
}

// WebSocket message types
export interface WebSocketMessage<T = unknown> {
  event: string;
  data: T;
}
