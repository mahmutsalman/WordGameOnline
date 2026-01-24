import { Client } from '@stomp/stompjs';
import type { IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type {
  WSEvent,
  JoinRoomWsRequest,
  ChangeTeamRequest,
  ConnectionStatus,
} from '../types';

/**
 * WebSocket service for real-time room communication.
 * Uses SockJS + STOMP protocol to match backend Spring Boot WebSocket configuration.
 */

type MessageHandler = (event: WSEvent) => void;
type ConnectionStatusHandler = (status: ConnectionStatus) => void;

interface WebSocketConfig {
  url: string;
  reconnectDelay?: number;
  heartbeatIncoming?: number;
  heartbeatOutgoing?: number;
  debug?: boolean;
}

export class WebSocketService {
  private client: Client | null = null;
  private roomSubscription: StompSubscription | null = null;
  private gameSubscription: StompSubscription | null = null;
  private privateSubscription: StompSubscription | null = null;
  private messageHandlers: Set<MessageHandler> = new Set();
  private statusHandlers: Set<ConnectionStatusHandler> = new Set();
  private currentRoomId: string | null = null;
  private currentUsername: string | null = null;
  private currentPlayerId: string | null = null;
  private autoJoinEnabled = false;
  private connectPromise: Promise<void> | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private config: WebSocketConfig;
  private intentionalDisconnect = false;
  // Connection instance ID to invalidate stale callbacks from old connections
  private connectionInstanceId = 0;

  constructor(config?: Partial<WebSocketConfig>) {
    this.config = {
      url: config?.url || 'http://localhost:8080/ws',
      reconnectDelay: config?.reconnectDelay || 5000,
      heartbeatIncoming: config?.heartbeatIncoming || 10000,
      heartbeatOutgoing: config?.heartbeatOutgoing || 10000,
      debug: config?.debug || false,
    };
  }

  /**
   * Connect to WebSocket server and join a specific room.
   * @param roomId - The room ID to join
   * @param username - The username to join with
   */
  public connect(roomId: string, username: string): Promise<void> {
    this.currentRoomId = roomId;
    this.currentUsername = username;
    this.autoJoinEnabled = true;
    this.intentionalDisconnect = false;

    if (this.client?.active) {
      if (this.client.connected) {
        this.setupSubscriptions(roomId);
        this.ensurePresence(roomId);
      }
      return this.connectPromise ?? Promise.resolve();
    }

    // Capture current instance ID to detect stale callbacks
    // Only increment when actually creating a new connection
    const instanceId = ++this.connectionInstanceId;

    this.connectPromise = new Promise((resolve, reject) => {
      this.updateStatus('CONNECTING');

      // Create STOMP client with SockJS
      this.client = new Client({
        webSocketFactory: () => new SockJS(this.config.url) as WebSocket,
        connectHeaders: {},
        debug: this.config.debug ? (str) => console.log(str) : () => {}, // No-op function when debug is false
        reconnectDelay: this.config.reconnectDelay,
        heartbeatIncoming: this.config.heartbeatIncoming,
        heartbeatOutgoing: this.config.heartbeatOutgoing,

        onConnect: () => {
          // Guard: ignore if this is a stale connection (from before a disconnect)
          if (this.connectionInstanceId !== instanceId) {
            console.log('Ignoring stale connection callback (connect)');
            this.client?.deactivate();
            return;
          }

          console.log('WebSocket connected');
          this.reconnectAttempts = 0;
          this.updateStatus('CONNECTED');
          this.setupSubscriptions(roomId);
          this.ensurePresence(roomId);
          this.connectPromise = null;
          resolve();
        },

        onStompError: (frame) => {
          console.error('STOMP error:', frame);
          this.updateStatus('ERROR');
          this.connectPromise = null;
          reject(new Error(`STOMP error: ${frame.headers['message']}`));
        },

        onWebSocketError: (event) => {
          console.error('WebSocket error:', event);
          this.updateStatus('ERROR');
          this.connectPromise = null;
          reject(new Error('WebSocket connection error'));
        },

        onDisconnect: () => {
          console.log('WebSocket disconnected');
          this.updateStatus('DISCONNECTED');
        },

        onWebSocketClose: () => {
          if (this.intentionalDisconnect) {
            return;
          }

          if (!this.currentRoomId) {
            return;
          }

          this.reconnectAttempts++;

          if (this.reconnectAttempts > this.maxReconnectAttempts) {
            console.error('Max reconnection attempts reached');
            this.updateStatus('ERROR');
            this.client?.deactivate();
            return;
          }

          this.updateStatus('RECONNECTING');
        },
      });

      // Activate the client
      this.client.activate();
    });

    return this.connectPromise;
  }

  /**
   * Connect to WebSocket server without sending join message.
   * Use this when player already joined via REST API.
   * @param roomId - The room ID to subscribe to
   */
  public connectWithoutJoin(roomId: string): Promise<void> {
    this.currentRoomId = roomId;
    this.autoJoinEnabled = false;
    this.intentionalDisconnect = false;

    if (this.client?.active) {
      if (this.client.connected) {
        this.setupSubscriptions(roomId);
        this.ensurePresence(roomId);
      }
      return this.connectPromise ?? Promise.resolve();
    }

    // Capture current instance ID to detect stale callbacks
    // Only increment when actually creating a new connection
    const instanceId = ++this.connectionInstanceId;

    this.connectPromise = new Promise((resolve, reject) => {
      this.updateStatus('CONNECTING');

      // Create STOMP client with SockJS
      this.client = new Client({
        webSocketFactory: () => new SockJS(this.config.url) as WebSocket,
        connectHeaders: {},
        debug: this.config.debug ? (str) => console.log(str) : () => {},
        reconnectDelay: this.config.reconnectDelay,
        heartbeatIncoming: this.config.heartbeatIncoming,
        heartbeatOutgoing: this.config.heartbeatOutgoing,

        onConnect: () => {
          // Guard: ignore if this is a stale connection (from before a disconnect)
          if (this.connectionInstanceId !== instanceId) {
            console.log('Ignoring stale connection callback (connectWithoutJoin)');
            this.client?.deactivate();
            return;
          }

          console.log('WebSocket connected (without join)');
          this.reconnectAttempts = 0;
          this.updateStatus('CONNECTED');
          this.setupSubscriptions(roomId);
          this.ensurePresence(roomId);
          this.connectPromise = null;
          resolve();
        },

        onStompError: (frame) => {
          console.error('STOMP error:', frame);
          this.updateStatus('ERROR');
          this.connectPromise = null;
          reject(new Error(`STOMP error: ${frame.headers['message']}`));
        },

        onWebSocketError: (event) => {
          console.error('WebSocket error:', event);
          this.updateStatus('ERROR');
          this.connectPromise = null;
          reject(new Error('WebSocket connection error'));
        },

        onDisconnect: () => {
          console.log('WebSocket disconnected');
          this.updateStatus('DISCONNECTED');
        },

        onWebSocketClose: () => {
          if (this.intentionalDisconnect) {
            return;
          }

          if (!this.currentRoomId) {
            return;
          }

          this.reconnectAttempts++;

          if (this.reconnectAttempts > this.maxReconnectAttempts) {
            console.error('Max reconnection attempts reached');
            this.updateStatus('ERROR');
            this.client?.deactivate();
            return;
          }

          this.updateStatus('RECONNECTING');
        },
      });

      // Activate the client
      this.client.activate();
    });

    return this.connectPromise;
  }

  /**
   * Disconnect from WebSocket server.
   */
  public disconnect(): void {
    // Increment instance ID FIRST to invalidate any pending callbacks
    // This prevents stale onConnect callbacks from executing after disconnect
    this.connectionInstanceId++;

    if (this.client?.active) {
      this.intentionalDisconnect = true;
      this.reconnectAttempts = 0;
      this.connectPromise = null;

      // Unsubscribe from topics
      this.roomSubscription?.unsubscribe();
      this.gameSubscription?.unsubscribe();
      this.privateSubscription?.unsubscribe();
      this.roomSubscription = null;
      this.gameSubscription = null;
      this.privateSubscription = null;

      // Deactivate client
      this.client.deactivate();
      this.client = null;
      this.currentRoomId = null;
      this.currentUsername = null;
      this.currentPlayerId = null;
      this.autoJoinEnabled = false;
      this.updateStatus('DISCONNECTED');
      console.log('WebSocket disconnected');
    }
  }

  /**
   * Setup subscriptions for room broadcasts and private messages.
   * @param roomId - The room ID to subscribe to
   */
  private setupSubscriptions(roomId: string): void {
    if (!this.client?.connected) {
      console.error('Cannot setup subscriptions: client not connected');
      return;
    }

    // Avoid duplicate subscriptions when the client reconnects
    this.roomSubscription?.unsubscribe();
    this.privateSubscription?.unsubscribe();

    // Subscribe to room topic for broadcasts
    this.roomSubscription = this.client.subscribe(
      `/topic/room/${roomId}`,
      (message: IMessage) => {
        this.handleMessage(message);
      }
    );

    // Unsubscribe from previous game subscription
    this.gameSubscription?.unsubscribe();

    // Subscribe to game topic for game state broadcasts
    this.gameSubscription = this.client.subscribe(
      `/topic/room/${roomId}/game`,
      (message: IMessage) => {
        this.handleMessage(message);
      }
    );

    // Subscribe to private queue for direct messages
    this.privateSubscription = this.client.subscribe(
      '/user/queue/private',
      (message: IMessage) => {
        this.handleMessage(message);
      }
    );

    console.log(`Subscribed to room ${roomId} and private queue`);
  }

  /**
   * Handle incoming WebSocket message.
   * @param message - The STOMP message
   */
  private handleMessage(message: IMessage): void {
    try {
      const event: WSEvent = JSON.parse(message.body);
      console.log('Received WebSocket event:', event.type, event);

      // Notify all registered handlers
      this.messageHandlers.forEach((handler) => {
        try {
          handler(event);
        } catch (error) {
          console.error('Error in message handler:', error);
        }
      });
    } catch (error) {
      console.error('Error parsing WebSocket message:', error);
    }
  }

  /**
   * Set the current playerId so reconnect can be used after transport reconnects.
   */
  public setPlayerId(playerId: string | null): void {
    this.currentPlayerId = playerId;
  }

  /**
   * Send join room request via WebSocket.
   * @param roomId - The room ID to join
   * @param username - The username to join with
   */
  private joinRoom(roomId: string, username: string): void {
    if (!this.client?.connected) {
      console.error('Cannot join room: client not connected');
      return;
    }

    const joinRequest: JoinRoomWsRequest = { username };

    this.client.publish({
      destination: `/app/room/${roomId}/join`,
      body: JSON.stringify(joinRequest),
    });

    console.log(`Sent join request for room ${roomId} as ${username}`);
  }

  /**
   * Reconnect an existing player to establish WebSocket session.
   * Use this when player joined via REST API and needs to link their playerId to WebSocket session.
   * @param roomId - The room ID
   * @param playerId - The player ID from REST API join
   */
  public reconnectPlayer(roomId: string, playerId: string): void {
    if (!this.client?.connected) {
      console.error('Cannot reconnect: client not connected');
      return;
    }

    this.client.publish({
      destination: `/app/room/${roomId}/reconnect`,
      body: JSON.stringify({ playerId }),
    });

    console.log(`Sent reconnect request for player ${playerId} in room ${roomId}`);
  }

  /**
   * Change team/role for the current player.
   * @param roomId - The room ID
   * @param playerId - The player ID for validation (prevents impersonation)
   * @param request - The team change request
   */
  public changeTeam(roomId: string, playerId: string, request: ChangeTeamRequest): void {
    if (!this.client?.connected) {
      console.error('Cannot change team: client not connected');
      return;
    }

    // Include playerId in request for server-side validation
    const requestWithPlayerId = {
      ...request,
      playerId,
    };

    this.client.publish({
      destination: `/app/room/${roomId}/team`,
      body: JSON.stringify(requestWithPlayerId),
    });

    console.log('Sent team change request:', requestWithPlayerId);
  }

  /**
   * Submit a clue (spymaster action).
   */
  public submitClue(roomId: string, word: string, number: number): void {
    if (!this.client?.connected) {
      console.error('Cannot submit clue: client not connected');
      return;
    }

    this.client.publish({
      destination: `/app/room/${roomId}/clue`,
      body: JSON.stringify({ word, number }),
    });
  }

  /**
   * Make a guess (operative action).
   */
  public makeGuess(roomId: string, cardIndex: number): void {
    if (!this.client?.connected) {
      console.error('Cannot make guess: client not connected');
      return;
    }

    this.client.publish({
      destination: `/app/room/${roomId}/guess`,
      body: JSON.stringify({ cardIndex }),
    });
  }

  /**
   * Register a message handler to receive WebSocket events.
   * @param handler - Function to handle incoming events
   * @returns Cleanup function to unregister the handler
   */
  public onMessage(handler: MessageHandler): () => void {
    this.messageHandlers.add(handler);
    return () => {
      this.messageHandlers.delete(handler);
    };
  }

  /**
   * Register a connection status handler.
   * @param handler - Function to handle status changes
   * @returns Cleanup function to unregister the handler
   */
  public onStatusChange(handler: ConnectionStatusHandler): () => void {
    this.statusHandlers.add(handler);
    return () => {
      this.statusHandlers.delete(handler);
    };
  }

  /**
   * Update connection status and notify handlers.
   * @param status - The new connection status
   */
  private updateStatus(status: ConnectionStatus): void {
    this.statusHandlers.forEach((handler) => {
      try {
        handler(status);
      } catch (error) {
        console.error('Error in status handler:', error);
      }
    });
  }

  /**
   * Check if WebSocket is currently connected.
   */
  public isConnected(): boolean {
    return this.client?.connected ?? false;
  }

  /**
   * Get the current room ID.
   */
  public getCurrentRoomId(): string | null {
    return this.currentRoomId;
  }

  private ensurePresence(roomId: string): void {
    if (!this.client?.connected) {
      return;
    }

    if (this.currentPlayerId) {
      this.reconnectPlayer(roomId, this.currentPlayerId);
      return;
    }

    if (this.autoJoinEnabled && this.currentUsername) {
      this.joinRoom(roomId, this.currentUsername);
    }
  }
}

// Export singleton instance
export const websocketService = new WebSocketService();
