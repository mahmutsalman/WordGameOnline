import { describe, it, expect, beforeEach, vi } from 'vitest';
import {
  createRoom,
  getRoom,
  roomExists,
  joinRoom,
  startGame,
  getGameState,
  CreateRoomRequest,
  JoinRoomRequest,
  RoomResponse,
  GameStateResponse,
} from '../api';

describe('API Service', () => {
  beforeEach(() => {
    // Reset fetch mock before each test
    vi.resetAllMocks();
    global.fetch = vi.fn();
  });

  // ========== CREATE ROOM TESTS ==========

  describe('createRoom', () => {
    it('should create room with valid username', async () => {
      // Arrange
      const request: CreateRoomRequest = { username: 'TestUser' };
      const mockResponse: RoomResponse = {
        roomId: 'ABC12-DEF34',
        players: [
          {
            id: 'player-1',
            username: 'TestUser',
            team: null,
            role: 'SPECTATOR',
            connected: true,
            admin: true,
          },
        ],
        settings: {
          wordPack: 'english',
          timerSeconds: null,
        },
        canStart: false,
        adminId: 'player-1',
      };

      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: true,
        status: 201,
        json: async () => mockResponse,
      } as Response);

      // Act
      const result = await createRoom(request);

      // Assert
      expect(result).toEqual(mockResponse);
    });

    it('should send POST request to correct endpoint', async () => {
      // Arrange
      const request: CreateRoomRequest = { username: 'TestUser' };
      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: true,
        status: 201,
        json: async () => ({} as RoomResponse),
      } as Response);

      // Act
      await createRoom(request);

      // Assert
      expect(global.fetch).toHaveBeenCalledWith(
        '/api/rooms',
        expect.objectContaining({
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(request),
        })
      );
    });

    it('should include username in request body', async () => {
      // Arrange
      const request: CreateRoomRequest = { username: 'Player1' };
      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: true,
        status: 201,
        json: async () => ({} as RoomResponse),
      } as Response);

      // Act
      await createRoom(request);

      // Assert
      const fetchCall = (global.fetch as ReturnType<typeof vi.fn>).mock.calls[0];
      const body = JSON.parse(fetchCall[1].body);
      expect(body.username).toBe('Player1');
    });

    it('should return room response', async () => {
      // Arrange
      const request: CreateRoomRequest = { username: 'Admin' };
      const mockResponse: RoomResponse = {
        roomId: 'TEST-ROOM',
        players: [],
        settings: { wordPack: 'english', timerSeconds: null },
        canStart: false,
        adminId: 'admin-1',
      };

      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: true,
        status: 201,
        json: async () => mockResponse,
      } as Response);

      // Act
      const result = await createRoom(request);

      // Assert
      expect(result).toEqual(mockResponse);
    });

    it('should throw error on create failure', async () => {
      // Arrange
      const request: CreateRoomRequest = { username: '' };
      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: async () => ({ username: 'Username is required' }),
      } as Response);

      // Act & Assert
      await expect(createRoom(request)).rejects.toThrow();
    });
  });

  // ========== GET ROOM TESTS ==========

  describe('getRoom', () => {
    it('should get room by ID', async () => {
      // Arrange
      const roomId = 'TEST-ROOM';
      const mockResponse: RoomResponse = {
        roomId: 'TEST-ROOM',
        players: [
          {
            id: 'p1',
            username: 'User1',
            team: null,
            role: 'SPECTATOR',
            connected: true,
            admin: true,
          },
        ],
        settings: { wordPack: 'english', timerSeconds: null },
        canStart: false,
        adminId: 'p1',
      };

      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockResponse,
      } as Response);

      // Act
      const result = await getRoom(roomId);

      // Assert
      expect(result).toEqual(mockResponse);
    });

    it('should send GET request to correct endpoint', async () => {
      // Arrange
      const roomId = 'ABC12-DEF34';
      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({} as RoomResponse),
      } as Response);

      // Act
      await getRoom(roomId);

      // Assert
      expect(global.fetch).toHaveBeenCalledWith(
        `/api/rooms/${roomId}`,
        expect.objectContaining({
          method: 'GET',
        })
      );
    });

    it('should throw error when room not found', async () => {
      // Arrange
      const roomId = 'INVALID';
      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: false,
        status: 404,
        json: async () => ({ error: 'Room not found: INVALID' }),
      } as Response);

      // Act & Assert
      await expect(getRoom(roomId)).rejects.toThrow();
    });
  });

  // ========== ROOM EXISTS TESTS ==========

  describe('roomExists', () => {
    it('should check if room exists', async () => {
      // Arrange
      const roomId = 'EXIST-ROOM';
      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({ exists: true }),
      } as Response);

      // Act
      const result = await roomExists(roomId);

      // Assert
      expect(result).toBe(true);
    });

    it('should return boolean', async () => {
      // Arrange
      const roomId = 'NOEXIST';
      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({ exists: false }),
      } as Response);

      // Act
      const result = await roomExists(roomId);

      // Assert
      expect(typeof result).toBe('boolean');
      expect(result).toBe(false);
    });
  });

  // ========== JOIN ROOM TESTS ==========

  describe('joinRoom', () => {
    it('should join room with username', async () => {
      // Arrange
      const roomId = 'TEST-ROOM';
      const request: JoinRoomRequest = { username: 'Player2' };
      const mockResponse: RoomResponse = {
        roomId: 'TEST-ROOM',
        players: [
          {
            id: 'p1',
            username: 'Admin',
            team: null,
            role: 'SPECTATOR',
            connected: true,
            admin: true,
          },
          {
            id: 'p2',
            username: 'Player2',
            team: null,
            role: 'SPECTATOR',
            connected: true,
            admin: false,
          },
        ],
        settings: { wordPack: 'english', timerSeconds: null },
        canStart: false,
        adminId: 'p1',
      };

      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockResponse,
      } as Response);

      // Act
      const result = await joinRoom(roomId, request);

      // Assert
      expect(result).toEqual(mockResponse);
      expect(result.players).toHaveLength(2);
    });

    it('should throw error on join failure', async () => {
      // Arrange
      const roomId = 'TEST-ROOM';
      const request: JoinRoomRequest = { username: 'DuplicateUser' };
      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: false,
        status: 409,
        json: async () => ({ error: 'Username already taken: DuplicateUser' }),
      } as Response);

      // Act & Assert
      await expect(joinRoom(roomId, request)).rejects.toThrow();
    });
  });

  // ========== START GAME TESTS ==========

  describe('startGame', () => {
    const createMockGameState = (): GameStateResponse => ({
      board: Array.from({ length: 25 }, (_, i) => ({
        word: `WORD${i}`,
        color: i < 9 ? 'BLUE' : i < 17 ? 'RED' : i < 24 ? 'NEUTRAL' : 'ASSASSIN',
        revealed: false,
        selectedBy: null,
      })),
      currentTeam: 'BLUE',
      phase: 'CLUE',
      currentClue: null,
      guessesRemaining: 0,
      blueRemaining: 9,
      redRemaining: 8,
      winner: null,
      history: [],
    });

    it('should start game successfully', async () => {
      // Arrange
      const roomId = 'TEST-ROOM';
      const mockResponse = createMockGameState();

      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockResponse,
      } as Response);

      // Act
      const result = await startGame(roomId);

      // Assert
      expect(result).toEqual(mockResponse);
      expect(result.board).toHaveLength(25);
      expect(result.phase).toBe('CLUE');
    });

    it('should send POST to correct endpoint', async () => {
      // Arrange
      const roomId = 'ABC12-DEF34';
      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => createMockGameState(),
      } as Response);

      // Act
      await startGame(roomId);

      // Assert
      expect(global.fetch).toHaveBeenCalledWith(
        `/api/rooms/${roomId}/start`,
        expect.objectContaining({
          method: 'POST',
        })
      );
    });

    it('should throw error when game cannot start', async () => {
      // Arrange
      const roomId = 'TEST-ROOM';
      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: async () => ({ error: 'Cannot start game: teams not properly assigned' }),
      } as Response);

      // Act & Assert
      await expect(startGame(roomId)).rejects.toThrow();
    });
  });

  // ========== GET GAME STATE TESTS ==========

  describe('getGameState', () => {
    const createMockGameState = (): GameStateResponse => ({
      board: Array.from({ length: 25 }, (_, i) => ({
        word: `WORD${i}`,
        color: i < 9 ? 'BLUE' : i < 17 ? 'RED' : i < 24 ? 'NEUTRAL' : 'ASSASSIN',
        revealed: false,
        selectedBy: null,
      })),
      currentTeam: 'RED',
      phase: 'GUESS',
      currentClue: { word: 'OCEAN', number: 3, team: 'RED' },
      guessesRemaining: 4,
      blueRemaining: 9,
      redRemaining: 7,
      winner: null,
      history: [
        {
          team: 'BLUE',
          clue: { word: 'WATER', number: 2, team: 'BLUE' },
          guessedWords: ['RIVER'],
          guessedColors: ['BLUE'],
        },
      ],
    });

    it('should get game state', async () => {
      // Arrange
      const roomId = 'TEST-ROOM';
      const mockResponse = createMockGameState();

      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockResponse,
      } as Response);

      // Act
      const result = await getGameState(roomId);

      // Assert
      expect(result).toEqual(mockResponse);
      expect(result.currentClue?.word).toBe('OCEAN');
      expect(result.history).toHaveLength(1);
    });

    it('should send GET to correct endpoint', async () => {
      // Arrange
      const roomId = 'ABC12-DEF34';
      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => createMockGameState(),
      } as Response);

      // Act
      await getGameState(roomId);

      // Assert
      expect(global.fetch).toHaveBeenCalledWith(
        `/api/rooms/${roomId}/game`,
        expect.objectContaining({
          method: 'GET',
        })
      );
    });

    it('should throw error when game not found', async () => {
      // Arrange
      const roomId = 'INVALID-ROOM';
      (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
        ok: false,
        status: 404,
        json: async () => ({ error: 'Game not found for room: INVALID-ROOM' }),
      } as Response);

      // Act & Assert
      await expect(getGameState(roomId)).rejects.toThrow();
    });
  });
});
