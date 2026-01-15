import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import type {
  WSEvent,
  PlayerJoinedEvent,
  PlayerLeftEvent,
  PlayerUpdatedEvent,
  RoomStateEvent,
  ErrorEvent,
  Player,
  GameSettings,
} from '../../types';

// ============================================================================
// MOCKS - Must be defined before imports that use them
// ============================================================================

// Store references to handlers for controlled testing
let capturedMessageHandler: ((event: WSEvent) => void) | null = null;
let capturedStatusHandler: ((status: string) => void) | null = null;

// Mock websocketService - use factory pattern for hoisting
vi.mock('../../services/websocketService', () => ({
  websocketService: {
    connect: vi.fn().mockResolvedValue(undefined),
    connectWithoutJoin: vi.fn().mockResolvedValue(undefined),
    disconnect: vi.fn(),
    onMessage: vi.fn((handler: (event: WSEvent) => void) => {
      capturedMessageHandler = handler;
      return vi.fn();
    }),
    onStatusChange: vi.fn((handler: (status: string) => void) => {
      capturedStatusHandler = handler;
      return vi.fn();
    }),
    setPlayerId: vi.fn(),
    changeTeam: vi.fn(),
  },
}));

// Mock Zustand store
interface MockRoomStore {
  room: {
    roomId: string;
    players: Player[];
    settings: GameSettings;
    canStart: boolean;
    adminId: string;
  } | null;
  currentPlayer: Player | null;
  setRoom: ReturnType<typeof vi.fn>;
  setCurrentPlayer: ReturnType<typeof vi.fn>;
  updatePlayers: ReturnType<typeof vi.fn>;
  reset: ReturnType<typeof vi.fn>;
}

const createMockStore = (): MockRoomStore => ({
  room: null,
  currentPlayer: null,
  setRoom: vi.fn(),
  setCurrentPlayer: vi.fn(),
  updatePlayers: vi.fn(),
  reset: vi.fn(),
});

let mockRoomStore: MockRoomStore = createMockStore();

vi.mock('../../store/roomStore', () => ({
  useRoomStore: Object.assign(
    (selector?: (state: MockRoomStore) => unknown) => {
      return selector ? selector(mockRoomStore) : mockRoomStore;
    },
    {
      getState: () => mockRoomStore,
    }
  ),
}));

// Import after mocks are defined
import { useRoomWebSocket } from '../useRoomWebSocket';
import { websocketService } from '../../services/websocketService';

// ============================================================================
// TEST HELPERS
// ============================================================================

function triggerWebSocketEvent(event: WSEvent): void {
  if (capturedMessageHandler) {
    capturedMessageHandler(event);
  }
}

function triggerStatusChange(status: string): void {
  if (capturedStatusHandler) {
    capturedStatusHandler(status);
  }
}

function createMockPlayer(overrides: Partial<Player> = {}): Player {
  return {
    id: 'player-1',
    username: 'testuser',
    avatar: '',
    team: null,
    role: 'SPECTATOR',
    connected: true,
    admin: false,
    ...overrides,
  };
}

function createMockSettings(): GameSettings {
  return {
    turnTimerSeconds: 60,
    maxPlayers: 8,
    enableHints: true,
    customWords: [],
    wordPackIds: [],
    language: 'en',
  };
}

function setupCreatorScenario(playerId = 'creator-player-id'): void {
  sessionStorage.setItem('isCreator', 'true');
  mockRoomStore.currentPlayer = createMockPlayer({ id: playerId, admin: true });
}

function setupJoinerScenario(): void {
  sessionStorage.setItem('isCreator', 'false');
  mockRoomStore.currentPlayer = null;
}

// ============================================================================
// TEST SUITE
// ============================================================================

describe('useRoomWebSocket', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    mockRoomStore = createMockStore();
    capturedMessageHandler = null;
    capturedStatusHandler = null;

    // Reset mock implementations
    vi.mocked(websocketService.onMessage).mockImplementation((handler) => {
      capturedMessageHandler = handler;
      return vi.fn();
    });

    vi.mocked(websocketService.onStatusChange).mockImplementation((handler) => {
      capturedStatusHandler = handler;
      return vi.fn();
    });

    vi.mocked(websocketService.connect).mockResolvedValue(undefined);
    vi.mocked(websocketService.connectWithoutJoin).mockResolvedValue(undefined);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  // ==========================================================================
  // PHASE 4: FOUNDATION TESTS
  // ==========================================================================

  describe('Initial State', () => {
    it('should start with DISCONNECTED before connection attempt', () => {
      // For creators without playerId, connection won't happen
      sessionStorage.setItem('isCreator', 'true');
      mockRoomStore.currentPlayer = null; // No playerId - connection won't start

      const { result } = renderHook(() =>
        useRoomWebSocket('room123', 'testuser')
      );

      // Should remain DISCONNECTED since connection didn't start
      expect(result.current.connectionStatus).toBe('DISCONNECTED');
    });

    it('should initialize with null error', () => {
      setupJoinerScenario();
      const { result } = renderHook(() =>
        useRoomWebSocket('room123', 'testuser')
      );

      expect(result.current.error).toBeNull();
    });

    it('should report isConnected as false initially', () => {
      setupJoinerScenario();
      const { result } = renderHook(() =>
        useRoomWebSocket('room123', 'testuser')
      );

      expect(result.current.isConnected).toBe(false);
    });
  });

  describe('Room Creator Connection Flow', () => {
    it('should wait for currentPlayer.id before connecting for creators', () => {
      sessionStorage.setItem('isCreator', 'true');
      mockRoomStore.currentPlayer = null; // No player ID yet

      renderHook(() => useRoomWebSocket('room123', 'testuser'));

      // Should not connect without playerId
      expect(websocketService.connectWithoutJoin).not.toHaveBeenCalled();
      expect(websocketService.connect).not.toHaveBeenCalled();
    });

    it('should use connectWithoutJoin() for creators with playerId', async () => {
      setupCreatorScenario('creator-id');

      renderHook(() => useRoomWebSocket('room123', 'testuser'));

      await waitFor(() => {
        expect(websocketService.connectWithoutJoin).toHaveBeenCalledWith(
          'room123'
        );
      });

      // Should NOT use regular connect
      expect(websocketService.connect).not.toHaveBeenCalled();
    });

    it('should detect isCreator from sessionStorage', async () => {
      // Creator scenario
      sessionStorage.setItem('isCreator', 'true');
      mockRoomStore.currentPlayer = createMockPlayer({ id: 'creator-id' });

      renderHook(() => useRoomWebSocket('room123', 'testuser'));

      await waitFor(() => {
        expect(websocketService.connectWithoutJoin).toHaveBeenCalled();
      });
    });

    it('should prevent duplicate connections with hasRequestedConnection ref', async () => {
      setupCreatorScenario();

      const { rerender } = renderHook(() =>
        useRoomWebSocket('room123', 'testuser')
      );

      await waitFor(() => {
        expect(websocketService.connectWithoutJoin).toHaveBeenCalledTimes(1);
      });

      // Trigger re-render
      rerender();
      rerender();

      // Should still only have connected once
      expect(websocketService.connectWithoutJoin).toHaveBeenCalledTimes(1);
    });

    it('should call setPlayerId with currentPlayer.id', async () => {
      setupCreatorScenario('my-player-id');

      renderHook(() => useRoomWebSocket('room123', 'testuser'));

      await waitFor(() => {
        expect(websocketService.setPlayerId).toHaveBeenCalledWith('my-player-id');
      });
    });
  });

  describe('Room Joiner Connection Flow', () => {
    it('should connect immediately for joiners', async () => {
      setupJoinerScenario();

      renderHook(() => useRoomWebSocket('room123', 'testuser'));

      await waitFor(() => {
        expect(websocketService.connect).toHaveBeenCalledWith(
          'room123',
          'testuser'
        );
      });
    });

    it('should not wait for currentPlayer.id for joiners', async () => {
      sessionStorage.setItem('isCreator', 'false');
      mockRoomStore.currentPlayer = null; // No player ID yet

      renderHook(() => useRoomWebSocket('room123', 'joiner-user'));

      await waitFor(() => {
        expect(websocketService.connect).toHaveBeenCalledWith(
          'room123',
          'joiner-user'
        );
      });
    });

    it('should use auto-join via WebSocket connect', async () => {
      setupJoinerScenario();

      renderHook(() => useRoomWebSocket('room123', 'testuser'));

      await waitFor(() => {
        // Joiners use connect() which auto-joins
        expect(websocketService.connect).toHaveBeenCalled();
        expect(websocketService.connectWithoutJoin).not.toHaveBeenCalled();
      });
    });
  });

  // ==========================================================================
  // PHASE 5: EVENT HANDLING TESTS
  // ==========================================================================

  describe('PLAYER_JOINED Event', () => {
    beforeEach(() => {
      setupJoinerScenario();
      mockRoomStore.room = {
        roomId: 'room123',
        players: [createMockPlayer({ id: 'existing-player' })],
        settings: createMockSettings(),
        canStart: false,
        adminId: 'existing-player',
      };
    });

    it('should add new player to room.players', async () => {
      renderHook(() => useRoomWebSocket('room123', 'testuser'));

      const event: PlayerJoinedEvent = {
        type: 'PLAYER_JOINED',
        playerId: 'new-player-id',
        username: 'newuser',
      };

      act(() => {
        triggerWebSocketEvent(event);
      });

      expect(mockRoomStore.updatePlayers).toHaveBeenCalled();
      const updateCall = mockRoomStore.updatePlayers.mock.calls[0][0];
      expect(updateCall).toHaveLength(2);
      expect(updateCall[1]).toMatchObject({
        id: 'new-player-id',
        username: 'newuser',
        connected: true,
      });
    });

    it('should update existing disconnected player (mark connected: true)', async () => {
      // Setup room with a disconnected player
      mockRoomStore.room!.players = [
        createMockPlayer({ id: 'reconnecting-player', connected: false }),
      ];

      renderHook(() => useRoomWebSocket('room123', 'testuser'));

      const event: PlayerJoinedEvent = {
        type: 'PLAYER_JOINED',
        playerId: 'reconnecting-player',
        username: 'reconnector',
      };

      act(() => {
        triggerWebSocketEvent(event);
      });

      expect(mockRoomStore.updatePlayers).toHaveBeenCalled();
      const updateCall = mockRoomStore.updatePlayers.mock.calls[0][0];
      expect(updateCall[0]).toMatchObject({
        id: 'reconnecting-player',
        connected: true,
      });
    });

    it('should handle gracefully when room is null', async () => {
      mockRoomStore.room = null;

      renderHook(() => useRoomWebSocket('room123', 'testuser'));

      const event: PlayerJoinedEvent = {
        type: 'PLAYER_JOINED',
        playerId: 'new-player',
        username: 'newuser',
      };

      // Should not throw
      expect(() => {
        act(() => {
          triggerWebSocketEvent(event);
        });
      }).not.toThrow();

      expect(mockRoomStore.updatePlayers).not.toHaveBeenCalled();
    });
  });

  describe('PLAYER_LEFT Event', () => {
    beforeEach(() => {
      setupJoinerScenario();
      mockRoomStore.room = {
        roomId: 'room123',
        players: [
          createMockPlayer({ id: 'leaving-player', connected: true }),
          createMockPlayer({ id: 'staying-player', connected: true }),
        ],
        settings: createMockSettings(),
        canStart: false,
        adminId: 'staying-player',
      };
    });

    it('should mark player as disconnected (connected: false)', async () => {
      renderHook(() => useRoomWebSocket('room123', 'testuser'));

      const event: PlayerLeftEvent = {
        type: 'PLAYER_LEFT',
        playerId: 'leaving-player',
      };

      act(() => {
        triggerWebSocketEvent(event);
      });

      expect(mockRoomStore.updatePlayers).toHaveBeenCalled();
      const updateCall = mockRoomStore.updatePlayers.mock.calls[0][0];
      const leftPlayer = updateCall.find(
        (p: Player) => p.id === 'leaving-player'
      );
      expect(leftPlayer.connected).toBe(false);
    });

    it('should handle gracefully when player not found', async () => {
      renderHook(() => useRoomWebSocket('room123', 'testuser'));

      const event: PlayerLeftEvent = {
        type: 'PLAYER_LEFT',
        playerId: 'non-existent-player',
      };

      expect(() => {
        act(() => {
          triggerWebSocketEvent(event);
        });
      }).not.toThrow();
    });
  });

  describe('PLAYER_UPDATED Event', () => {
    beforeEach(() => {
      setupJoinerScenario();
      mockRoomStore.room = {
        roomId: 'room123',
        players: [
          createMockPlayer({ id: 'updating-player', team: null, role: 'SPECTATOR' }),
        ],
        settings: createMockSettings(),
        canStart: false,
        adminId: 'updating-player',
      };
    });

    it('should update player team/role in room.players', async () => {
      renderHook(() => useRoomWebSocket('room123', 'testuser'));

      const event: PlayerUpdatedEvent = {
        type: 'PLAYER_UPDATED',
        playerId: 'updating-player',
        team: 'BLUE',
        role: 'OPERATIVE',
      };

      act(() => {
        triggerWebSocketEvent(event);
      });

      expect(mockRoomStore.updatePlayers).toHaveBeenCalled();
      const updateCall = mockRoomStore.updatePlayers.mock.calls[0][0];
      expect(updateCall[0]).toMatchObject({
        id: 'updating-player',
        team: 'BLUE',
        role: 'OPERATIVE',
      });
    });

    it('should update currentPlayer if self-update', async () => {
      mockRoomStore.currentPlayer = createMockPlayer({
        id: 'updating-player',
        team: null,
        role: 'SPECTATOR',
      });

      renderHook(() => useRoomWebSocket('room123', 'testuser'));

      const event: PlayerUpdatedEvent = {
        type: 'PLAYER_UPDATED',
        playerId: 'updating-player',
        team: 'RED',
        role: 'SPYMASTER',
      };

      act(() => {
        triggerWebSocketEvent(event);
      });

      expect(mockRoomStore.setCurrentPlayer).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 'updating-player',
          team: 'RED',
          role: 'SPYMASTER',
        })
      );
    });

    it('should handle gracefully when player not found', async () => {
      renderHook(() => useRoomWebSocket('room123', 'testuser'));

      const event: PlayerUpdatedEvent = {
        type: 'PLAYER_UPDATED',
        playerId: 'non-existent-player',
        team: 'BLUE',
        role: 'OPERATIVE',
      };

      expect(() => {
        act(() => {
          triggerWebSocketEvent(event);
        });
      }).not.toThrow();
    });
  });

  describe('ROOM_STATE Event', () => {
    beforeEach(() => {
      setupJoinerScenario();
    });

    it('should replace entire room state', async () => {
      renderHook(() => useRoomWebSocket('room123', 'testuser'));

      const event: RoomStateEvent = {
        type: 'ROOM_STATE',
        players: [
          createMockPlayer({ id: 'p1', username: 'testuser', admin: true }),
          createMockPlayer({ id: 'p2', username: 'other' }),
        ],
        settings: createMockSettings(),
        gameState: null,
        canStart: true,
      };

      act(() => {
        triggerWebSocketEvent(event);
      });

      expect(mockRoomStore.setRoom).toHaveBeenCalledWith(
        expect.objectContaining({
          roomId: 'room123',
          players: expect.arrayContaining([
            expect.objectContaining({ id: 'p1' }),
            expect.objectContaining({ id: 'p2' }),
          ]),
          canStart: true,
        })
      );
    });

    it('should set currentPlayer if not already set', async () => {
      mockRoomStore.currentPlayer = null;

      renderHook(() => useRoomWebSocket('room123', 'testuser'));

      const event: RoomStateEvent = {
        type: 'ROOM_STATE',
        players: [
          createMockPlayer({ id: 'my-id', username: 'testuser' }),
          createMockPlayer({ id: 'other-id', username: 'otheruser' }),
        ],
        settings: createMockSettings(),
        gameState: null,
        canStart: false,
      };

      act(() => {
        triggerWebSocketEvent(event);
      });

      expect(mockRoomStore.setCurrentPlayer).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 'my-id',
          username: 'testuser',
        })
      );
    });

    it('should call setPlayerId with player ID from ROOM_STATE', async () => {
      mockRoomStore.currentPlayer = null;

      renderHook(() => useRoomWebSocket('room123', 'testuser'));

      const event: RoomStateEvent = {
        type: 'ROOM_STATE',
        players: [createMockPlayer({ id: 'my-ws-player-id', username: 'testuser' })],
        settings: createMockSettings(),
        gameState: null,
        canStart: false,
      };

      act(() => {
        triggerWebSocketEvent(event);
      });

      expect(websocketService.setPlayerId).toHaveBeenCalledWith('my-ws-player-id');
    });
  });

  describe('ERROR Event', () => {
    beforeEach(() => {
      setupJoinerScenario();
    });

    it('should display error message', async () => {
      const { result } = renderHook(() =>
        useRoomWebSocket('room123', 'testuser')
      );

      const event: ErrorEvent = {
        type: 'ERROR',
        message: 'Room is full',
      };

      act(() => {
        triggerWebSocketEvent(event);
      });

      expect(result.current.error).toBe('Room is full');
    });

    it('should auto-clear error after 5 seconds', async () => {
      vi.useFakeTimers();

      const { result } = renderHook(() =>
        useRoomWebSocket('room123', 'testuser')
      );

      const event: ErrorEvent = {
        type: 'ERROR',
        message: 'Test error',
      };

      act(() => {
        triggerWebSocketEvent(event);
      });

      expect(result.current.error).toBe('Test error');

      // Advance time by 5 seconds
      act(() => {
        vi.advanceTimersByTime(5000);
      });

      expect(result.current.error).toBeNull();
    });
  });

  // ==========================================================================
  // PHASE 6: COMPLETION TESTS
  // ==========================================================================

  describe('Team Change', () => {
    beforeEach(() => {
      setupJoinerScenario();
    });

    it('should call changeTeam with correct parameters', async () => {
      mockRoomStore.currentPlayer = createMockPlayer({ id: 'my-player-id' });

      const { result } = renderHook(() =>
        useRoomWebSocket('room123', 'testuser')
      );

      act(() => {
        result.current.changeTeam('BLUE', 'OPERATIVE');
      });

      expect(websocketService.changeTeam).toHaveBeenCalledWith(
        'room123',
        'my-player-id',
        { team: 'BLUE', role: 'OPERATIVE' }
      );
    });

    it('should set error when currentPlayer.id is missing', async () => {
      mockRoomStore.currentPlayer = null;

      const { result } = renderHook(() =>
        useRoomWebSocket('room123', 'testuser')
      );

      act(() => {
        result.current.changeTeam('RED', 'SPYMASTER');
      });

      expect(websocketService.changeTeam).not.toHaveBeenCalled();
      expect(result.current.error).toBe('Cannot change team: player ID not found');
    });
  });

  describe('Cleanup', () => {
    beforeEach(() => {
      setupJoinerScenario();
    });

    it('should disconnect on unmount', async () => {
      const { unmount } = renderHook(() =>
        useRoomWebSocket('room123', 'testuser')
      );

      unmount();

      expect(websocketService.disconnect).toHaveBeenCalled();
    });

    it('should unsubscribe handlers on unmount', async () => {
      const unsubscribeMessage = vi.fn();
      const unsubscribeStatus = vi.fn();

      vi.mocked(websocketService.onMessage).mockReturnValue(unsubscribeMessage);
      vi.mocked(websocketService.onStatusChange).mockReturnValue(unsubscribeStatus);

      const { unmount } = renderHook(() =>
        useRoomWebSocket('room123', 'testuser')
      );

      unmount();

      expect(unsubscribeMessage).toHaveBeenCalled();
      expect(unsubscribeStatus).toHaveBeenCalled();
    });

    it('should disconnect on roomId change', async () => {
      const { rerender, unmount } = renderHook(
        ({ roomId }) => useRoomWebSocket(roomId, 'testuser'),
        { initialProps: { roomId: 'room123' } }
      );

      // Clear disconnect calls from initial render
      vi.mocked(websocketService.disconnect).mockClear();

      // Change roomId
      rerender({ roomId: 'room456' });

      // Should disconnect from old room
      expect(websocketService.disconnect).toHaveBeenCalled();

      unmount();
    });

    it('should reset hasRequestedConnection on connection error', async () => {
      setupJoinerScenario();

      // Make connect fail
      vi.mocked(websocketService.connect).mockRejectedValueOnce(
        new Error('Connection failed')
      );

      const { result } = renderHook(() =>
        useRoomWebSocket('room123', 'testuser')
      );

      // Wait for error
      await waitFor(() => {
        expect(result.current.connectionStatus).toBe('ERROR');
      });

      // Verify error is set
      expect(result.current.error).toBe('Failed to connect to room. Please try again.');
    });
  });

  describe('Connection Status Updates', () => {
    beforeEach(() => {
      setupJoinerScenario();
    });

    it('should update connectionStatus on status change', async () => {
      const { result } = renderHook(() =>
        useRoomWebSocket('room123', 'testuser')
      );

      act(() => {
        triggerStatusChange('CONNECTED');
      });

      expect(result.current.connectionStatus).toBe('CONNECTED');
      expect(result.current.isConnected).toBe(true);
    });

    it('should reflect RECONNECTING status', async () => {
      const { result } = renderHook(() =>
        useRoomWebSocket('room123', 'testuser')
      );

      act(() => {
        triggerStatusChange('RECONNECTING');
      });

      expect(result.current.connectionStatus).toBe('RECONNECTING');
      expect(result.current.isConnected).toBe(false);
    });
  });

  describe('Integration Tests', () => {
    it('should handle full creator flow: wait for ID → connect → receive events', async () => {
      // Step 1: Creator without player ID yet
      sessionStorage.setItem('isCreator', 'true');
      mockRoomStore.currentPlayer = null;

      const { result, rerender } = renderHook(
        ({ playerId }) => {
          mockRoomStore.currentPlayer = playerId
            ? createMockPlayer({ id: playerId, admin: true })
            : null;
          return useRoomWebSocket('room123', 'testuser');
        },
        { initialProps: { playerId: null as string | null } }
      );

      // Should not connect yet
      expect(websocketService.connectWithoutJoin).not.toHaveBeenCalled();

      // Step 2: Player ID becomes available
      rerender({ playerId: 'creator-player-id' });

      await waitFor(() => {
        expect(websocketService.connectWithoutJoin).toHaveBeenCalledWith(
          'room123'
        );
      });

      // Step 3: Receive ROOM_STATE
      act(() => {
        triggerStatusChange('CONNECTED');
      });

      expect(result.current.isConnected).toBe(true);
    });

    it('should handle full joiner flow: connect → join → receive events', async () => {
      setupJoinerScenario();

      const { result } = renderHook(() =>
        useRoomWebSocket('room123', 'joiner-user')
      );

      // Should connect immediately
      await waitFor(() => {
        expect(websocketService.connect).toHaveBeenCalledWith(
          'room123',
          'joiner-user'
        );
      });

      // Simulate connection success
      act(() => {
        triggerStatusChange('CONNECTED');
      });

      expect(result.current.isConnected).toBe(true);

      // Receive ROOM_STATE event
      const roomStateEvent: RoomStateEvent = {
        type: 'ROOM_STATE',
        players: [
          createMockPlayer({ id: 'joiner-id', username: 'joiner-user' }),
          createMockPlayer({ id: 'host-id', username: 'host', admin: true }),
        ],
        settings: createMockSettings(),
        gameState: null,
        canStart: false,
      };

      act(() => {
        triggerWebSocketEvent(roomStateEvent);
      });

      // Should set room and current player
      expect(mockRoomStore.setRoom).toHaveBeenCalled();
    });
  });
});
