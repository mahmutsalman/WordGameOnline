/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import GamePage from '../GamePage';
import { useGameStore } from '../../store/gameStore';
import { useRoomStore } from '../../store/roomStore';
import type { GameState, Card } from '../../types';

// Mock dependencies
vi.mock('../../store/gameStore');
vi.mock('../../store/roomStore');
vi.mock('../../hooks/useRoomWebSocket', () => ({
  useRoomWebSocket: () => ({
    connectionStatus: 'CONNECTED',
    error: null,
    changeTeam: vi.fn(),
    isConnected: true,
  }),
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ roomId: 'TEST-ROOM' }),
    useNavigate: () => mockNavigate,
  };
});

describe('GamePage', () => {
  // Generate a sample 25-card board
  const generateBoard = (options?: {
    allRevealed?: boolean;
    withColors?: boolean;
  }): Card[] => {
    const colors: (Card['color'])[] = [
      'BLUE', 'BLUE', 'BLUE', 'BLUE', 'BLUE', 'BLUE', 'BLUE', 'BLUE', 'BLUE',
      'RED', 'RED', 'RED', 'RED', 'RED', 'RED', 'RED', 'RED',
      'NEUTRAL', 'NEUTRAL', 'NEUTRAL', 'NEUTRAL', 'NEUTRAL', 'NEUTRAL', 'NEUTRAL',
      'ASSASSIN',
    ];

    return Array.from({ length: 25 }, (_, i) => ({
      word: `WORD${i + 1}`,
      color: options?.withColors !== false ? colors[i] : null,
      revealed: options?.allRevealed ?? false,
    }));
  };

  const defaultGameState: GameState = {
    board: generateBoard(),
    currentTeam: 'BLUE',
    phase: 'CLUE',
    currentClue: null,
    guessesRemaining: 0,
    blueRemaining: 9,
    redRemaining: 8,
    winner: null,
    turnHistory: [],
  };

  const defaultRoomState = {
    room: {
      roomId: 'TEST-ROOM',
      players: [],
      settings: { wordPack: 'standard', timerSeconds: null },
      canStart: false,
      adminId: 'admin-1',
    },
    currentPlayer: {
      id: 'player-1',
      username: 'TestPlayer',
      team: 'BLUE' as const,
      role: 'OPERATIVE' as const,
      connected: true,
      admin: false,
    },
    reset: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(useGameStore).mockReturnValue({
      gameState: defaultGameState,
    } as any);

    vi.mocked(useRoomStore).mockReturnValue(defaultRoomState as any);
  });

  describe('Board Rendering', () => {
    it('should render game board with 25 cards', () => {
      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      const gameBoard = screen.getByTestId('game-board');
      expect(gameBoard).toBeInTheDocument();

      // Check all 25 cards are rendered
      for (let i = 0; i < 25; i++) {
        expect(screen.getByTestId(`card-${i}`)).toBeInTheDocument();
      }
    });

    it('should display all words on cards', () => {
      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      // Check some words are visible
      expect(screen.getByText('WORD1')).toBeInTheDocument();
      expect(screen.getByText('WORD10')).toBeInTheDocument();
      expect(screen.getByText('WORD25')).toBeInTheDocument();
    });

    it('should display words in uppercase', () => {
      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      const card = screen.getByTestId('card-0');
      expect(card).toHaveTextContent('WORD1');
    });
  });

  describe('Game Info Display', () => {
    it('should show correct team indicator', () => {
      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      expect(screen.getByText('BLUE')).toBeInTheDocument();
    });

    it('should show remaining counts', () => {
      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      expect(screen.getByTestId('blue-remaining')).toHaveTextContent('Blue: 9');
      expect(screen.getByTestId('red-remaining')).toHaveTextContent('Red: 8');
    });

    it('should show phase indicator', () => {
      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      expect(screen.getByText('CLUE')).toBeInTheDocument();
    });

    it('should show room code in header', () => {
      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      expect(screen.getByText('TEST-ROOM')).toBeInTheDocument();
    });

    it('should show connection status', () => {
      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      expect(screen.getByText('Connected')).toBeInTheDocument();
    });
  });

  describe('Spymaster View', () => {
    it('should show colors to spymaster', () => {
      vi.mocked(useRoomStore).mockReturnValue({
        ...defaultRoomState,
        currentPlayer: {
          ...defaultRoomState.currentPlayer,
          role: 'SPYMASTER',
        },
      } as any);

      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      // Spymasters should see all card colors - check that cards have color data
      const blueCard = screen.getByTestId('card-0');
      expect(blueCard).toHaveAttribute('data-color', 'BLUE');
    });

    it('should display SPYMASTER role badge', () => {
      vi.mocked(useRoomStore).mockReturnValue({
        ...defaultRoomState,
        currentPlayer: {
          ...defaultRoomState.currentPlayer,
          role: 'SPYMASTER',
        },
      } as any);

      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      expect(screen.getByText('SPYMASTER')).toBeInTheDocument();
    });
  });

  describe('Operative View', () => {
    it('should hide colors from operative for unrevealed cards', () => {
      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      // Check that an unrevealed card shows as face-down
      const card = screen.getByTestId('card-0');
      expect(card).toHaveAttribute('data-revealed', 'false');
    });

    it('should display OPERATIVE role badge', () => {
      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      expect(screen.getByText('OPERATIVE')).toBeInTheDocument();
    });
  });

  describe('Revealed Cards', () => {
    it('should show revealed card colors to all players', () => {
      const boardWithRevealed = generateBoard();
      boardWithRevealed[0].revealed = true; // First card revealed
      boardWithRevealed[9].revealed = true; // A red card revealed

      vi.mocked(useGameStore).mockReturnValue({
        gameState: {
          ...defaultGameState,
          board: boardWithRevealed,
        },
      } as any);

      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      const revealedCard = screen.getByTestId('card-0');
      expect(revealedCard).toHaveAttribute('data-revealed', 'true');
      expect(revealedCard).toHaveAttribute('data-color', 'BLUE');
    });
  });

  describe('Clue Display', () => {
    it('should display current clue when in GUESS phase', () => {
      vi.mocked(useGameStore).mockReturnValue({
        gameState: {
          ...defaultGameState,
          phase: 'GUESS',
          currentClue: { word: 'OCEAN', number: 3, team: 'BLUE' },
          guessesRemaining: 4,
        },
      } as any);

      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      const clueDisplay = screen.getByTestId('clue-display');
      expect(clueDisplay).toBeInTheDocument();
      expect(screen.getByText('"OCEAN"')).toBeInTheDocument();
      expect(screen.getByText('(3)')).toBeInTheDocument();
      expect(screen.getByText('Guesses remaining: 4')).toBeInTheDocument();
    });

    it('should not display clue in CLUE phase', () => {
      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      expect(screen.queryByTestId('clue-display')).not.toBeInTheDocument();
    });
  });

  describe('Winner Display', () => {
    it('should display winner when game is over', () => {
      vi.mocked(useGameStore).mockReturnValue({
        gameState: {
          ...defaultGameState,
          phase: 'GAME_OVER',
          winner: 'BLUE',
        },
      } as any);

      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      const winnerDisplay = screen.getByTestId('winner-display');
      expect(winnerDisplay).toBeInTheDocument();
      expect(screen.getByText('BLUE Team Wins!')).toBeInTheDocument();
    });

    it('should not display winner when game is in progress', () => {
      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      expect(screen.queryByTestId('winner-display')).not.toBeInTheDocument();
    });
  });

  describe('Navigation', () => {
    it('should navigate back to lobby when clicking back button', () => {
      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      const backButton = screen.getByText('← Back to Lobby');
      fireEvent.click(backButton);

      expect(mockNavigate).toHaveBeenCalledWith('/room/TEST-ROOM');
    });

    it('should navigate back when clicking header back button', () => {
      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      const headerBackButton = screen.getByLabelText('Back to lobby');
      fireEvent.click(headerBackButton);

      expect(mockNavigate).toHaveBeenCalledWith('/room/TEST-ROOM');
    });
  });

  describe('No Game State', () => {
    it('should redirect if no game state', () => {
      vi.mocked(useGameStore).mockReturnValue({
        gameState: null,
      } as any);

      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      expect(screen.getByText('No Game in Progress')).toBeInTheDocument();
      expect(screen.getByText('Return to Lobby')).toBeInTheDocument();
    });

    it('should show return to lobby button when no game state', () => {
      vi.mocked(useGameStore).mockReturnValue({
        gameState: null,
      } as any);

      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      const returnButton = screen.getByText('Return to Lobby');
      fireEvent.click(returnButton);

      expect(mockNavigate).toHaveBeenCalledWith('/room/TEST-ROOM');
    });
  });

  describe('Turn Indicator', () => {
    it('should show "Your Team" when it is the player team turn', () => {
      vi.mocked(useRoomStore).mockReturnValue({
        ...defaultRoomState,
        currentPlayer: {
          ...defaultRoomState.currentPlayer,
          team: 'BLUE',
        },
      } as any);

      vi.mocked(useGameStore).mockReturnValue({
        gameState: {
          ...defaultGameState,
          currentTeam: 'BLUE',
        },
      } as any);

      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      expect(screen.getByText('(Your Team)')).toBeInTheDocument();
    });

    it('should not show "Your Team" when it is not the player team turn', () => {
      vi.mocked(useRoomStore).mockReturnValue({
        ...defaultRoomState,
        currentPlayer: {
          ...defaultRoomState.currentPlayer,
          team: 'RED',
        },
      } as any);

      vi.mocked(useGameStore).mockReturnValue({
        gameState: {
          ...defaultGameState,
          currentTeam: 'BLUE',
        },
      } as any);

      render(
        <BrowserRouter>
          <GamePage />
        </BrowserRouter>
      );

      expect(screen.queryByText('(Your Team)')).not.toBeInTheDocument();
    });
  });
});
