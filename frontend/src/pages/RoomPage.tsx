import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useRoomStore } from '../store/roomStore';
import { useGameStore } from '../store/gameStore';
import { useRoomWebSocket } from '../hooks/useRoomWebSocket';
import { startGame } from '../services/api';
import type { Team, Role, GameState } from '../types';

/**
 * RoomPage - Real-time multiplayer room lobby with WebSocket integration.
 * Players can join, select teams/roles, and see live updates.
 */
export default function RoomPage() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const { room, currentPlayer, reset } = useRoomStore();
  const { setGameState } = useGameStore();
  const [isStarting, setIsStarting] = useState(false);
  const [startError, setStartError] = useState<string | null>(null);
  const canStart = room?.canStart ?? false;
  const isAdmin = currentPlayer?.admin ?? false;

  // Get username from session storage (set during room creation/join)
  const username = sessionStorage.getItem('username') || 'Guest';

  // Connect to WebSocket
  const { connectionStatus, error, changeTeam, isConnected } =
    useRoomWebSocket(roomId!, username);

  const handleBackToHome = () => {
    reset();
    sessionStorage.removeItem('playerId');
    sessionStorage.removeItem('roomId');
    sessionStorage.removeItem('isCreator');
    navigate('/');
  };

  const totalPlayers = room?.players.length ?? 0;
  const onlinePlayers = room?.players.filter((p) => p.connected).length ?? 0;

  // Handle team/role change
  const handleTeamChange = (team: Team | null, role: Role) => {
    changeTeam(team, role);
  };

  // Handle game start
  const handleStartGame = async () => {
    if (!canStart || !roomId || isStarting) {
      return;
    }

    setIsStarting(true);
    setStartError(null);

    try {
      const response = await startGame(roomId);

      // Map API response to GameState (history -> turnHistory)
      const gameState: GameState = {
        board: response.board.map(card => ({
          word: card.word,
          color:
            currentPlayer?.role === 'SPYMASTER' || card.revealed ? card.color : null,
          revealed: card.revealed,
          selectedBy: card.selectedBy ?? undefined,
        })),
        currentTeam: response.currentTeam,
        phase: response.phase,
        currentClue: response.currentClue,
        guessesRemaining: response.guessesRemaining,
        blueRemaining: response.blueRemaining,
        redRemaining: response.redRemaining,
        winner: response.winner,
        turnHistory: response.history,
      };

      setGameState(gameState);

      // Navigate to game page
      navigate(`/room/${roomId}/game`);
    } catch (err) {
      console.error('Failed to start game:', err);
      setStartError(err instanceof Error ? err.message : 'Failed to start game');
    } finally {
      setIsStarting(false);
    }
  };

  // Connection status indicator
  const getStatusColor = () => {
    switch (connectionStatus) {
      case 'CONNECTED':
        return 'bg-green-500';
      case 'CONNECTING':
      case 'RECONNECTING':
        return 'bg-yellow-500';
      case 'ERROR':
      case 'DISCONNECTED':
        return 'bg-red-500';
      default:
        return 'bg-gray-500';
    }
  };

  const getStatusText = () => {
    switch (connectionStatus) {
      case 'CONNECTED':
        return 'Connected';
      case 'CONNECTING':
        return 'Connecting...';
      case 'RECONNECTING':
        return 'Reconnecting...';
      case 'ERROR':
        return 'Connection Error';
      case 'DISCONNECTED':
        return 'Disconnected';
      default:
        return 'Unknown';
    }
  };

  // Get team color
  const getTeamColor = (team: Team | null) => {
    if (team === 'BLUE') return 'bg-blue-500 text-white';
    if (team === 'RED') return 'bg-red-500 text-white';
    return 'bg-gray-300 text-gray-700';
  };

  // Get role badge
  const getRoleBadge = (role: Role) => {
    if (role === 'SPYMASTER') return 'bg-purple-600 text-white';
    if (role === 'OPERATIVE') return 'bg-blue-600 text-white';
    return 'bg-gray-600 text-white';
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 p-4">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="bg-white rounded-lg shadow-lg p-6 mb-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-800">Room Lobby</h1>
              <p className="text-gray-600 mt-1">
                Room Code:{' '}
                <span className="font-mono font-bold text-lg">{roomId}</span>
              </p>
            </div>
            {/* Connection Status */}
            <div className="flex items-center gap-2">
              <div className={`w-3 h-3 rounded-full ${getStatusColor()}`}></div>
              <span className="text-sm text-gray-600">{getStatusText()}</span>
            </div>
          </div>

          {/* Error Message */}
          {error && (
            <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-sm text-red-700">{error}</p>
            </div>
          )}
        </div>

        {/* Main Content */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Team Selection */}
          {currentPlayer && isConnected && (
            <div className="md:col-span-1">
              <div className="bg-white rounded-lg shadow-lg p-6">
                <h2 className="text-xl font-semibold text-gray-800 mb-4">
                  Your Role
                </h2>

                {/* Current Status */}
                <div className="mb-6 p-4 bg-gray-50 rounded-lg">
                  <p className="text-sm text-gray-600 mb-2">Current Team:</p>
                  <div
                    className={`inline-block px-4 py-2 rounded-lg font-semibold ${getTeamColor(currentPlayer.team)}`}
                  >
                    {currentPlayer.team || 'SPECTATOR'}
                  </div>
                  <p className="text-sm text-gray-600 mt-3 mb-2">
                    Current Role:
                  </p>
                  <div
                    className={`inline-block px-4 py-2 rounded-lg font-semibold text-sm ${getRoleBadge(currentPlayer.role)}`}
                  >
                    {currentPlayer.role}
                  </div>
                </div>

                {/* Team Selection Buttons */}
                <div className="space-y-3">
                  <h3 className="text-sm font-semibold text-gray-700">
                    Join a Team:
                  </h3>

                  {/* Blue Team */}
                  <div>
                    <p className="text-xs text-gray-600 mb-1">Blue Team</p>
                    <div className="flex gap-2">
                      <button
                        onClick={() => handleTeamChange('BLUE', 'OPERATIVE')}
                        className="flex-1 bg-blue-500 hover:bg-blue-600 text-white font-semibold py-2 px-4 rounded-lg transition duration-200 text-sm"
                      >
                        Operative
                      </button>
                      <button
                        onClick={() => handleTeamChange('BLUE', 'SPYMASTER')}
                        className="flex-1 bg-blue-700 hover:bg-blue-800 text-white font-semibold py-2 px-4 rounded-lg transition duration-200 text-sm"
                      >
                        Spymaster
                      </button>
                    </div>
                  </div>

                  {/* Red Team */}
                  <div>
                    <p className="text-xs text-gray-600 mb-1">Red Team</p>
                    <div className="flex gap-2">
                      <button
                        onClick={() => handleTeamChange('RED', 'OPERATIVE')}
                        className="flex-1 bg-red-500 hover:bg-red-600 text-white font-semibold py-2 px-4 rounded-lg transition duration-200 text-sm"
                      >
                        Operative
                      </button>
                      <button
                        onClick={() => handleTeamChange('RED', 'SPYMASTER')}
                        className="flex-1 bg-red-700 hover:bg-red-800 text-white font-semibold py-2 px-4 rounded-lg transition duration-200 text-sm"
                      >
                        Spymaster
                      </button>
                    </div>
                  </div>

                  {/* Spectator */}
                  <button
                    onClick={() => handleTeamChange(null, 'SPECTATOR')}
                    className="w-full bg-gray-500 hover:bg-gray-600 text-white font-semibold py-2 px-4 rounded-lg transition duration-200 text-sm"
                  >
                    Become Spectator
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Players List */}
          <div className="md:col-span-2">
            <div className="bg-white rounded-lg shadow-lg p-6">
              <h2 className="text-xl font-semibold text-gray-800 mb-4">
                Players ({totalPlayers})
                <span className="text-sm font-normal text-gray-500 ml-2">
                  {onlinePlayers} online
                </span>
              </h2>

              {room && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {/* Blue Team */}
                  <div>
                    <h3 className="text-sm font-semibold text-blue-700 mb-2">
                      Blue Team
                    </h3>
                    <div className="space-y-2">
                      {room.players
                        .filter((p) => p.team === 'BLUE')
                        .map((player) => (
                          <div
                            key={player.id}
                            className={`p-3 bg-blue-50 rounded-lg border border-blue-200 ${!player.connected ? 'opacity-60' : ''}`}
                          >
                            <div className="flex items-center justify-between">
                              <span
                                className={`font-medium ${player.connected ? 'text-gray-800' : 'text-gray-500 line-through'}`}
                              >
                                {player.username}
                              </span>
                              <div className="flex items-center gap-2">
                                {!player.connected && (
                                  <span className="px-2 py-1 text-xs rounded-full bg-gray-200 text-gray-700">
                                    Disconnected
                                  </span>
                                )}
                                <span
                                  className={`px-2 py-1 text-xs rounded-full ${getRoleBadge(player.role)}`}
                                >
                                  {player.role}
                                </span>
                              </div>
                            </div>
                            {player.admin && (
                              <span className="text-xs text-blue-600 mt-1 block">
                                ⭐ Admin
                              </span>
                            )}
                          </div>
                        ))}
                      {room.players.filter((p) => p.team === 'BLUE').length ===
                        0 && (
                        <p className="text-sm text-gray-400 italic">
                          No players yet
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Red Team */}
                  <div>
                    <h3 className="text-sm font-semibold text-red-700 mb-2">
                      Red Team
                    </h3>
                    <div className="space-y-2">
                      {room.players
                        .filter((p) => p.team === 'RED')
                        .map((player) => (
                          <div
                            key={player.id}
                            className={`p-3 bg-red-50 rounded-lg border border-red-200 ${!player.connected ? 'opacity-60' : ''}`}
                          >
                            <div className="flex items-center justify-between">
                              <span
                                className={`font-medium ${player.connected ? 'text-gray-800' : 'text-gray-500 line-through'}`}
                              >
                                {player.username}
                              </span>
                              <div className="flex items-center gap-2">
                                {!player.connected && (
                                  <span className="px-2 py-1 text-xs rounded-full bg-gray-200 text-gray-700">
                                    Disconnected
                                  </span>
                                )}
                                <span
                                  className={`px-2 py-1 text-xs rounded-full ${getRoleBadge(player.role)}`}
                                >
                                  {player.role}
                                </span>
                              </div>
                            </div>
                            {player.admin && (
                              <span className="text-xs text-red-600 mt-1 block">
                                ⭐ Admin
                              </span>
                            )}
                          </div>
                        ))}
                      {room.players.filter((p) => p.team === 'RED').length ===
                        0 && (
                        <p className="text-sm text-gray-400 italic">
                          No players yet
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Spectators */}
                  {room.players.filter((p) => p.team === null).length > 0 && (
                    <div className="md:col-span-2">
                      <h3 className="text-sm font-semibold text-gray-700 mb-2">
                        Spectators
                      </h3>
                      <div className="space-y-2">
                        {room.players
                          .filter((p) => p.team === null)
                          .map((player) => (
                            <div
                              key={player.id}
                              className={`p-3 bg-gray-50 rounded-lg border border-gray-200 inline-block mr-2 ${!player.connected ? 'opacity-60' : ''}`}
                            >
                              <span
                                className={`font-medium ${player.connected ? 'text-gray-800' : 'text-gray-500 line-through'}`}
                              >
                                {player.username}
                              </span>
                              {player.admin && (
                                <span className="text-xs text-gray-600 ml-2">
                                  ⭐ Admin
                                </span>
                              )}
                              {!player.connected && (
                                <span className="text-xs text-gray-500 ml-2">
                                  (disconnected)
                                </span>
                              )}
                            </div>
                          ))}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Start Game Section - Admin Only */}
        {isAdmin && (
          <div className="bg-white rounded-lg shadow-lg p-6 mt-6">
            {startError && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
                <p className="text-sm text-red-700">{startError}</p>
              </div>
            )}
            <button
              onClick={handleStartGame}
              disabled={!canStart || isStarting}
              className={`w-full px-6 py-3 rounded-lg text-lg font-semibold transition duration-200 ${
                canStart && !isStarting
                  ? 'bg-green-500 hover:bg-green-600 text-white'
                  : 'bg-gray-300 text-gray-500 cursor-not-allowed'
              }`}
            >
              {isStarting ? 'Starting Game...' : canStart ? 'Start Game' : 'Waiting for Teams...'}
            </button>
            {!canStart && (
              <p className="text-sm text-gray-600 text-center mt-2">
                Need at least 4 players with 1 Spymaster per team
              </p>
            )}
          </div>
        )}

        {/* Action Button */}
        <div className="mt-6">
          <button
            onClick={handleBackToHome}
            className="bg-gray-600 hover:bg-gray-700 text-white font-semibold py-3 px-6 rounded-lg transition duration-200"
          >
            ← Back to Home
          </button>
        </div>
      </div>
    </div>
  );
}
