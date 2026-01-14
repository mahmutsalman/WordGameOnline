import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createRoom, joinRoom } from '../services/api';
import { useRoomStore } from '../store/roomStore';

/**
 * HomePage component for creating or joining game rooms.
 * Provides two forms: one for creating a new room, one for joining an existing room.
 */
export default function HomePage() {
  const navigate = useNavigate();
  const { setRoom, setCurrentPlayer } = useRoomStore();

  // Create Room State
  const [createUsername, setCreateUsername] = useState('');
  const [createError, setCreateError] = useState('');
  const [createLoading, setCreateLoading] = useState(false);

  // Join Room State
  const [joinUsername, setJoinUsername] = useState('');
  const [roomCode, setRoomCode] = useState('');
  const [joinError, setJoinError] = useState('');
  const [joinLoading, setJoinLoading] = useState(false);

  /**
   * Handles room creation.
   * Validates username, creates room via API, updates store, and navigates to room page.
   */
  const handleCreateRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreateError('');

    // Validation
    if (!createUsername.trim()) {
      setCreateError('Username is required');
      return;
    }

    setCreateLoading(true);

    try {
      const room = await createRoom({ username: createUsername.trim() });

      // Store room and player data from REST API
      setRoom(room);
      const adminPlayer = room.players.find((p) => p.admin);
      if (adminPlayer) {
        setCurrentPlayer(adminPlayer);
        // Save playerId for WebSocket session identification
        sessionStorage.setItem('playerId', adminPlayer.id);
      }

      // Save username and roomId for WebSocket
      sessionStorage.setItem('username', createUsername.trim());
      sessionStorage.setItem('roomId', room.roomId);
      sessionStorage.setItem('isCreator', 'true');

      // Navigate to room page
      navigate(`/room/${room.roomId}`);
    } catch (error) {
      console.error('Create room error:', error);
      setCreateError('Error creating room. Please try again.');
    } finally {
      setCreateLoading(false);
    }
  };

  /**
   * Handles joining an existing room.
   * Validates inputs, joins room via API, updates store, and navigates to room page.
   */
  const handleJoinRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    setJoinError('');

    // Validation
    if (!joinUsername.trim()) {
      setJoinError('Username is required');
      return;
    }

    if (!roomCode.trim()) {
      setJoinError('Room code is required');
      return;
    }

    setJoinLoading(true);

    try {
      const roomIdUpper = roomCode.trim().toUpperCase();

      // Save username and roomId for WebSocket join
      sessionStorage.setItem('username', joinUsername.trim());
      sessionStorage.setItem('roomId', roomIdUpper);
      sessionStorage.setItem('isCreator', 'false');

      // Navigate to room page - WebSocket will handle the join
      navigate(`/room/${roomIdUpper}`);
    } catch (error) {
      console.error('Join room error:', error);
      setJoinError('Error joining room. Please check the room code and try again.');
    } finally {
      setJoinLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
      <div className="max-w-4xl w-full">
        {/* Header */}
        <div className="text-center mb-12">
          <h1 className="text-5xl font-bold text-gray-800 mb-2">Codenames</h1>
          <p className="text-gray-600">Create or join a game room to start playing</p>
        </div>

        <div className="grid md:grid-cols-2 gap-8">
          {/* Create Room Card */}
          <div className="bg-white rounded-lg shadow-lg p-8">
            <h2 className="text-2xl font-semibold text-gray-800 mb-6">
              Create Room
            </h2>

            <form onSubmit={handleCreateRoom} className="space-y-4">
              <div>
                <label
                  htmlFor="create-username"
                  className="block text-sm font-medium text-gray-700 mb-2"
                >
                  Your Username
                </label>
                <input
                  id="create-username"
                  type="text"
                  placeholder="Enter your username"
                  value={createUsername}
                  onChange={(e) => setCreateUsername(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition"
                  disabled={createLoading}
                  maxLength={20}
                />
              </div>

              {createError && (
                <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
                  {createError}
                </div>
              )}

              <button
                type="submit"
                disabled={createLoading}
                className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-blue-300 text-white font-semibold py-3 px-6 rounded-lg transition duration-200 transform hover:scale-105 active:scale-95"
              >
                {createLoading ? 'Creating...' : 'Create Room'}
              </button>
            </form>
          </div>

          {/* Join Room Card */}
          <div className="bg-white rounded-lg shadow-lg p-8">
            <h2 className="text-2xl font-semibold text-gray-800 mb-6">
              Join Room
            </h2>

            <form onSubmit={handleJoinRoom} className="space-y-4">
              <div>
                <label
                  htmlFor="join-username"
                  className="block text-sm font-medium text-gray-700 mb-2"
                >
                  Your Username
                </label>
                <input
                  id="join-username"
                  type="text"
                  placeholder="Enter your username"
                  value={joinUsername}
                  onChange={(e) => setJoinUsername(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent outline-none transition"
                  disabled={joinLoading}
                  maxLength={20}
                />
              </div>

              <div>
                <label
                  htmlFor="room-code"
                  className="block text-sm font-medium text-gray-700 mb-2"
                >
                  Room Code
                </label>
                <input
                  id="room-code"
                  type="text"
                  placeholder="Enter room code (e.g., ABC12-DEF34)"
                  value={roomCode}
                  onChange={(e) =>
                    setRoomCode(e.target.value.toUpperCase())
                  }
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent outline-none transition font-mono"
                  disabled={joinLoading}
                />
              </div>

              {joinError && (
                <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
                  {joinError}
                </div>
              )}

              <button
                type="submit"
                disabled={joinLoading}
                className="w-full bg-green-600 hover:bg-green-700 disabled:bg-green-300 text-white font-semibold py-3 px-6 rounded-lg transition duration-200 transform hover:scale-105 active:scale-95"
              >
                {joinLoading ? 'Joining...' : 'Join Room'}
              </button>
            </form>
          </div>
        </div>

        {/* Footer */}
        <div className="text-center mt-8 text-gray-600 text-sm">
          <p>Step 2: Room REST API Implementation</p>
        </div>
      </div>
    </div>
  );
}
