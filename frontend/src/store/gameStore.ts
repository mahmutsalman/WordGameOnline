import { create } from 'zustand';
import type { GameState, Card, Clue, GamePhase, Team, TurnHistory } from '../types';

/**
 * Game state management store using Zustand.
 * Manages the current game state for the Codenames game.
 */

interface GameStore {
  // State
  gameState: GameState | null;

  // Actions
  setGameState: (state: GameState) => void;
  updateCard: (index: number, updates: Partial<Card>) => void;
  setCurrentClue: (clue: Clue | null) => void;
  setPhase: (phase: GamePhase) => void;
  setCurrentTeam: (team: Team) => void;
  setGuessesRemaining: (count: number) => void;
  setWinner: (team: Team | null) => void;
  decrementRemaining: (team: Team) => void;
  addTurnHistory: (turn: TurnHistory) => void;
  reset: () => void;
}

const initialState = {
  gameState: null,
};

export const useGameStore = create<GameStore>((set) => ({
  ...initialState,

  /**
   * Sets the entire game state.
   * Clones the board array to prevent mutation.
   * @param state - The game state to store
   */
  setGameState: (state: GameState) => {
    set({
      gameState: {
        ...state,
        board: [...state.board],
        turnHistory: [...state.turnHistory],
      },
    });
  },

  /**
   * Updates a specific card on the board by index.
   * Creates a new board array to maintain immutability.
   * @param index - The card index (0-24)
   * @param updates - Partial card updates to apply
   */
  updateCard: (index: number, updates: Partial<Card>) => {
    set((state) => {
      if (!state.gameState) return state;
      if (index < 0 || index >= state.gameState.board.length) return state;

      const newBoard = [...state.gameState.board];
      newBoard[index] = { ...newBoard[index], ...updates };

      return {
        gameState: {
          ...state.gameState,
          board: newBoard,
        },
      };
    });
  },

  /**
   * Sets the current clue.
   * @param clue - The clue to set, or null to clear
   */
  setCurrentClue: (clue: Clue | null) => {
    set((state) => ({
      gameState: state.gameState
        ? { ...state.gameState, currentClue: clue }
        : null,
    }));
  },

  /**
   * Sets the game phase.
   * @param phase - The new game phase
   */
  setPhase: (phase: GamePhase) => {
    set((state) => ({
      gameState: state.gameState
        ? { ...state.gameState, phase }
        : null,
    }));
  },

  /**
   * Sets the current team.
   * @param team - The team whose turn it is
   */
  setCurrentTeam: (team: Team) => {
    set((state) => ({
      gameState: state.gameState
        ? { ...state.gameState, currentTeam: team }
        : null,
    }));
  },

  /**
   * Sets the number of guesses remaining.
   * @param count - The number of guesses left
   */
  setGuessesRemaining: (count: number) => {
    set((state) => ({
      gameState: state.gameState
        ? { ...state.gameState, guessesRemaining: count }
        : null,
    }));
  },

  /**
   * Sets the winner of the game.
   * @param team - The winning team, or null if no winner yet
   */
  setWinner: (team: Team | null) => {
    set((state) => ({
      gameState: state.gameState
        ? { ...state.gameState, winner: team }
        : null,
    }));
  },

  /**
   * Decrements the remaining count for a team.
   * @param team - The team to decrement
   */
  decrementRemaining: (team: Team) => {
    set((state) => {
      if (!state.gameState) return state;

      if (team === 'BLUE') {
        return {
          gameState: {
            ...state.gameState,
            blueRemaining: Math.max(0, state.gameState.blueRemaining - 1),
          },
        };
      } else {
        return {
          gameState: {
            ...state.gameState,
            redRemaining: Math.max(0, state.gameState.redRemaining - 1),
          },
        };
      }
    });
  },

  /**
   * Adds a turn to the history.
   * @param turn - The turn history entry to add
   */
  addTurnHistory: (turn: TurnHistory) => {
    set((state) => ({
      gameState: state.gameState
        ? {
            ...state.gameState,
            turnHistory: [...state.gameState.turnHistory, turn],
          }
        : null,
    }));
  },

  /**
   * Resets the store to initial state.
   * Call this when leaving the game or starting a new game.
   */
  reset: () => {
    set(initialState);
  },
}));
