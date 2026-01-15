/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import RoomPage from '../RoomPage';
import { useRoomStore } from '../../store/roomStore';
import { useRoomWebSocket } from '../../hooks/useRoomWebSocket';

// Mock dependencies
vi.mock('../../store/roomStore');
vi.mock('../../hooks/useRoomWebSocket');
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ roomId: 'TEST-ROOM' }),
    useNavigate: () => vi.fn(),
  };
});

// Mock sessionStorage
const mockSessionStorage = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
};
Object.defineProperty(window, 'sessionStorage', { value: mockSessionStorage });

describe('RoomPage', () => {
  const mockChangeTeam = vi.fn();

  const defaultStoreState = {
    room: {
      roomId: 'TEST-ROOM',
      players: [],
      settings: { maxPlayers: 8 },
      canStart: false,
      adminId: 'admin-1',
    },
    currentPlayer: null,
    reset: vi.fn(),
  };

  const defaultWebSocketState = {
    connectionStatus: 'CONNECTED' as const,
    error: null,
    changeTeam: mockChangeTeam,
    isConnected: true,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    mockSessionStorage.getItem.mockReturnValue('TestUser');

    vi.mocked(useRoomStore).mockReturnValue(defaultStoreState as any);
    vi.mocked(useRoomWebSocket).mockReturnValue(defaultWebSocketState);
  });

  describe('Rendering', () => {
    it('should render room code', () => {
      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      expect(screen.getByText('TEST-ROOM')).toBeInTheDocument();
    });

    it('should show connection status as Connected', () => {
      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      expect(screen.getByText('Connected')).toBeInTheDocument();
    });

    it('should show Disconnected when not connected', () => {
      vi.mocked(useRoomWebSocket).mockReturnValue({
        ...defaultWebSocketState,
        connectionStatus: 'DISCONNECTED',
        isConnected: false,
      });

      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      expect(screen.getByText('Disconnected')).toBeInTheDocument();
    });

    it('should display Blue Team section', () => {
      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      expect(screen.getByText('Blue Team')).toBeInTheDocument();
    });

    it('should display Red Team section', () => {
      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      expect(screen.getByText('Red Team')).toBeInTheDocument();
    });
  });

  describe('Player Display', () => {
    it('should show players in Blue Team', () => {
      vi.mocked(useRoomStore).mockReturnValue({
        ...defaultStoreState,
        room: {
          ...defaultStoreState.room!,
          players: [
            { id: '1', username: 'BluePlayer', team: 'BLUE', role: 'OPERATIVE', connected: true, admin: false },
          ],
        },
      } as any);

      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      expect(screen.getByText('BluePlayer')).toBeInTheDocument();
    });

    it('should show players in Red Team', () => {
      vi.mocked(useRoomStore).mockReturnValue({
        ...defaultStoreState,
        room: {
          ...defaultStoreState.room!,
          players: [
            { id: '2', username: 'RedPlayer', team: 'RED', role: 'SPYMASTER', connected: true, admin: false },
          ],
        },
      } as any);

      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      expect(screen.getByText('RedPlayer')).toBeInTheDocument();
    });

    it('should show disconnected players with visual indicator', () => {
      vi.mocked(useRoomStore).mockReturnValue({
        ...defaultStoreState,
        room: {
          ...defaultStoreState.room!,
          players: [
            { id: '3', username: 'OfflinePlayer', team: 'BLUE', role: 'OPERATIVE', connected: false, admin: false },
          ],
        },
      } as any);

      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      expect(screen.getByText('OfflinePlayer')).toBeInTheDocument();
      expect(screen.getByText('Disconnected')).toBeInTheDocument();
    });
  });

  describe('Team Selection', () => {
    beforeEach(() => {
      vi.mocked(useRoomStore).mockReturnValue({
        ...defaultStoreState,
        currentPlayer: { id: 'p1', username: 'TestUser', team: null, role: 'SPECTATOR', connected: true, admin: false },
      } as any);
    });

    it('should show team selection buttons when player is connected', () => {
      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      // There are 2 Operative and 2 Spymaster buttons (Blue and Red teams)
      expect(screen.getAllByText('Operative')).toHaveLength(2);
      expect(screen.getAllByText('Spymaster')).toHaveLength(2);
    });

    it('should call changeTeam when clicking Blue Operative', () => {
      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      const buttons = screen.getAllByText('Operative');
      fireEvent.click(buttons[0]); // First Operative button is Blue

      expect(mockChangeTeam).toHaveBeenCalledWith('BLUE', 'OPERATIVE');
    });

    it('should call changeTeam when clicking Red Spymaster', () => {
      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      const buttons = screen.getAllByText('Spymaster');
      fireEvent.click(buttons[1]); // Second Spymaster button is Red

      expect(mockChangeTeam).toHaveBeenCalledWith('RED', 'SPYMASTER');
    });

    it('should call changeTeam with null team for Spectator', () => {
      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      fireEvent.click(screen.getByText('Become Spectator'));

      expect(mockChangeTeam).toHaveBeenCalledWith(null, 'SPECTATOR');
    });
  });

  describe('Start Game', () => {
    it('should show Start Game button for admin', () => {
      vi.mocked(useRoomStore).mockReturnValue({
        ...defaultStoreState,
        currentPlayer: { id: 'admin-1', username: 'Admin', team: 'BLUE', role: 'SPYMASTER', connected: true, admin: true },
        room: {
          ...defaultStoreState.room!,
          canStart: true,
        },
      } as any);

      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      expect(screen.getByText('Start Game')).toBeInTheDocument();
    });

    it('should NOT show Start Game button for non-admin', () => {
      vi.mocked(useRoomStore).mockReturnValue({
        ...defaultStoreState,
        currentPlayer: { id: 'p1', username: 'Player', team: 'BLUE', role: 'OPERATIVE', connected: true, admin: false },
      } as any);

      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      expect(screen.queryByText('Start Game')).not.toBeInTheDocument();
    });

    it('should show "Waiting for Teams..." when canStart is false', () => {
      vi.mocked(useRoomStore).mockReturnValue({
        ...defaultStoreState,
        currentPlayer: { id: 'admin-1', username: 'Admin', team: 'BLUE', role: 'SPYMASTER', connected: true, admin: true },
        room: {
          ...defaultStoreState.room!,
          canStart: false,
        },
      } as any);

      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      expect(screen.getByText('Waiting for Teams...')).toBeInTheDocument();
    });

    it('should disable Start Game button when canStart is false', () => {
      vi.mocked(useRoomStore).mockReturnValue({
        ...defaultStoreState,
        currentPlayer: { id: 'admin-1', username: 'Admin', team: 'BLUE', role: 'SPYMASTER', connected: true, admin: true },
        room: {
          ...defaultStoreState.room!,
          canStart: false,
        },
      } as any);

      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      const button = screen.getByText('Waiting for Teams...');
      expect(button).toBeDisabled();
    });

    it('should enable Start Game button when canStart is true', () => {
      vi.mocked(useRoomStore).mockReturnValue({
        ...defaultStoreState,
        currentPlayer: { id: 'admin-1', username: 'Admin', team: 'BLUE', role: 'SPYMASTER', connected: true, admin: true },
        room: {
          ...defaultStoreState.room!,
          canStart: true,
        },
      } as any);

      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      const button = screen.getByText('Start Game');
      expect(button).not.toBeDisabled();
    });
  });

  describe('Error Handling', () => {
    it('should display error message when error exists', () => {
      vi.mocked(useRoomWebSocket).mockReturnValue({
        ...defaultWebSocketState,
        error: 'Connection failed',
      });

      render(
        <BrowserRouter>
          <RoomPage />
        </BrowserRouter>
      );

      expect(screen.getByText('Connection failed')).toBeInTheDocument();
    });
  });
});
