import { create } from 'zustand';
import type { RoomResponse, PlayerResponse } from '../services/api';

/**
 * Room state management store using Zustand.
 * Manages current room and player state across the application.
 */

interface RoomState {
  // State
  room: RoomResponse | null;
  currentPlayer: PlayerResponse | null;

  // Actions
  setRoom: (room: RoomResponse) => void;
  setCurrentPlayer: (player: PlayerResponse) => void;
  updatePlayers: (players: PlayerResponse[]) => void;
  reset: () => void;
}

const initialState = {
  room: null,
  currentPlayer: null,
};

export const useRoomStore = create<RoomState>((set) => ({
  ...initialState,

  /**
   * Sets the current room state.
   * Creates a copy with players array cloned to prevent mutation.
   * @param room - The room data to store
   */
  setRoom: (room: RoomResponse) => {
    set({ room: { ...room, players: [...room.players] } });
  },

  /**
   * Sets the current player state.
   * @param player - The current player data
   */
  setCurrentPlayer: (player: PlayerResponse) => {
    set({ currentPlayer: player });
  },

  /**
   * Updates the players list in the current room.
   * Preserves other room properties like roomId, settings, etc.
   * @param players - The updated players list
   */
  updatePlayers: (players: PlayerResponse[]) => {
    set((state) => ({
      room: state.room
        ? {
            ...state.room,
            players,
          }
        : null,
    }));
  },

  /**
   * Resets the store to initial state.
   * Call this when leaving a room or logging out.
   */
  reset: () => {
    set(initialState);
  },
}));
