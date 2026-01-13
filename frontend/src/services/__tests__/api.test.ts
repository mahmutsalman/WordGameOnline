import { describe, it, expect, beforeEach, vi } from 'vitest';
import {
  createRoom,
  getRoom,
  roomExists,
  joinRoom,
  CreateRoomRequest,
  JoinRoomRequest,
  RoomResponse,
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
});
