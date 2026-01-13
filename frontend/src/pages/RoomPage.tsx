import { useParams } from 'react-router-dom';
import { useRoomStore } from '../store/roomStore';

/**
 * RoomPage - Placeholder for Step-02.
 * Full lobby functionality (WebSocket, team selection, game start) will be implemented in Step-03.
 */
export default function RoomPage() {
  const { roomId } = useParams<{ roomId: string }>();
  const { room, currentPlayer } = useRoomStore();

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
      <div className="max-w-2xl w-full bg-white rounded-lg shadow-lg p-8">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-gray-800 mb-2">Room Lobby</h1>
          <p className="text-gray-600">Room Code: <span className="font-mono font-bold text-lg">{roomId}</span></p>
        </div>

        {/* Player Info */}
        {currentPlayer && (
          <div className="mb-6 p-4 bg-blue-50 rounded-lg">
            <p className="text-sm text-gray-600">You joined as:</p>
            <p className="text-xl font-semibold text-blue-700">{currentPlayer.username}</p>
            {currentPlayer.admin && (
              <span className="inline-block mt-2 px-3 py-1 bg-blue-600 text-white text-xs rounded-full">
                Admin
              </span>
            )}
          </div>
        )}

        {/* Players List */}
        {room && (
          <div className="mb-6">
            <h2 className="text-xl font-semibold text-gray-800 mb-4">
              Players ({room.players.length})
            </h2>
            <div className="space-y-2">
              {room.players.map((player) => (
                <div
                  key={player.id}
                  className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-gradient-to-br from-blue-400 to-purple-500 rounded-full flex items-center justify-center text-white font-bold">
                      {player.username.charAt(0).toUpperCase()}
                    </div>
                    <span className="font-medium text-gray-800">{player.username}</span>
                  </div>
                  {player.admin && (
                    <span className="px-2 py-1 bg-blue-100 text-blue-700 text-xs rounded-full">
                      Admin
                    </span>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Step 3 Notice */}
        <div className="mt-8 p-6 bg-yellow-50 border-2 border-yellow-200 rounded-lg">
          <h3 className="text-lg font-semibold text-yellow-800 mb-2">
            🚧 Step-02 Complete - Room Created!
          </h3>
          <p className="text-sm text-yellow-700 mb-3">
            The REST API is working! You successfully created/joined a room.
          </p>
          <p className="text-sm text-gray-600">
            <strong>Coming in Step-03:</strong>
          </p>
          <ul className="text-sm text-gray-600 list-disc list-inside ml-2 mt-1">
            <li>Real-time player updates (WebSocket)</li>
            <li>Team selection (Blue/Red)</li>
            <li>Role assignment (Spymaster/Operative)</li>
            <li>Game start functionality</li>
            <li>Live player connection status</li>
          </ul>
        </div>

        {/* Action Button */}
        <div className="mt-6">
          <button
            onClick={() => window.location.href = '/'}
            className="w-full bg-gray-600 hover:bg-gray-700 text-white font-semibold py-3 px-6 rounded-lg transition duration-200"
          >
            ← Back to Home
          </button>
        </div>
      </div>
    </div>
  );
}
