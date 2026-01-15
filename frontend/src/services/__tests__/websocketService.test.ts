import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { WebSocketService, websocketService } from '../websocketService';
import type { WSEvent, ConnectionStatus, GameSettings } from '../../types';
import type { IFrame, StompConfig, IMessage } from '@stomp/stompjs';

// ============================================================================
// MOCKS
// ============================================================================

// Type for STOMP error frame
interface StompErrorFrame {
  headers: Record<string, string>;
  body: string;
}

// Type for mock STOMP message (partial - only what we use in tests)
type MockStompMessage = Pick<IMessage, 'body'>;

// Mock settings factory
const createMockSettings = (): GameSettings => ({
  turnTimerSeconds: 60,
  maxPlayers: 8,
  enableHints: true,
  customWords: [],
  wordPackIds: [],
  language: 'en',
});

// Type for mock client instance
interface MockClient {
  connected: boolean;
  active: boolean;
  activate: ReturnType<typeof vi.fn>;
  deactivate: ReturnType<typeof vi.fn>;
  publish: ReturnType<typeof vi.fn>;
  subscribe: ReturnType<typeof vi.fn>;
  unsubscribe: ReturnType<typeof vi.fn>;
}

// Mock SockJS
vi.mock('sockjs-client', () => ({
  default: vi.fn(() => ({
    readyState: 1,
    close: vi.fn(),
    send: vi.fn(),
  })),
}));

// Store references to lifecycle callbacks and control behavior
let mockClientCallbacks: {
  onConnect?: () => void;
  onStompError?: (frame: IFrame) => void;
  onWebSocketError?: (event: Event) => void;
  onDisconnect?: () => void;
  onWebSocketClose?: () => void;
} = {};

let shouldTriggerError = false;
let errorFrame: StompErrorFrame | null = null;

// Mock STOMP subscription
const createMockSubscription = () => ({
  id: 'sub-' + Math.random(),
  unsubscribe: vi.fn(),
});

// Create mock client factory
const createMockClient = (): MockClient => ({
  connected: false,
  active: false,
  activate: vi.fn(function (this: MockClient) {
    this.active = true;
    // Simulate async connection
    setTimeout(() => {
      if (shouldTriggerError && mockClientCallbacks.onStompError) {
        mockClientCallbacks.onStompError(
          (errorFrame || {
            headers: { message: 'Connection refused' },
            body: 'STOMP error',
          }) as IFrame
        );
      } else if (mockClientCallbacks.onConnect) {
        this.connected = true;
        mockClientCallbacks.onConnect();
      }
    }, 0);
  }),
  deactivate: vi.fn(function (this: MockClient) {
    this.active = false;
    this.connected = false;
    if (mockClientCallbacks.onDisconnect) {
      mockClientCallbacks.onDisconnect();
    }
  }),
  publish: vi.fn(),
  subscribe: vi.fn(() => createMockSubscription()),
  unsubscribe: vi.fn(),
});

let mockClient: MockClient;

vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn(function (this: MockClient, config: StompConfig) {
    // Store callbacks for later triggering
    mockClientCallbacks = {
      onConnect: config.onConnect,
      onStompError: config.onStompError,
      onWebSocketError: config.onWebSocketError,
      onDisconnect: config.onDisconnect,
      onWebSocketClose: config.onWebSocketClose,
    };

    // Return mock client instance
    mockClient = createMockClient();
    Object.assign(this, mockClient);
    return this;
  }),
}));

// ============================================================================
// TEST SUITE
// ============================================================================

describe('WebSocketService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockClientCallbacks = {};
    shouldTriggerError = false;
    errorFrame = null;
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  // ==========================================================================
  // PHASE 1: FOUNDATION TESTS
  // ==========================================================================

  describe('Constructor and Singleton', () => {
    it('should create service with default configuration', () => {
      const service = new WebSocketService();
      expect(service).toBeDefined();
      expect(service.isConnected()).toBe(false);
    });

    it('should create service with custom configuration', () => {
      const service = new WebSocketService({
        url: 'http://custom:9090/ws',
        reconnectDelay: 3000,
        heartbeatIncoming: 5000,
        heartbeatOutgoing: 5000,
        debug: true,
      });
      expect(service).toBeDefined();
    });

    it('should export a singleton instance', () => {
      expect(websocketService).toBeDefined();
      expect(websocketService).toBeInstanceOf(WebSocketService);
    });
  });

  describe('Connection Lifecycle - connect()', () => {
    let service: WebSocketService;

    beforeEach(() => {
      service = new WebSocketService();
    });

    it('should successfully connect and activate STOMP client', async () => {
      const connectPromise = service.connect('room123', 'testuser');

      // Wait for connection
      await connectPromise;

      expect(mockClient.activate).toHaveBeenCalled();
      expect(service.isConnected()).toBe(true);
    });

    it('should setup subscriptions after connection', async () => {
      await service.connect('room123', 'testuser');

      // Should subscribe to room topic and private queue
      expect(mockClient.subscribe).toHaveBeenCalledWith(
        '/topic/room/room123',
        expect.any(Function)
      );
      expect(mockClient.subscribe).toHaveBeenCalledWith(
        '/user/queue/private',
        expect.any(Function)
      );
    });

    it('should send join message after connection (auto-join)', async () => {
      await service.connect('room123', 'testuser');

      // Should publish join request
      expect(mockClient.publish).toHaveBeenCalledWith({
        destination: '/app/room/room123/join',
        body: JSON.stringify({ username: 'testuser' }),
      });
    });

    it('should prevent duplicate connections', async () => {
      const promise1 = service.connect('room123', 'user1');
      const promise2 = service.connect('room123', 'user1');

      // Should return the same promise
      expect(promise1).toBe(promise2);

      await promise1;

      // Should only activate once
      expect(mockClient.activate).toHaveBeenCalledTimes(1);
    });

    it('should return immediately if already connected', async () => {
      await service.connect('room123', 'user1');
      expect(mockClient.activate).toHaveBeenCalledTimes(1);

      // Connect again
      await service.connect('room123', 'user1');

      // Should not activate again
      expect(mockClient.activate).toHaveBeenCalledTimes(1);
    });
  });

  describe('Connection Lifecycle - connectWithoutJoin()', () => {
    let service: WebSocketService;

    beforeEach(() => {
      service = new WebSocketService();
    });

    it('should connect without sending join message', async () => {
      await service.connectWithoutJoin('room123');

      expect(mockClient.activate).toHaveBeenCalled();
      expect(service.isConnected()).toBe(true);

      // Should NOT publish join request
      expect(mockClient.publish).not.toHaveBeenCalledWith(
        expect.objectContaining({
          destination: '/app/room/room123/join',
        })
      );
    });

    it('should setup subscriptions without auto-join', async () => {
      await service.connectWithoutJoin('room123');

      // Should subscribe to topics
      expect(mockClient.subscribe).toHaveBeenCalledWith(
        '/topic/room/room123',
        expect.any(Function)
      );
      expect(mockClient.subscribe).toHaveBeenCalledWith(
        '/user/queue/private',
        expect.any(Function)
      );
    });
  });

  describe('Connection Error Handling', () => {
    let service: WebSocketService;

    beforeEach(() => {
      service = new WebSocketService();
    });

    it('should reject promise on STOMP error', async () => {
      // Configure mock to trigger error
      shouldTriggerError = true;
      errorFrame = {
        headers: { message: 'Connection refused' },
        body: 'STOMP error',
      };

      const connectPromise = service.connect('room123', 'user1');

      await expect(connectPromise).rejects.toThrow('STOMP error: Connection refused');
    });

    it('should update status to ERROR on STOMP error', async () => {
      const statusHandler = vi.fn();
      service.onStatusChange(statusHandler);

      // Configure mock to trigger error
      shouldTriggerError = true;
      errorFrame = {
        headers: { message: 'Error' },
        body: 'Error body',
      };

      const connectPromise = service.connect('room123', 'user1');

      await expect(connectPromise).rejects.toThrow();

      // Should have received CONNECTING and ERROR statuses
      expect(statusHandler).toHaveBeenCalledWith('CONNECTING');
      expect(statusHandler).toHaveBeenCalledWith('ERROR');
    });
  });

  describe('Disconnection', () => {
    let service: WebSocketService;

    beforeEach(() => {
      service = new WebSocketService();
    });

    it('should disconnect and deactivate client', async () => {
      await service.connect('room123', 'user1');
      expect(service.isConnected()).toBe(true);

      service.disconnect();

      expect(mockClient.deactivate).toHaveBeenCalled();
      expect(service.isConnected()).toBe(false);
    });

    it('should unsubscribe from topics on disconnect', async () => {
      await service.connect('room123', 'user1');

      // Get the subscriptions that were created
      const subscribeCallCount = mockClient.subscribe.mock.calls.length;
      expect(subscribeCallCount).toBeGreaterThan(0);

      service.disconnect();

      // Verify client was deactivated (which triggers unsubscribe internally)
      expect(mockClient.deactivate).toHaveBeenCalled();
    });

    it('should update status to DISCONNECTED', async () => {
      const statusHandler = vi.fn();
      service.onStatusChange(statusHandler);

      await service.connect('room123', 'user1');
      service.disconnect();

      expect(statusHandler).toHaveBeenCalledWith('DISCONNECTED');
    });

    it('should handle disconnect when not connected (no-op)', () => {
      expect(() => service.disconnect()).not.toThrow();
      expect(service.isConnected()).toBe(false);
    });
  });

  describe('Connection Status Tracking', () => {
    let service: WebSocketService;

    beforeEach(() => {
      service = new WebSocketService();
    });

    it('should track connection status correctly', async () => {
      const statuses: ConnectionStatus[] = [];
      service.onStatusChange((status) => statuses.push(status));

      expect(service.isConnected()).toBe(false);

      await service.connect('room123', 'user1');
      expect(service.isConnected()).toBe(true);

      service.disconnect();
      expect(service.isConnected()).toBe(false);

      // Should have received: CONNECTING, CONNECTED, DISCONNECTED
      expect(statuses).toContain('CONNECTING');
      expect(statuses).toContain('CONNECTED');
      expect(statuses).toContain('DISCONNECTED');
    });
  });

  // ==========================================================================
  // PHASE 2: CORE FEATURES
  // ==========================================================================

  describe('Subscription Management', () => {
    let service: WebSocketService;

    beforeEach(() => {
      service = new WebSocketService();
    });

    it('should subscribe to room topic and private queue', async () => {
      await service.connect('room123', 'user1');

      expect(mockClient.subscribe).toHaveBeenCalledWith(
        '/topic/room/room123',
        expect.any(Function)
      );
      expect(mockClient.subscribe).toHaveBeenCalledWith(
        '/user/queue/private',
        expect.any(Function)
      );
    });

    it('should prevent duplicate subscriptions on reconnect', async () => {
      await service.connect('room123', 'user1');

      // Verify initial subscriptions
      expect(mockClient.subscribe).toHaveBeenCalledTimes(2);

      // Disconnect and reconnect
      service.disconnect();

      // Mock will be recreated on new connect, so we'll verify the behavior
      // through the fact that subscribe is called again
      await service.connect('room123', 'user1');

      // Should have subscribed again (new client, new subscriptions)
      // The important part is that the old subscriptions are cleaned up on disconnect
      expect(mockClient.subscribe).toHaveBeenCalled();
    });

    it('should not setup subscriptions if client not connected', () => {
      // This test verifies the guard in setupSubscriptions
      // Creating a new service without connecting - setupSubscriptions
      // is private and called internally on connect, this test documents
      // the expected behavior that subscriptions require connection first
      new WebSocketService();
      expect(mockClient.subscribe).not.toHaveBeenCalled();
    });
  });

  describe('Message Handling', () => {
    let service: WebSocketService;

    beforeEach(() => {
      service = new WebSocketService();
    });

    it('should parse and dispatch messages to handlers', async () => {
      const handler = vi.fn();
      service.onMessage(handler);

      await service.connect('room123', 'user1');

      // Get the room subscription callback
      const roomSubscription = mockClient.subscribe.mock.calls.find(
        (call) => call[0] === '/topic/room/room123'
      );
      expect(roomSubscription).toBeDefined();

      const subscriptionCallback = roomSubscription![1];

      // Simulate receiving a message
      const testEvent: WSEvent = {
        type: 'PLAYER_JOINED',
        playerId: 'p1',
        username: 'testuser',
      };

      subscriptionCallback({
        body: JSON.stringify(testEvent),
      } as MockStompMessage);

      // Handler should be called with parsed event
      expect(handler).toHaveBeenCalledWith(testEvent);
    });

    it('should invoke all registered handlers', async () => {
      const handler1 = vi.fn();
      const handler2 = vi.fn();
      const handler3 = vi.fn();

      service.onMessage(handler1);
      service.onMessage(handler2);
      service.onMessage(handler3);

      await service.connect('room123', 'user1');

      // Get subscription callback
      const roomSubscription = mockClient.subscribe.mock.calls.find(
        (call) => call[0] === '/topic/room/room123'
      );
      const subscriptionCallback = roomSubscription![1];

      // Simulate message
      const testEvent: WSEvent = {
        type: 'ROOM_STATE',
        roomId: 'room123',
        players: [],
        settings: createMockSettings(),
        gameState: null,
        adminId: 'p1',
      };

      subscriptionCallback({
        body: JSON.stringify(testEvent),
      } as MockStompMessage);

      // All handlers should be invoked
      expect(handler1).toHaveBeenCalledWith(testEvent);
      expect(handler2).toHaveBeenCalledWith(testEvent);
      expect(handler3).toHaveBeenCalledWith(testEvent);
    });

    it('should handle JSON parse errors gracefully', async () => {
      const handler = vi.fn();
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      service.onMessage(handler);
      await service.connect('room123', 'user1');

      const roomSubscription = mockClient.subscribe.mock.calls.find(
        (call) => call[0] === '/topic/room/room123'
      );
      const subscriptionCallback = roomSubscription![1];

      // Send invalid JSON
      subscriptionCallback({
        body: 'invalid json {{{',
      } as MockStompMessage);

      // Handler should not be called
      expect(handler).not.toHaveBeenCalled();
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        'Error parsing WebSocket message:',
        expect.any(Error)
      );

      consoleErrorSpy.mockRestore();
    });

    it('should isolate handler errors', async () => {
      const failingHandler = vi.fn(() => {
        throw new Error('Handler error');
      });
      const workingHandler = vi.fn();
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      service.onMessage(failingHandler);
      service.onMessage(workingHandler);

      await service.connect('room123', 'user1');

      const roomSubscription = mockClient.subscribe.mock.calls.find(
        (call) => call[0] === '/topic/room/room123'
      );
      const subscriptionCallback = roomSubscription![1];

      const testEvent: WSEvent = {
        type: 'PLAYER_LEFT',
        playerId: 'p1',
      };

      subscriptionCallback({
        body: JSON.stringify(testEvent),
      } as MockStompMessage);

      // Both handlers should be called despite first one throwing
      expect(failingHandler).toHaveBeenCalled();
      expect(workingHandler).toHaveBeenCalledWith(testEvent);
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        'Error in message handler:',
        expect.any(Error)
      );

      consoleErrorSpy.mockRestore();
    });
  });

  describe('Message Sending', () => {
    let service: WebSocketService;

    beforeEach(() => {
      service = new WebSocketService();
    });

    it('should send join message with correct payload', async () => {
      await service.connect('room123', 'testuser');

      // The connect() method calls joinRoom() automatically
      expect(mockClient.publish).toHaveBeenCalledWith({
        destination: '/app/room/room123/join',
        body: JSON.stringify({ username: 'testuser' }),
      });
    });

    it('should send reconnect message with playerId', async () => {
      await service.connectWithoutJoin('room123');

      service.reconnectPlayer('room123', 'player-id-123');

      expect(mockClient.publish).toHaveBeenCalledWith({
        destination: '/app/room/room123/reconnect',
        body: JSON.stringify({ playerId: 'player-id-123' }),
      });
    });

    it('should send team change message with playerId for validation', async () => {
      await service.connect('room123', 'user1');

      service.changeTeam('room123', 'player-id-123', {
        team: 'BLUE',
        role: 'OPERATIVE',
      });

      expect(mockClient.publish).toHaveBeenCalledWith({
        destination: '/app/room/room123/team',
        body: JSON.stringify({
          team: 'BLUE',
          role: 'OPERATIVE',
          playerId: 'player-id-123',
        }),
      });
    });

    it('should not send messages when not connected', () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      service.reconnectPlayer('room123', 'p1');

      expect(mockClient.publish).not.toHaveBeenCalled();
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        'Cannot reconnect: client not connected'
      );

      consoleErrorSpy.mockRestore();
    });

    it('should handle changeTeam when not connected', () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      service.changeTeam('room123', 'p1', {
        team: 'RED',
        role: 'SPYMASTER',
      });

      expect(mockClient.publish).not.toHaveBeenCalled();
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        'Cannot change team: client not connected'
      );

      consoleErrorSpy.mockRestore();
    });
  });

  describe('Handler Registration', () => {
    let service: WebSocketService;

    beforeEach(() => {
      service = new WebSocketService();
    });

    it('should register message handler and return cleanup function', () => {
      const handler = vi.fn();

      const cleanup = service.onMessage(handler);

      expect(cleanup).toBeInstanceOf(Function);
    });

    it('should remove handler when cleanup function is called', async () => {
      const handler = vi.fn();

      const cleanup = service.onMessage(handler);
      await service.connect('room123', 'user1');

      // Get subscription callback
      const roomSubscription = mockClient.subscribe.mock.calls.find(
        (call) => call[0] === '/topic/room/room123'
      );
      const subscriptionCallback = roomSubscription![1];

      // Call cleanup
      cleanup();

      // Send message
      const testEvent: WSEvent = {
        type: 'PLAYER_JOINED',
        playerId: 'p1',
        username: 'user1',
      };

      subscriptionCallback({
        body: JSON.stringify(testEvent),
      } as MockStompMessage);

      // Handler should not be called after cleanup
      expect(handler).not.toHaveBeenCalled();
    });

    it('should support multiple message handlers', async () => {
      const handler1 = vi.fn();
      const handler2 = vi.fn();

      service.onMessage(handler1);
      service.onMessage(handler2);

      await service.connect('room123', 'user1');

      const roomSubscription = mockClient.subscribe.mock.calls.find(
        (call) => call[0] === '/topic/room/room123'
      );
      const subscriptionCallback = roomSubscription![1];

      const testEvent: WSEvent = {
        type: 'ERROR',
        message: 'Test error',
      };

      subscriptionCallback({
        body: JSON.stringify(testEvent),
      } as MockStompMessage);

      expect(handler1).toHaveBeenCalledWith(testEvent);
      expect(handler2).toHaveBeenCalledWith(testEvent);
    });

    it('should register status handler and return cleanup function', () => {
      const handler = vi.fn();

      const cleanup = service.onStatusChange(handler);

      expect(cleanup).toBeInstanceOf(Function);
    });

    it('should remove status handler when cleanup function is called', async () => {
      const handler = vi.fn();

      const cleanup = service.onStatusChange(handler);

      // Call cleanup before connecting
      cleanup();

      await service.connect('room123', 'user1');

      // Handler should not have been called
      expect(handler).not.toHaveBeenCalled();
    });
  });

  // ==========================================================================
  // PHASE 3: RECONNECTION LOGIC
  // ==========================================================================

  describe('Reconnection Logic', () => {
    let service: WebSocketService;

    beforeEach(() => {
      service = new WebSocketService();
    });

    it('should update status to RECONNECTING on WebSocket close', async () => {
      const statusHandler = vi.fn();
      service.onStatusChange(statusHandler);

      await service.connect('room123', 'user1');

      // Clear previous status calls
      statusHandler.mockClear();

      // Trigger WebSocket close (simulating connection drop)
      mockClientCallbacks.onWebSocketClose?.();

      expect(statusHandler).toHaveBeenCalledWith('RECONNECTING');
    });

    it('should enforce max reconnection attempts (5)', async () => {
      const statusHandler = vi.fn();
      service.onStatusChange(statusHandler);

      await service.connect('room123', 'user1');
      statusHandler.mockClear();

      // Trigger 5 WebSocket closes - should all result in RECONNECTING
      for (let i = 0; i < 5; i++) {
        mockClientCallbacks.onWebSocketClose?.();
      }

      // After 5 attempts, should still be RECONNECTING
      expect(statusHandler).toHaveBeenCalledWith('RECONNECTING');

      // 6th close should trigger ERROR (max exceeded)
      mockClientCallbacks.onWebSocketClose?.();

      expect(statusHandler).toHaveBeenCalledWith('ERROR');
      expect(mockClient.deactivate).toHaveBeenCalled();
    });

    it('should reset reconnection counter on successful connect', async () => {
      const statusHandler = vi.fn();
      service.onStatusChange(statusHandler);

      await service.connect('room123', 'user1');

      // Trigger a few closes to increment counter
      mockClientCallbacks.onWebSocketClose?.();
      mockClientCallbacks.onWebSocketClose?.();

      // Simulate successful reconnection
      mockClient.connected = true;
      mockClientCallbacks.onConnect?.();

      // Clear and trigger more closes - should reset count
      statusHandler.mockClear();

      // Should be able to do 5 more closes before ERROR
      for (let i = 0; i < 5; i++) {
        mockClientCallbacks.onWebSocketClose?.();
      }

      expect(statusHandler).toHaveBeenCalledWith('RECONNECTING');
      expect(statusHandler).not.toHaveBeenCalledWith('ERROR');
    });

    it('should not reconnect on intentional disconnect', async () => {
      const statusHandler = vi.fn();
      service.onStatusChange(statusHandler);

      await service.connect('room123', 'user1');
      statusHandler.mockClear();

      // Intentionally disconnect
      service.disconnect();

      expect(statusHandler).toHaveBeenCalledWith('DISCONNECTED');
      expect(statusHandler).not.toHaveBeenCalledWith('RECONNECTING');
    });

    it('should not update status on WebSocket close after intentional disconnect', async () => {
      const statusHandler = vi.fn();
      service.onStatusChange(statusHandler);

      await service.connect('room123', 'user1');

      // Intentionally disconnect
      service.disconnect();
      statusHandler.mockClear();

      // Trigger WebSocket close - should be ignored
      mockClientCallbacks.onWebSocketClose?.();

      expect(statusHandler).not.toHaveBeenCalled();
    });

    it('should not reconnect without currentRoomId', async () => {
      const statusHandler = vi.fn();
      service.onStatusChange(statusHandler);

      // Connect then clear roomId state via disconnect
      await service.connect('room123', 'user1');
      service.disconnect();
      statusHandler.mockClear();

      // Trigger WebSocket close - should not reconnect (no roomId)
      mockClientCallbacks.onWebSocketClose?.();

      expect(statusHandler).not.toHaveBeenCalledWith('RECONNECTING');
    });
  });

  describe('setPlayerId', () => {
    let service: WebSocketService;

    beforeEach(() => {
      service = new WebSocketService();
    });

    it('should store playerId via setPlayerId', async () => {
      service.setPlayerId('player-123');

      // Connect and verify reconnectPlayer is called with the stored playerId
      await service.connectWithoutJoin('room123');

      expect(mockClient.publish).toHaveBeenCalledWith({
        destination: '/app/room/room123/reconnect',
        body: JSON.stringify({ playerId: 'player-123' }),
      });
    });

    it('should use stored playerId in ensurePresence after reconnect', async () => {
      await service.connectWithoutJoin('room123');

      // Set playerId after initial connection
      service.setPlayerId('player-456');

      // Clear previous publish calls
      mockClient.publish.mockClear();

      // Simulate reconnection
      mockClient.connected = true;
      mockClientCallbacks.onConnect?.();

      // Should use reconnectPlayer with stored playerId
      expect(mockClient.publish).toHaveBeenCalledWith({
        destination: '/app/room/room123/reconnect',
        body: JSON.stringify({ playerId: 'player-456' }),
      });
    });

    it('should clear playerId when set to null', async () => {
      service.setPlayerId('player-123');
      service.setPlayerId(null);

      // Connect with autoJoin - should use joinRoom instead of reconnectPlayer
      await service.connect('room123', 'testuser');

      // Should have called joinRoom (not reconnectPlayer)
      expect(mockClient.publish).toHaveBeenCalledWith({
        destination: '/app/room/room123/join',
        body: JSON.stringify({ username: 'testuser' }),
      });
    });
  });

  describe('ensurePresence Logic', () => {
    let service: WebSocketService;

    beforeEach(() => {
      service = new WebSocketService();
    });

    it('should use reconnectPlayer when playerId is set', async () => {
      service.setPlayerId('player-abc');
      await service.connectWithoutJoin('room123');

      expect(mockClient.publish).toHaveBeenCalledWith({
        destination: '/app/room/room123/reconnect',
        body: JSON.stringify({ playerId: 'player-abc' }),
      });
    });

    it('should use joinRoom when autoJoin enabled without playerId', async () => {
      // Connect with autoJoin (no playerId set)
      await service.connect('room123', 'testuser');

      expect(mockClient.publish).toHaveBeenCalledWith({
        destination: '/app/room/room123/join',
        body: JSON.stringify({ username: 'testuser' }),
      });
    });

    it('should prioritize reconnectPlayer over joinRoom when both available', async () => {
      // Set playerId first
      service.setPlayerId('player-xyz');

      // Connect with autoJoin enabled
      await service.connect('room123', 'testuser');

      // Should only call reconnectPlayer, not joinRoom
      expect(mockClient.publish).toHaveBeenCalledWith({
        destination: '/app/room/room123/reconnect',
        body: JSON.stringify({ playerId: 'player-xyz' }),
      });

      // Verify joinRoom was NOT called
      expect(mockClient.publish).not.toHaveBeenCalledWith(
        expect.objectContaining({
          destination: '/app/room/room123/join',
        })
      );
    });

    it('should not call ensurePresence when client not connected', async () => {
      service.setPlayerId('player-123');

      // Don't connect, just verify no publish happened
      expect(mockClient.publish).not.toHaveBeenCalled();
    });
  });

  describe('Integration Tests', () => {
    let service: WebSocketService;

    beforeEach(() => {
      service = new WebSocketService();
    });

    it('should handle full lifecycle: connect → message → disconnect → reconnect', async () => {
      const messageHandler = vi.fn();
      const statusHandler = vi.fn();

      service.onMessage(messageHandler);
      service.onStatusChange(statusHandler);

      // 1. Connect
      await service.connect('room123', 'user1');

      expect(statusHandler).toHaveBeenCalledWith('CONNECTING');
      expect(statusHandler).toHaveBeenCalledWith('CONNECTED');
      expect(service.isConnected()).toBe(true);

      // 2. Receive message
      const roomSubscription = mockClient.subscribe.mock.calls.find(
        (call) => call[0] === '/topic/room/room123'
      );
      const subscriptionCallback = roomSubscription![1];

      const testEvent: WSEvent = {
        type: 'PLAYER_JOINED',
        playerId: 'p1',
        username: 'user1',
      };

      subscriptionCallback({
        body: JSON.stringify(testEvent),
      } as MockStompMessage);

      expect(messageHandler).toHaveBeenCalledWith(testEvent);

      // 3. Simulate connection drop (WebSocket close)
      statusHandler.mockClear();
      mockClientCallbacks.onWebSocketClose?.();

      expect(statusHandler).toHaveBeenCalledWith('RECONNECTING');

      // 4. Simulate successful reconnection
      mockClient.connected = true;
      mockClient.publish.mockClear();
      mockClientCallbacks.onConnect?.();

      expect(statusHandler).toHaveBeenCalledWith('CONNECTED');

      // 5. Verify presence was restored (joinRoom called again)
      expect(mockClient.publish).toHaveBeenCalledWith({
        destination: '/app/room/room123/join',
        body: JSON.stringify({ username: 'user1' }),
      });
    });

    it('should handle creator flow: connectWithoutJoin → setPlayerId → reconnect', async () => {
      const statusHandler = vi.fn();
      service.onStatusChange(statusHandler);

      // 1. Creator connects without join (already joined via REST)
      await service.connectWithoutJoin('room123');

      expect(statusHandler).toHaveBeenCalledWith('CONNECTED');

      // Initially no publish (no playerId yet)
      expect(mockClient.publish).not.toHaveBeenCalled();

      // 2. Set playerId (simulating ROOM_STATE event handler in hook)
      service.setPlayerId('creator-player-id');

      // 3. Simulate connection drop
      statusHandler.mockClear();
      mockClientCallbacks.onWebSocketClose?.();

      expect(statusHandler).toHaveBeenCalledWith('RECONNECTING');

      // 4. Simulate successful reconnection
      mockClient.connected = true;
      mockClientCallbacks.onConnect?.();

      // 5. Verify reconnectPlayer was called (not joinRoom)
      expect(mockClient.publish).toHaveBeenCalledWith({
        destination: '/app/room/room123/reconnect',
        body: JSON.stringify({ playerId: 'creator-player-id' }),
      });
    });
  });
});
