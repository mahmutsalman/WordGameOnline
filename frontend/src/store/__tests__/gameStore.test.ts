import { describe, it, expect, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useGameStore } from '../gameStore';
import type { GameState, Card, Clue, TurnHistory } from '../../types';

describe('useGameStore', () => {
  // Helper to create a mock game state
  const createMockGameState = (): GameState => ({
    board: Array.from({ length: 25 }, (_, i) => ({
      word: `WORD${i}`,
      color: i < 9 ? 'BLUE' : i < 17 ? 'RED' : i < 24 ? 'NEUTRAL' : 'ASSASSIN',
      revealed: false,
      selectedBy: undefined,
    })),
    currentTeam: 'BLUE',
    currentClue: null,
    guessesRemaining: 0,
    blueRemaining: 9,
    redRemaining: 8,
    phase: 'CLUE',
    winner: null,
    turnHistory: [],
  });

  beforeEach(() => {
    // Reset store state before each test
    const { result } = renderHook(() => useGameStore());
    act(() => {
      result.current.reset();
    });
  });

  it('should initialize with null gameState', () => {
    // Arrange & Act
    const { result } = renderHook(() => useGameStore());

    // Assert
    expect(result.current.gameState).toBeNull();
  });

  it('should set game state', () => {
    // Arrange
    const { result } = renderHook(() => useGameStore());
    const mockState = createMockGameState();

    // Act
    act(() => {
      result.current.setGameState(mockState);
    });

    // Assert
    expect(result.current.gameState).toEqual(mockState);
    expect(result.current.gameState?.board).toHaveLength(25);
    expect(result.current.gameState?.currentTeam).toBe('BLUE');
  });

  it('should update card at index', () => {
    // Arrange
    const { result } = renderHook(() => useGameStore());
    const mockState = createMockGameState();

    // Act
    act(() => {
      result.current.setGameState(mockState);
      result.current.updateCard(0, { revealed: true, selectedBy: 'player-1' });
    });

    // Assert
    expect(result.current.gameState?.board[0].revealed).toBe(true);
    expect(result.current.gameState?.board[0].selectedBy).toBe('player-1');
    expect(result.current.gameState?.board[0].word).toBe('WORD0');
  });

  it('should not mutate original board when updating card', () => {
    // Arrange
    const { result } = renderHook(() => useGameStore());
    const mockState = createMockGameState();
    const originalBoard = [...mockState.board];

    // Act
    act(() => {
      result.current.setGameState(mockState);
    });

    const boardBeforeUpdate = result.current.gameState?.board;

    act(() => {
      result.current.updateCard(5, { revealed: true });
    });

    // Assert
    expect(originalBoard[5].revealed).toBe(false);
    expect(result.current.gameState?.board).not.toBe(boardBeforeUpdate);
  });

  it('should set current clue', () => {
    // Arrange
    const { result } = renderHook(() => useGameStore());
    const mockState = createMockGameState();
    const clue: Clue = { word: 'OCEAN', number: 3, team: 'BLUE' };

    // Act
    act(() => {
      result.current.setGameState(mockState);
      result.current.setCurrentClue(clue);
    });

    // Assert
    expect(result.current.gameState?.currentClue).toEqual(clue);
  });

  it('should clear current clue with null', () => {
    // Arrange
    const { result } = renderHook(() => useGameStore());
    const mockState = createMockGameState();
    mockState.currentClue = { word: 'TEST', number: 2, team: 'BLUE' };

    // Act
    act(() => {
      result.current.setGameState(mockState);
      result.current.setCurrentClue(null);
    });

    // Assert
    expect(result.current.gameState?.currentClue).toBeNull();
  });

  it('should set phase', () => {
    // Arrange
    const { result } = renderHook(() => useGameStore());
    const mockState = createMockGameState();

    // Act
    act(() => {
      result.current.setGameState(mockState);
      result.current.setPhase('GUESS');
    });

    // Assert
    expect(result.current.gameState?.phase).toBe('GUESS');
  });

  it('should set current team', () => {
    // Arrange
    const { result } = renderHook(() => useGameStore());
    const mockState = createMockGameState();

    // Act
    act(() => {
      result.current.setGameState(mockState);
      result.current.setCurrentTeam('RED');
    });

    // Assert
    expect(result.current.gameState?.currentTeam).toBe('RED');
  });

  it('should set guesses remaining', () => {
    // Arrange
    const { result } = renderHook(() => useGameStore());
    const mockState = createMockGameState();

    // Act
    act(() => {
      result.current.setGameState(mockState);
      result.current.setGuessesRemaining(4);
    });

    // Assert
    expect(result.current.gameState?.guessesRemaining).toBe(4);
  });

  it('should set winner', () => {
    // Arrange
    const { result } = renderHook(() => useGameStore());
    const mockState = createMockGameState();

    // Act
    act(() => {
      result.current.setGameState(mockState);
      result.current.setWinner('BLUE');
    });

    // Assert
    expect(result.current.gameState?.winner).toBe('BLUE');
  });

  it('should decrement blue remaining', () => {
    // Arrange
    const { result } = renderHook(() => useGameStore());
    const mockState = createMockGameState();

    // Act
    act(() => {
      result.current.setGameState(mockState);
      result.current.decrementRemaining('BLUE');
    });

    // Assert
    expect(result.current.gameState?.blueRemaining).toBe(8);
  });

  it('should decrement red remaining', () => {
    // Arrange
    const { result } = renderHook(() => useGameStore());
    const mockState = createMockGameState();

    // Act
    act(() => {
      result.current.setGameState(mockState);
      result.current.decrementRemaining('RED');
    });

    // Assert
    expect(result.current.gameState?.redRemaining).toBe(7);
  });

  it('should not decrement below zero', () => {
    // Arrange
    const { result } = renderHook(() => useGameStore());
    const mockState = createMockGameState();
    mockState.blueRemaining = 0;

    // Act
    act(() => {
      result.current.setGameState(mockState);
      result.current.decrementRemaining('BLUE');
    });

    // Assert
    expect(result.current.gameState?.blueRemaining).toBe(0);
  });

  it('should reset to initial state', () => {
    // Arrange
    const { result } = renderHook(() => useGameStore());
    const mockState = createMockGameState();

    // Act
    act(() => {
      result.current.setGameState(mockState);
    });

    expect(result.current.gameState).not.toBeNull();

    act(() => {
      result.current.reset();
    });

    // Assert
    expect(result.current.gameState).toBeNull();
  });

  it('should handle boundary card indices', () => {
    // Arrange
    const { result } = renderHook(() => useGameStore());
    const mockState = createMockGameState();

    // Act
    act(() => {
      result.current.setGameState(mockState);
    });

    // Update first card (index 0)
    act(() => {
      result.current.updateCard(0, { revealed: true });
    });
    expect(result.current.gameState?.board[0].revealed).toBe(true);

    // Update last card (index 24)
    act(() => {
      result.current.updateCard(24, { revealed: true });
    });
    expect(result.current.gameState?.board[24].revealed).toBe(true);

    // Invalid index should not change state
    const stateBefore = result.current.gameState;
    act(() => {
      result.current.updateCard(-1, { revealed: true });
    });
    expect(result.current.gameState?.board).toEqual(stateBefore?.board);

    act(() => {
      result.current.updateCard(25, { revealed: true });
    });
    expect(result.current.gameState?.board).toEqual(stateBefore?.board);
  });

  it('should add turn history', () => {
    // Arrange
    const { result } = renderHook(() => useGameStore());
    const mockState = createMockGameState();
    const turn: TurnHistory = {
      team: 'BLUE',
      clue: { word: 'WATER', number: 2, team: 'BLUE' },
      guessedWords: ['OCEAN', 'RIVER'],
      guessedColors: ['BLUE', 'BLUE'],
    };

    // Act
    act(() => {
      result.current.setGameState(mockState);
      result.current.addTurnHistory(turn);
    });

    // Assert
    expect(result.current.gameState?.turnHistory).toHaveLength(1);
    expect(result.current.gameState?.turnHistory[0]).toEqual(turn);
  });

  it('should handle actions when gameState is null', () => {
    // Arrange
    const { result } = renderHook(() => useGameStore());

    // Act & Assert - should not throw
    act(() => {
      result.current.updateCard(0, { revealed: true });
      result.current.setCurrentClue({ word: 'TEST', number: 1, team: 'BLUE' });
      result.current.setPhase('GUESS');
      result.current.setCurrentTeam('RED');
      result.current.setGuessesRemaining(3);
      result.current.setWinner('BLUE');
      result.current.decrementRemaining('BLUE');
      result.current.addTurnHistory({
        team: 'BLUE',
        clue: { word: 'TEST', number: 1, team: 'BLUE' },
        guessedWords: [],
        guessedColors: [],
      });
    });

    expect(result.current.gameState).toBeNull();
  });

  it('should work with TypeScript types', () => {
    // Arrange & Act
    const { result } = renderHook(() => useGameStore());

    // Assert - TypeScript type checking
    expect(result.current.gameState).toBeNull();
    expect(typeof result.current.setGameState).toBe('function');
    expect(typeof result.current.updateCard).toBe('function');
    expect(typeof result.current.setCurrentClue).toBe('function');
    expect(typeof result.current.setPhase).toBe('function');
    expect(typeof result.current.setCurrentTeam).toBe('function');
    expect(typeof result.current.setGuessesRemaining).toBe('function');
    expect(typeof result.current.setWinner).toBe('function');
    expect(typeof result.current.decrementRemaining).toBe('function');
    expect(typeof result.current.addTurnHistory).toBe('function');
    expect(typeof result.current.reset).toBe('function');
  });
});
