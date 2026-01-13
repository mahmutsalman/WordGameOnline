import { describe, it, expect, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useRoomStore } from '../roomStore';
import type { RoomResponse, PlayerResponse } from '../../services/api';

describe('useRoomStore', () => {
  beforeEach(() => {
    // Reset store state before each test
    const { result } = renderHook(() => useRoomStore());
    act(() => {
      result.current.reset();
    });
  });

  it('should initialize with null room', () => {
    // Arrange & Act
    const { result } = renderHook(() => useRoomStore());

    // Assert
    expect(result.current.room).toBeNull();
  });

  it('should initialize with null current player', () => {
    // Arrange & Act
    const { result } = renderHook(() => useRoomStore());

    // Assert
    expect(result.current.currentPlayer).toBeNull();
  });

  it('should set room', () => {
    // Arrange
    const { result } = renderHook(() => useRoomStore());
    const mockRoom: RoomResponse = {
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
      settings: {
        wordPack: 'english',
        timerSeconds: null,
      },
      canStart: false,
      adminId: 'p1',
    };

    // Act
    act(() => {
      result.current.setRoom(mockRoom);
    });

    // Assert
    expect(result.current.room).toEqual(mockRoom);
  });

  it('should set current player', () => {
    // Arrange
    const { result } = renderHook(() => useRoomStore());
    const mockPlayer: PlayerResponse = {
      id: 'p1',
      username: 'TestUser',
      team: 'BLUE',
      role: 'OPERATIVE',
      connected: true,
      admin: false,
    };

    // Act
    act(() => {
      result.current.setCurrentPlayer(mockPlayer);
    });

    // Assert
    expect(result.current.currentPlayer).toEqual(mockPlayer);
  });

  it('should update players list', () => {
    // Arrange
    const { result } = renderHook(() => useRoomStore());
    const mockRoom: RoomResponse = {
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

    const newPlayers: PlayerResponse[] = [
      {
        id: 'p1',
        username: 'User1',
        team: null,
        role: 'SPECTATOR',
        connected: true,
        admin: true,
      },
      {
        id: 'p2',
        username: 'User2',
        team: null,
        role: 'SPECTATOR',
        connected: true,
        admin: false,
      },
    ];

    // Act
    act(() => {
      result.current.setRoom(mockRoom);
      result.current.updatePlayers(newPlayers);
    });

    // Assert
    expect(result.current.room?.players).toEqual(newPlayers);
    expect(result.current.room?.players).toHaveLength(2);
  });

  it('should preserve room ID when updating players', () => {
    // Arrange
    const { result } = renderHook(() => useRoomStore());
    const mockRoom: RoomResponse = {
      roomId: 'PRESERVE-ID',
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

    const newPlayers: PlayerResponse[] = [
      {
        id: 'p1',
        username: 'User1',
        team: 'BLUE',
        role: 'SPYMASTER',
        connected: true,
        admin: true,
      },
    ];

    // Act
    act(() => {
      result.current.setRoom(mockRoom);
      result.current.updatePlayers(newPlayers);
    });

    // Assert
    expect(result.current.room?.roomId).toBe('PRESERVE-ID');
  });

  it('should reset to initial state', () => {
    // Arrange
    const { result } = renderHook(() => useRoomStore());
    const mockRoom: RoomResponse = {
      roomId: 'TEST-ROOM',
      players: [],
      settings: { wordPack: 'english', timerSeconds: null },
      canStart: false,
      adminId: 'admin-1',
    };
    const mockPlayer: PlayerResponse = {
      id: 'p1',
      username: 'TestUser',
      team: null,
      role: 'SPECTATOR',
      connected: true,
      admin: false,
    };

    // Act
    act(() => {
      result.current.setRoom(mockRoom);
      result.current.setCurrentPlayer(mockPlayer);
    });

    expect(result.current.room).not.toBeNull();
    expect(result.current.currentPlayer).not.toBeNull();

    act(() => {
      result.current.reset();
    });

    // Assert
    expect(result.current.room).toBeNull();
    expect(result.current.currentPlayer).toBeNull();
  });

  it('should handle multiple state updates', () => {
    // Arrange
    const { result } = renderHook(() => useRoomStore());
    const room1: RoomResponse = {
      roomId: 'ROOM-1',
      players: [],
      settings: { wordPack: 'english', timerSeconds: null },
      canStart: false,
      adminId: 'admin-1',
    };
    const room2: RoomResponse = {
      roomId: 'ROOM-2',
      players: [],
      settings: { wordPack: 'spanish', timerSeconds: 60 },
      canStart: false,
      adminId: 'admin-2',
    };

    // Act
    act(() => {
      result.current.setRoom(room1);
    });
    expect(result.current.room?.roomId).toBe('ROOM-1');

    act(() => {
      result.current.setRoom(room2);
    });

    // Assert
    expect(result.current.room?.roomId).toBe('ROOM-2');
    expect(result.current.room?.settings.wordPack).toBe('spanish');
  });

  it('should not mutate original state', () => {
    // Arrange
    const { result } = renderHook(() => useRoomStore());
    const originalRoom: RoomResponse = {
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

    // Act
    act(() => {
      result.current.setRoom(originalRoom);
    });

    const storedRoom = result.current.room;
    const newPlayers: PlayerResponse[] = [
      {
        id: 'p2',
        username: 'User2',
        team: null,
        role: 'SPECTATOR',
        connected: true,
        admin: false,
      },
    ];

    act(() => {
      result.current.updatePlayers(newPlayers);
    });

    // Assert
    expect(originalRoom.players).toHaveLength(1);
    expect(storedRoom?.players).not.toBe(originalRoom.players);
  });

  it('should work with TypeScript types', () => {
    // Arrange & Act
    const { result } = renderHook(() => useRoomStore());

    // Assert - TypeScript type checking
    expect(result.current.room).toBeNull();
    expect(typeof result.current.setRoom).toBe('function');
    expect(typeof result.current.setCurrentPlayer).toBe('function');
    expect(typeof result.current.updatePlayers).toBe('function');
    expect(typeof result.current.reset).toBe('function');
  });
});
