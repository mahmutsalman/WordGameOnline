import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import HomePage from '../HomePage';
import * as api from '../../services/api';
import type { RoomResponse } from '../../services/api';

// Mock the API module
vi.mock('../../services/api');

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('HomePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderHomePage = () => {
    return render(
      <BrowserRouter>
        <HomePage />
      </BrowserRouter>
    );
  };

  // ========== RENDERING TESTS ==========

  it('should render username input', () => {
    renderHomePage();
    const usernameInputs = screen.getAllByPlaceholderText(/username/i);
    expect(usernameInputs.length).toBeGreaterThan(0);
  });

  it('should render create room button', () => {
    renderHomePage();
    const createButton = screen.getByRole('button', { name: /create room/i });
    expect(createButton).toBeInTheDocument();
  });

  it('should render join room section', () => {
    renderHomePage();
    const joinButton = screen.getByRole('button', { name: /join room/i });
    expect(joinButton).toBeInTheDocument();
  });

  it('should render room code input', () => {
    renderHomePage();
    const roomCodeInput = screen.getByPlaceholderText(/room code/i);
    expect(roomCodeInput).toBeInTheDocument();
  });

  // ========== CREATE ROOM FLOW TESTS ==========

  it('should show error when username is empty on create', async () => {
    renderHomePage();
    const createButton = screen.getByRole('button', { name: /create room/i });

    fireEvent.click(createButton);

    await waitFor(() => {
      const errorMessage = screen.getByText(/username.*required/i);
      expect(errorMessage).toBeInTheDocument();
    });
  });

  it('should call createRoom API with valid username', async () => {
    const mockRoom: RoomResponse = {
      roomId: 'TEST-ROOM',
      players: [],
      settings: { wordPack: 'english', timerSeconds: null },
      canStart: false,
      adminId: 'admin-1',
    };

    vi.mocked(api.createRoom).mockResolvedValueOnce(mockRoom);

    renderHomePage();

    // Fill in create room form
    const usernameInputs = screen.getAllByPlaceholderText(/username/i);
    const createUsernameInput = usernameInputs[0]; // First input is for create
    fireEvent.change(createUsernameInput, { target: { value: 'TestUser' } });

    const createButton = screen.getByRole('button', { name: /create room/i });
    fireEvent.click(createButton);

    await waitFor(() => {
      expect(api.createRoom).toHaveBeenCalledWith({ username: 'TestUser' });
    });
  });

  it('should navigate to room after creation', async () => {
    const mockRoom: RoomResponse = {
      roomId: 'ABC12-DEF34',
      players: [],
      settings: { wordPack: 'english', timerSeconds: null },
      canStart: false,
      adminId: 'admin-1',
    };

    vi.mocked(api.createRoom).mockResolvedValueOnce(mockRoom);

    renderHomePage();

    const usernameInputs = screen.getAllByPlaceholderText(/username/i);
    fireEvent.change(usernameInputs[0], { target: { value: 'TestUser' } });

    const createButton = screen.getByRole('button', { name: /create room/i });
    fireEvent.click(createButton);

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/room/ABC12-DEF34');
    });
  });

  it('should update store after room creation', async () => {
    const mockRoom: RoomResponse = {
      roomId: 'STORE-TEST',
      players: [
        {
          id: 'p1',
          username: 'StoreUser',
          team: null,
          role: 'SPECTATOR',
          connected: true,
          admin: true,
        },
      ],
      settings: { wordPack: 'english', timerSeconds: null },
      canStart: false,
      adminId: 'p1',
    };

    vi.mocked(api.createRoom).mockResolvedValueOnce(mockRoom);

    renderHomePage();

    const usernameInputs = screen.getAllByPlaceholderText(/username/i);
    fireEvent.change(usernameInputs[0], { target: { value: 'StoreUser' } });

    const createButton = screen.getByRole('button', { name: /create room/i });
    fireEvent.click(createButton);

    await waitFor(() => {
      expect(api.createRoom).toHaveBeenCalled();
    });
  });

  it('should show error message on create failure', async () => {
    vi.mocked(api.createRoom).mockRejectedValueOnce(
      new Error('{"username": "Username is required"}')
    );

    renderHomePage();

    const usernameInputs = screen.getAllByPlaceholderText(/username/i);
    fireEvent.change(usernameInputs[0], { target: { value: 'A' } });

    const createButton = screen.getByRole('button', { name: /create room/i });
    fireEvent.click(createButton);

    await waitFor(() => {
      const errorMessage = screen.getByText(/error/i);
      expect(errorMessage).toBeInTheDocument();
    });
  });

  // ========== JOIN ROOM FLOW TESTS ==========

  it('should show error when username is empty on join', async () => {
    renderHomePage();

    const roomCodeInput = screen.getByPlaceholderText(/room code/i);
    fireEvent.change(roomCodeInput, { target: { value: 'TEST-ROOM' } });

    const joinButton = screen.getByRole('button', { name: /join room/i });
    fireEvent.click(joinButton);

    await waitFor(() => {
      const errorMessage = screen.getByText(/username.*required/i);
      expect(errorMessage).toBeInTheDocument();
    });
  });

  it('should show error when room code is empty', async () => {
    renderHomePage();

    const usernameInputs = screen.getAllByPlaceholderText(/username/i);
    const joinUsernameInput = usernameInputs[1]; // Second input is for join
    fireEvent.change(joinUsernameInput, { target: { value: 'TestUser' } });

    const joinButton = screen.getByRole('button', { name: /join room/i });
    fireEvent.click(joinButton);

    await waitFor(() => {
      const errorMessage = screen.getByText(/room code.*required/i);
      expect(errorMessage).toBeInTheDocument();
    });
  });

  it('should call joinRoom API with valid inputs', async () => {
    const mockRoom: RoomResponse = {
      roomId: 'JOIN-TEST',
      players: [],
      settings: { wordPack: 'english', timerSeconds: null },
      canStart: false,
      adminId: 'admin-1',
    };

    vi.mocked(api.joinRoom).mockResolvedValueOnce(mockRoom);

    renderHomePage();

    const usernameInputs = screen.getAllByPlaceholderText(/username/i);
    fireEvent.change(usernameInputs[1], { target: { value: 'Player2' } });

    const roomCodeInput = screen.getByPlaceholderText(/room code/i);
    fireEvent.change(roomCodeInput, { target: { value: 'TEST-ROOM' } });

    const joinButton = screen.getByRole('button', { name: /join room/i });
    fireEvent.click(joinButton);

    await waitFor(() => {
      expect(api.joinRoom).toHaveBeenCalledWith('TEST-ROOM', {
        username: 'Player2',
      });
    });
  });

  it('should navigate to room after joining', async () => {
    const mockRoom: RoomResponse = {
      roomId: 'JOINED-ROOM',
      players: [],
      settings: { wordPack: 'english', timerSeconds: null },
      canStart: false,
      adminId: 'admin-1',
    };

    vi.mocked(api.joinRoom).mockResolvedValueOnce(mockRoom);

    renderHomePage();

    const usernameInputs = screen.getAllByPlaceholderText(/username/i);
    fireEvent.change(usernameInputs[1], { target: { value: 'Player2' } });

    const roomCodeInput = screen.getByPlaceholderText(/room code/i);
    fireEvent.change(roomCodeInput, { target: { value: 'JOINED-ROOM' } });

    const joinButton = screen.getByRole('button', { name: /join room/i });
    fireEvent.click(joinButton);

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/room/JOINED-ROOM');
    });
  });

  it('should update store after joining', async () => {
    const mockRoom: RoomResponse = {
      roomId: 'STORE-JOIN',
      players: [
        {
          id: 'p2',
          username: 'JoinUser',
          team: null,
          role: 'SPECTATOR',
          connected: true,
          admin: false,
        },
      ],
      settings: { wordPack: 'english', timerSeconds: null },
      canStart: false,
      adminId: 'p1',
    };

    vi.mocked(api.joinRoom).mockResolvedValueOnce(mockRoom);

    renderHomePage();

    const usernameInputs = screen.getAllByPlaceholderText(/username/i);
    fireEvent.change(usernameInputs[1], { target: { value: 'JoinUser' } });

    const roomCodeInput = screen.getByPlaceholderText(/room code/i);
    fireEvent.change(roomCodeInput, { target: { value: 'STORE-JOIN' } });

    const joinButton = screen.getByRole('button', { name: /join room/i });
    fireEvent.click(joinButton);

    await waitFor(() => {
      expect(api.joinRoom).toHaveBeenCalled();
    });
  });

  it('should show error message on join failure', async () => {
    vi.mocked(api.joinRoom).mockRejectedValueOnce(
      new Error('{"error": "Room not found"}')
    );

    renderHomePage();

    const usernameInputs = screen.getAllByPlaceholderText(/username/i);
    fireEvent.change(usernameInputs[1], { target: { value: 'Player2' } });

    const roomCodeInput = screen.getByPlaceholderText(/room code/i);
    fireEvent.change(roomCodeInput, { target: { value: 'INVALID' } });

    const joinButton = screen.getByRole('button', { name: /join room/i });
    fireEvent.click(joinButton);

    await waitFor(() => {
      const errorMessage = screen.getByText(/error/i);
      expect(errorMessage).toBeInTheDocument();
    });
  });
});
