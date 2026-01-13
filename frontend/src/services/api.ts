/**
 * API service for Room management endpoints.
 * Handles HTTP requests to the backend REST API.
 */

const API_BASE = '/api';

// ========== TYPE DEFINITIONS ==========

export interface CreateRoomRequest {
  username: string;
}

export interface JoinRoomRequest {
  username: string;
}

export interface GameSettings {
  wordPack: string;
  timerSeconds: number | null;
}

export interface PlayerResponse {
  id: string;
  username: string;
  team: 'BLUE' | 'RED' | null;
  role: 'SPYMASTER' | 'OPERATIVE' | 'SPECTATOR';
  connected: boolean;
  admin: boolean;
}

export interface RoomResponse {
  roomId: string;
  players: PlayerResponse[];
  settings: GameSettings;
  canStart: boolean;
  adminId: string;
}

// ========== API FUNCTIONS ==========

/**
 * Creates a new game room with the specified admin username.
 * POST /api/rooms
 *
 * @param request - The room creation request with username
 * @returns Promise<RoomResponse> - The created room details
 * @throws Error if request fails or validation errors occur
 */
export async function createRoom(
  request: CreateRoomRequest
): Promise<RoomResponse> {
  const response = await fetch(`${API_BASE}/rooms`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(JSON.stringify(error));
  }

  return response.json();
}

/**
 * Retrieves room details by room ID.
 * GET /api/rooms/{roomId}
 *
 * @param roomId - The room ID to retrieve
 * @returns Promise<RoomResponse> - The room details
 * @throws Error if room not found or request fails
 */
export async function getRoom(roomId: string): Promise<RoomResponse> {
  const response = await fetch(`${API_BASE}/rooms/${roomId}`, {
    method: 'GET',
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(JSON.stringify(error));
  }

  return response.json();
}

/**
 * Checks if a room exists by room ID.
 * GET /api/rooms/{roomId}/exists
 *
 * @param roomId - The room ID to check
 * @returns Promise<boolean> - True if room exists, false otherwise
 */
export async function roomExists(roomId: string): Promise<boolean> {
  const response = await fetch(`${API_BASE}/rooms/${roomId}/exists`, {
    method: 'GET',
  });

  const data = await response.json();
  return data.exists;
}

/**
 * Adds a player to an existing room.
 * POST /api/rooms/{roomId}/join
 *
 * @param roomId - The room ID to join
 * @param request - The join request with username
 * @returns Promise<RoomResponse> - The updated room details
 * @throws Error if room not found, username taken, or validation fails
 */
export async function joinRoom(
  roomId: string,
  request: JoinRoomRequest
): Promise<RoomResponse> {
  const response = await fetch(`${API_BASE}/rooms/${roomId}/join`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(JSON.stringify(error));
  }

  return response.json();
}
