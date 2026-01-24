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
  color: CardColor | null;  // null for hidden cards (operative view)
  revealed: boolean;
  selectedBy?: string;      // Player ID of selector
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
  guessedWords: string[];     // Words guessed during this turn
  guessedColors: CardColor[]; // Colors revealed for each guess
}

// Player interface (matches backend Player entity)
export interface Player {
  id: string;
  username: string;
  avatar: string;
  team: Team | null;  // null for spectators
  role: Role;
  connected: boolean;
  admin: boolean;
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

// WebSocket connection status
export type ConnectionStatus =
  | 'DISCONNECTED'
  | 'CONNECTING'
  | 'CONNECTED'
  | 'RECONNECTING'
  | 'ERROR';

// Base WebSocket event interface
export interface BaseWSEvent {
  type: string;
}

// WebSocket Event Types (matching backend DTOs)

export interface PlayerJoinedEvent extends BaseWSEvent {
  type: 'PLAYER_JOINED';
  playerId: string;
  username: string;
}

export interface PlayerLeftEvent extends BaseWSEvent {
  type: 'PLAYER_LEFT';
  playerId: string;
}

export interface PlayerUpdatedEvent extends BaseWSEvent {
  type: 'PLAYER_UPDATED';
  playerId: string;
  team: Team | null;
  role: Role;
}

export interface RoomStateEvent extends BaseWSEvent {
  type: 'ROOM_STATE';
  players: Player[];
  settings: GameSettings;
  canStart: boolean;
}

export interface ErrorEvent extends BaseWSEvent {
  type: 'ERROR';
  message: string;
}

export interface GameStateEvent extends BaseWSEvent {
  type: 'GAME_STATE';
  board: Card[];
  currentTeam: Team;
  phase: GamePhase;
  currentClue: Clue | null;
  guessesRemaining: number;
  blueRemaining: number;
  redRemaining: number;
  winner: Team | null;
  history: TurnHistory[];
}

// Union type for all WebSocket events
export type WSEvent =
  | PlayerJoinedEvent
  | PlayerLeftEvent
  | PlayerUpdatedEvent
  | RoomStateEvent
  | GameStateEvent
  | ErrorEvent;

// WebSocket Request Types (matching backend request DTOs)

export interface JoinRoomWsRequest {
  username: string;
}

export interface ChangeTeamRequest {
  team?: Team | null;
  role: Role;
}

// Legacy WebSocket message interface (kept for compatibility)
export interface WebSocketMessage<T = unknown> {
  event: string;
  data: T;
}
