package com.codenames.service;

import com.codenames.exception.GameStartException;
import com.codenames.exception.RoomNotFoundException;
import com.codenames.factory.GameStateFactory;
import com.codenames.model.GameState;
import com.codenames.model.Room;
import com.codenames.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for orchestrating Codenames game operations.
 * Handles game start, state management, and game flow coordination.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GameService {

    private static final String DEFAULT_WORD_PACK = "english";

    private final RoomRepository roomRepository;
    private final GameStateFactory gameStateFactory;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * In-memory storage for game states.
     * GameState is not persisted to DB (Room.gameState is @Transient),
     * so we store it in memory for the duration of the server session.
     */
    private final Map<String, GameState> gameStates = new ConcurrentHashMap<>();

    /**
     * Starts a new game in the specified room.
     * Validates that all required roles are filled before starting.
     *
     * @param roomId the room ID to start the game in
     * @return the initialized game state
     * @throws RoomNotFoundException if the room doesn't exist
     * @throws GameStartException if the room doesn't meet the requirements to start
     */
    public GameState startGame(String roomId) {
        log.info("Starting game in room: {}", roomId);

        // Check if game already started
        if (gameStates.containsKey(roomId)) {
            log.warn("Game already started in room: {}", roomId);
            throw new GameStartException("Game already started in room: " + roomId);
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        // Validate room can start
        if (!room.canStart()) {
            log.warn("Room {} cannot start - missing required roles", roomId);
            throw new GameStartException(
                    "Game cannot start: requires connected spymasters and operatives on both teams");
        }

        // Generate board and initialize game state (factory handles random team selection)
        GameState gameState = gameStateFactory.create(DEFAULT_WORD_PACK);
        log.info("Starting team selected: {}", gameState.getCurrentTeam());

        // Store game state in memory map and room
        gameStates.put(roomId, gameState);
        room.setGameState(gameState);

        // Broadcast game state to all room subscribers
        broadcastGameState(roomId, gameState);

        log.info("Game started successfully in room: {}", roomId);
        return gameState;
    }

    /**
     * Gets the current game state for a room.
     *
     * @param roomId the room ID
     * @return the game state, or null if game hasn't started
     * @throws RoomNotFoundException if the room doesn't exist
     */
    @Transactional(readOnly = true)
    public GameState getGameState(String roomId) {
        // Validate room exists
        if (!roomRepository.existsById(roomId)) {
            throw new RoomNotFoundException(roomId);
        }

        // Return game state from in-memory storage
        return gameStates.get(roomId);
    }

    /**
     * Creates a game state event map for WebSocket broadcasting.
     *
     * @param state the game state to convert to event
     * @return a map containing all game state fields with "type" = "GAME_STATE"
     */
    public Map<String, Object> createGameStateEvent(GameState state) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "GAME_STATE");
        event.put("board", state.getBoard());
        event.put("currentTeam", state.getCurrentTeam());
        event.put("phase", state.getPhase());
        event.put("currentClue", state.getCurrentClue());
        event.put("guessesRemaining", state.getGuessesRemaining());
        event.put("blueRemaining", state.getBlueRemaining());
        event.put("redRemaining", state.getRedRemaining());
        event.put("winner", state.getWinner());
        event.put("history", state.getHistory());
        return event;
    }

    /**
     * Broadcasts game state to all subscribers of the room's game topic.
     *
     * @param roomId the room ID
     * @param state the game state to broadcast
     */
    private void broadcastGameState(String roomId, GameState state) {
        String destination = "/topic/room/" + roomId + "/game";
        log.debug("Broadcasting game state to {}", destination);
        messagingTemplate.convertAndSend(destination, createGameStateEvent(state));
    }
}
