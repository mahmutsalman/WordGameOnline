import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useGameStore } from '../store/gameStore';
import { useRoomStore } from '../store/roomStore';
import { useRoomWebSocket } from '../hooks/useRoomWebSocket';
import { websocketService } from '../services/websocketService';
import type { Card, CardColor } from '../types';

/**
 * Color mapping for cards based on their type.
 * Uses Tailwind theme colors defined in tailwind.config.js
 */
const colorMap: Record<CardColor, string> = {
  BLUE: 'bg-team-blue',
  RED: 'bg-team-red',
  NEUTRAL: 'bg-card-neutral',
  ASSASSIN: 'bg-card-assassin',
};

/**
 * Returns the appropriate CSS classes for a card based on its state and viewer role.
 * @param card - The card to style
 * @param isSpymaster - Whether the viewer is a spymaster
 */
function getCardClasses(card: Card, isSpymaster: boolean, isClickable: boolean): string {
  const baseClasses = `flex items-center justify-center p-2 rounded-lg shadow-md transition-all duration-200 min-h-[80px] font-semibold text-sm md:text-base ${
    isClickable ? 'cursor-pointer hover:scale-105' : 'cursor-default'
  }`;

  // Revealed cards always show their true color
  if (card.revealed) {
    const bgColor = card.color ? colorMap[card.color] : 'bg-card-face';
    const textColor =
      card.color === 'ASSASSIN' || card.color === 'BLUE' || card.color === 'RED'
        ? 'text-white'
        : 'text-gray-800';
    return `${baseClasses} ${bgColor} ${textColor} opacity-90`;
  }

  // Spymaster sees all colors (semi-transparent to indicate unrevealed)
  if (isSpymaster && card.color) {
    const bgColor = colorMap[card.color];
    const textColor =
      card.color === 'ASSASSIN' || card.color === 'BLUE' || card.color === 'RED'
        ? 'text-white'
        : 'text-gray-800';
    return `${baseClasses} ${bgColor} ${textColor} opacity-60 hover:opacity-75`;
  }

  // Operative sees neutral face-down cards
  return `${baseClasses} bg-card-face text-gray-800 hover:bg-gray-100`;
}

/**
 * GamePage - Main game board display with 5x5 card grid.
 * Shows the Codenames board after game starts.
 * Spymasters see all card colors, operatives only see revealed cards.
 */
export default function GamePage() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const { gameState } = useGameStore();
  const { currentPlayer } = useRoomStore();
  const username = sessionStorage.getItem('username') || 'Guest';
  const { connectionStatus } = useRoomWebSocket(roomId!, username);
  const [clueWord, setClueWord] = useState('');
  const [clueNumber, setClueNumber] = useState(1);
  const [clueError, setClueError] = useState<string | null>(null);

  // Redirect if no game state
  if (!gameState) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center">
        <div className="bg-white rounded-lg shadow-lg p-8 text-center">
          <h2 className="text-xl font-semibold text-gray-800 mb-4">
            No Game in Progress
          </h2>
          <p className="text-gray-600 mb-6">
            The game hasn&apos;t started yet or the game state was lost.
          </p>
          <button
            onClick={() => navigate(`/room/${roomId}`)}
            className="bg-blue-500 hover:bg-blue-600 text-white font-semibold py-2 px-6 rounded-lg transition duration-200"
          >
            Return to Lobby
          </button>
        </div>
      </div>
    );
  }

  // Calculate remaining cards
  const { blueRemaining, redRemaining, currentTeam, phase, currentClue, guessesRemaining, winner } =
    gameState;

  const isSpymaster = currentPlayer?.role === 'SPYMASTER';
  const isMyTurn = currentPlayer?.team === currentTeam;
  const canSubmitClue = !winner && phase === 'CLUE' && isMyTurn && isSpymaster;
  const canGuess =
    !winner && phase === 'GUESS' && isMyTurn && currentPlayer?.role === 'OPERATIVE';

  const handleSubmitClue = (e: React.FormEvent) => {
    e.preventDefault();
    setClueError(null);

    const word = clueWord.trim();
    if (!word) {
      setClueError('Clue word is required');
      return;
    }

    const number = Number.isFinite(clueNumber) ? clueNumber : 1;
    if (number < 0 || number > 9) {
      setClueError('Clue number must be between 0 and 9');
      return;
    }

    websocketService.submitClue(roomId!, word, number);
    setClueWord('');
    setClueNumber(1);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-800 to-gray-900 p-4">
      <div className="max-w-5xl mx-auto">
        {/* Header */}
        <div className="bg-white rounded-lg shadow-lg p-4 mb-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => navigate(`/room/${roomId}`)}
                className="text-gray-600 hover:text-gray-800 transition duration-200"
                aria-label="Back to lobby"
              >
                ← Back
              </button>
              <div>
                <span className="text-gray-600 text-sm">Room:</span>
                <span className="font-mono font-bold text-lg ml-2">{roomId}</span>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <div
                className={`w-3 h-3 rounded-full ${connectionStatus === 'CONNECTED' ? 'bg-green-500' : 'bg-gray-400'}`}
              ></div>
              <span className="text-sm text-gray-600">
                {connectionStatus === 'CONNECTED' ? 'Connected' : connectionStatus}
              </span>
            </div>
          </div>
        </div>

        {/* Game Info Bar */}
        <div className="bg-white rounded-lg shadow-lg p-4 mb-4">
          <div className="flex items-center justify-between flex-wrap gap-4">
            {/* Current Team Indicator */}
            <div className="flex items-center gap-2">
              <span className="text-gray-600">Turn:</span>
              <div
                className={`px-4 py-2 rounded-lg font-bold ${
                  currentTeam === 'BLUE'
                    ? 'bg-team-blue text-white'
                    : 'bg-team-red text-white'
                }`}
              >
                {currentTeam}
                {isMyTurn && <span className="ml-2">(Your Team)</span>}
              </div>
            </div>

            {/* Remaining Counts */}
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 rounded bg-team-blue"></div>
                <span className="font-semibold" data-testid="blue-remaining">
                  Blue: {blueRemaining}
                </span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 rounded bg-team-red"></div>
                <span className="font-semibold" data-testid="red-remaining">
                  Red: {redRemaining}
                </span>
              </div>
            </div>

            {/* Phase Indicator */}
            <div className="flex items-center gap-2">
              <span className="text-gray-600">Phase:</span>
              <span
                className={`px-3 py-1 rounded-full text-sm font-semibold ${
                  phase === 'CLUE'
                    ? 'bg-purple-100 text-purple-700'
                    : phase === 'GUESS'
                      ? 'bg-green-100 text-green-700'
                      : phase === 'GAME_OVER'
                        ? 'bg-gray-100 text-gray-700'
                        : 'bg-blue-100 text-blue-700'
                }`}
              >
                {phase}
              </span>
            </div>

            {/* Role Indicator */}
            {currentPlayer && (
              <div className="flex items-center gap-2">
                <span className="text-gray-600">Role:</span>
                <span
                  className={`px-3 py-1 rounded-full text-sm font-semibold ${
                    isSpymaster
                      ? 'bg-purple-600 text-white'
                      : 'bg-blue-600 text-white'
                  }`}
                >
                  {currentPlayer.role}
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Clue Display - Show when in GUESS phase */}
        {phase === 'GUESS' && currentClue && (
          <div className="bg-white rounded-lg shadow-lg p-4 mb-4" data-testid="clue-display">
            <div className="flex items-center justify-center gap-4">
              <span className="text-gray-600">Clue:</span>
              <span className="text-2xl font-bold text-gray-800">
                &quot;{currentClue.word}&quot;
              </span>
              <span className="text-xl font-semibold text-gray-600">
                ({currentClue.number})
              </span>
              <span className="text-gray-500 ml-4">
                Guesses remaining: {guessesRemaining}
              </span>
            </div>
          </div>
        )}

        {/* Clue Input - Show for spymaster when it's their team's CLUE phase */}
        {canSubmitClue && (
          <div className="bg-white rounded-lg shadow-lg p-4 mb-4" data-testid="clue-input">
            <form onSubmit={handleSubmitClue} className="flex flex-wrap items-end gap-4 justify-center">
              <div className="flex flex-col">
                <label className="text-sm text-gray-600 mb-1" htmlFor="clue-word">
                  Clue word
                </label>
                <input
                  id="clue-word"
                  type="text"
                  value={clueWord}
                  onChange={(e) => setClueWord(e.target.value)}
                  className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent outline-none"
                  maxLength={30}
                />
              </div>
              <div className="flex flex-col">
                <label className="text-sm text-gray-600 mb-1" htmlFor="clue-number">
                  Number
                </label>
                <input
                  id="clue-number"
                  type="number"
                  min={0}
                  max={9}
                  value={clueNumber}
                  onChange={(e) => setClueNumber(Number(e.target.value))}
                  className="w-24 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent outline-none"
                />
              </div>
              <button
                type="submit"
                disabled={connectionStatus !== 'CONNECTED'}
                className="bg-purple-600 hover:bg-purple-700 disabled:bg-purple-300 text-white font-semibold py-2 px-6 rounded-lg transition duration-200"
              >
                Submit Clue
              </button>
              {clueError && (
                <div className="w-full text-center text-sm text-red-600">
                  {clueError}
                </div>
              )}
            </form>
          </div>
        )}

        {/* Winner Display */}
        {winner && (
          <div
            className={`rounded-lg shadow-lg p-6 mb-4 text-center ${
              winner === 'BLUE' ? 'bg-team-blue' : 'bg-team-red'
            }`}
            data-testid="winner-display"
          >
            <h2 className="text-3xl font-bold text-white">
              {winner} Team Wins!
            </h2>
          </div>
        )}

        {/* Game Board - 5x5 Grid */}
        <div className="bg-game-surface rounded-lg shadow-lg p-4" data-testid="game-board">
          <div className="grid grid-cols-5 gap-3 md:gap-4">
            {gameState.board.map((card, index) => (
              <div
                key={index}
                className={getCardClasses(card, isSpymaster, canGuess && !card.revealed)}
                data-testid={`card-${index}`}
                data-color={card.color}
                data-revealed={card.revealed}
                onClick={() => {
                  if (!canGuess || card.revealed) {
                    return;
                  }
                  websocketService.makeGuess(roomId!, index);
                }}
              >
                <span className="text-center break-words uppercase">
                  {card.word}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Game Controls Footer */}
        <div className="mt-4 flex justify-center">
          <button
            onClick={() => navigate(`/room/${roomId}`)}
            className="bg-gray-600 hover:bg-gray-700 text-white font-semibold py-3 px-6 rounded-lg transition duration-200"
          >
            ← Back to Lobby
          </button>
        </div>
      </div>
    </div>
  );
}
