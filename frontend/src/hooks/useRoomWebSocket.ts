import { useEffect, useState, useCallback, useRef } from 'react';
import { websocketService } from '../services/websocketService';
import { useRoomStore } from '../store/roomStore';
import type {
  WSEvent,
  ErrorEvent,
  ConnectionStatus,
  ChangeTeamRequest,
  Team,
  Role,
} from '../types';

/**
 * Custom React hook for room WebSocket functionality.
 * Manages WebSocket connection, event handling, and state synchronization.
 *
 * @param roomId - The room ID to connect to
 * @param username - The username to join with
 * @returns WebSocket connection state and control functions
 */
export function useRoomWebSocket(roomId: string, username: string) {
  const [connectionStatus, setConnectionStatus] =
    useState<ConnectionStatus>('DISCONNECTED');
  const [error, setError] = useState<string | null>(null);
  const { currentPlayer } = useRoomStore();
  const hasRequestedConnection = useRef(false);

  /**
   * Handle ERROR event.
   * Displays error messages to the user.
   */
  const handleError = useCallback((event: ErrorEvent) => {
    console.error('WebSocket error:', event.message);
    setError(event.message);

    // Clear error after 5 seconds
    setTimeout(() => setError(null), 5000);
  }, []);

  /**
   * Handle incoming WebSocket events based on type.
   */
  const handleWebSocketEvent = useCallback(
    (event: WSEvent) => {
      const { room, setRoom, updatePlayers, currentPlayer, setCurrentPlayer } =
        useRoomStore.getState();

      switch (event.type) {
        case 'PLAYER_JOINED':
          console.log('Player joined:', event.username);

          if (room) {
            const existingPlayerIndex = room.players.findIndex(
              (p) => p.id === event.playerId
            );

            if (existingPlayerIndex >= 0) {
              const updatedPlayers = room.players.map((p) =>
                p.id === event.playerId ? { ...p, connected: true } : p
              );
              updatePlayers(updatedPlayers);
            } else {
              const newPlayer = {
                id: event.playerId,
                username: event.username,
                avatar: '',
                team: null,
                role: 'SPECTATOR' as Role,
                connected: true,
                admin: false,
              };

              updatePlayers([...room.players, newPlayer]);
            }
          }

          break;
        case 'PLAYER_LEFT':
          console.log('Player left:', event.playerId);

          if (room) {
            const updatedPlayers = room.players.map((p) =>
              p.id === event.playerId ? { ...p, connected: false } : p
            );
            updatePlayers(updatedPlayers);
          }

          break;
        case 'PLAYER_UPDATED':
          console.log('Player updated:', event);

          if (room) {
            const updatedPlayers = room.players.map((player) => {
              if (player.id === event.playerId) {
                return {
                  ...player,
                  team: event.team,
                  role: event.role,
                };
              }
              return player;
            });
            updatePlayers(updatedPlayers);

            if (currentPlayer?.id === event.playerId) {
              setCurrentPlayer({
                ...currentPlayer,
                team: event.team,
                role: event.role,
              });
            }
          }

          break;
        case 'ROOM_STATE':
          console.log('Room state received:', event);

          // Find admin player ID
          const adminPlayer = event.players.find((p) => p.admin);
          const adminId = adminPlayer?.id || '';

          setRoom({
            roomId: roomId,
            players: event.players,
            settings: event.settings,
            canStart: event.canStart,
            adminId: adminId,
          });

          // Set current player if not already set
          if (!currentPlayer && event.players.length > 0) {
            const myPlayer = event.players.find(
              (p) => p.username.toLowerCase() === username.toLowerCase()
            );
            if (myPlayer) {
              setCurrentPlayer(myPlayer);
              websocketService.setPlayerId(myPlayer.id);
            }
          }
          break;
        case 'ERROR':
          handleError(event);
          break;
        default:
          console.warn('Unknown WebSocket event type:', event);
      }
    },
    [roomId, username, handleError]
  );

  /**
   * Change team/role for the current player.
   */
  const changeTeam = useCallback(
    (team: Team | null, role: Role) => {
      if (!currentPlayer?.id) {
        console.error('Cannot change team: current player ID not found');
        setError('Cannot change team: player ID not found');
        return;
      }

      const request: ChangeTeamRequest = {
        team: team,
        role: role,
      };

      websocketService.changeTeam(roomId, currentPlayer.id, request);
    },
    [roomId, currentPlayer]
  );

  /**
   * Keep websocketService updated with the latest playerId so it can use reconnect
   * instead of sending duplicate join messages on STOMP reconnects.
   */
  useEffect(() => {
    websocketService.setPlayerId(currentPlayer?.id ?? null);
  }, [currentPlayer?.id]);

  /**
   * Register handlers once per room.
   */
  useEffect(() => {
    const unsubscribeMessage = websocketService.onMessage(handleWebSocketEvent);
    const unsubscribeStatus = websocketService.onStatusChange(setConnectionStatus);

    return () => {
      unsubscribeMessage();
      unsubscribeStatus();
    };
  }, [handleWebSocketEvent]);

  /**
   * Connect to WebSocket (exactly once per roomId).
   * Room creators: wait for playerId, then connect + auto-reconnect.
   * Room joiners: connect + auto-join.
   */
  useEffect(() => {
    if (!roomId || hasRequestedConnection.current) {
      return;
    }

    const isCreator = sessionStorage.getItem('isCreator') === 'true';

    if (isCreator && !currentPlayer?.id) {
      return;
    }

    hasRequestedConnection.current = true;
    setConnectionStatus('CONNECTING');

    console.log(`Connecting to room ${roomId} as ${username}...`);

    if (isCreator) {
      console.log('Room creator detected - using reconnect flow');
      // CRITICAL: Set playerId synchronously BEFORE starting connection
      // This ensures playerId is available when onConnect fires and calls ensurePresence
      websocketService.setPlayerId(currentPlayer!.id);
      websocketService
        .connectWithoutJoin(roomId)
        .catch((error) => {
          console.error('Failed to connect to WebSocket:', error);
          setConnectionStatus('ERROR');
          setError('Failed to connect to room. Please try again.');
          hasRequestedConnection.current = false;
        });
      return;
    }

    websocketService
      .connect(roomId, username)
      .catch((error) => {
        console.error('Failed to connect to WebSocket:', error);
        setConnectionStatus('ERROR');
        setError('Failed to connect to room. Please try again.');
        hasRequestedConnection.current = false;
      });
  }, [roomId, username, currentPlayer?.id]);

  /**
   * Disconnect when leaving the room (roomId change/unmount).
   */
  useEffect(() => {
    return () => {
      console.log('Cleaning up WebSocket connection...');
      websocketService.disconnect();
      hasRequestedConnection.current = false;
    };
  }, [roomId]);

  return {
    connectionStatus,
    error,
    changeTeam,
    isConnected: connectionStatus === 'CONNECTED',
  };
}
