package com.codenames.service;

import com.codenames.exception.GameStartException;
import com.codenames.exception.RoomNotFoundException;
import com.codenames.factory.GameStateFactory;
import com.codenames.model.Card;
import com.codenames.model.CardColor;
import com.codenames.model.Clue;
import com.codenames.model.GamePhase;
import com.codenames.model.GameState;
import com.codenames.model.Player;
import com.codenames.model.Role;
import com.codenames.model.Room;
import com.codenames.model.Team;
import com.codenames.model.TurnHistory;
import com.codenames.repository.RoomRepository;
import com.codenames.service.WebSocketSessionManager.SessionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
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
    private static final int BOARD_SIZE = 25;

    private final RoomRepository roomRepository;
    private final GameStateFactory gameStateFactory;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager sessionManager;

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
     * Submit a clue for the current team.
     *
     * @param roomId the room ID
     * @param playerId the submitting player's ID
     * @param word the clue word
     * @param number the clue number (0+)
     * @return updated game state
     */
    public GameState submitClue(String roomId, String playerId, String word, int number) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        GameState state = requireGameStarted(roomId);
        requireGameNotOver(state);

        if (state.getPhase() != GamePhase.CLUE) {
            throw new IllegalStateException("Cannot submit clue during " + state.getPhase() + " phase");
        }

        Player player = requirePlayer(room, playerId);
        if (player.getRole() != Role.SPYMASTER || player.getTeam() != state.getCurrentTeam()) {
            throw new IllegalStateException("Only the current team's spymaster can submit a clue");
        }

        String clueWord = word == null ? "" : word.trim();
        if (clueWord.isBlank()) {
            throw new IllegalArgumentException("Clue word is required");
        }
        if (clueWord.contains(" ")) {
            throw new IllegalArgumentException("Clue must be a single word");
        }
        if (number < 0 || number > 9) {
            throw new IllegalArgumentException("Clue number must be between 0 and 9");
        }

        Clue clue = Clue.builder()
                .word(clueWord)
                .number(number)
                .team(state.getCurrentTeam())
                .build();

        state.setCurrentClue(clue);
        state.setGuessesRemaining(number + 1);
        state.setPhase(GamePhase.GUESS);

        if (state.getHistory() == null) {
            state.setHistory(new java.util.ArrayList<>());
        }
        state.getHistory().add(TurnHistory.builder()
                .team(state.getCurrentTeam())
                .clue(clue)
                .guessedWords(new java.util.ArrayList<>())
                .guessedColors(new java.util.ArrayList<>())
                .build());

        broadcastGameState(roomId, state);
        return state;
    }

    /**
     * Make a guess as an operative for the current team.
     *
     * @param roomId the room ID
     * @param playerId the guessing player's ID
     * @param cardIndex board index (0-24)
     * @return updated game state
     */
    public GameState makeGuess(String roomId, String playerId, int cardIndex) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        GameState state = requireGameStarted(roomId);
        requireGameNotOver(state);

        if (state.getPhase() != GamePhase.GUESS) {
            throw new IllegalStateException("Cannot guess during " + state.getPhase() + " phase");
        }
        if (cardIndex < 0 || cardIndex >= BOARD_SIZE) {
            throw new IllegalArgumentException("Invalid card index");
        }

        Player player = requirePlayer(room, playerId);
        if (player.getRole() != Role.OPERATIVE || player.getTeam() != state.getCurrentTeam()) {
            throw new IllegalStateException("Only the current team's operatives can guess");
        }

        Card card = state.getCard(cardIndex);
        if (card.isRevealed()) {
            throw new IllegalStateException("Card already revealed");
        }

        card.setRevealed(true);
        card.setSelectedBy(playerId);

        TurnHistory currentTurn = ensureCurrentTurn(state);
        currentTurn.getGuessedWords().add(card.getWord());
        currentTurn.getGuessedColors().add(card.getColor());

        CardColor color = card.getColor();
        Team currentTeam = state.getCurrentTeam();

        if (color == CardColor.ASSASSIN) {
            state.setPhase(GamePhase.GAME_OVER);
            state.setWinner(otherTeam(currentTeam));
            state.setGuessesRemaining(0);
            broadcastGameState(roomId, state);
            return state;
        }

        if (color == CardColor.BLUE) {
            state.setBlueRemaining(Math.max(0, state.getBlueRemaining() - 1));
            if (state.getBlueRemaining() == 0) {
                state.setPhase(GamePhase.GAME_OVER);
                state.setWinner(Team.BLUE);
                state.setGuessesRemaining(0);
                broadcastGameState(roomId, state);
                return state;
            }
        } else if (color == CardColor.RED) {
            state.setRedRemaining(Math.max(0, state.getRedRemaining() - 1));
            if (state.getRedRemaining() == 0) {
                state.setPhase(GamePhase.GAME_OVER);
                state.setWinner(Team.RED);
                state.setGuessesRemaining(0);
                broadcastGameState(roomId, state);
                return state;
            }
        }

        // Correct guess: decrement remaining guesses, continue if > 0.
        if ((color == CardColor.BLUE && currentTeam == Team.BLUE)
                || (color == CardColor.RED && currentTeam == Team.RED)) {
            state.setGuessesRemaining(Math.max(0, state.getGuessesRemaining() - 1));
            if (state.getGuessesRemaining() == 0) {
                endTurn(state);
            }
            broadcastGameState(roomId, state);
            return state;
        }

        // Neutral or opponent card: turn ends immediately.
        endTurn(state);
        broadcastGameState(roomId, state);
        return state;
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
        return createGameStateEvent(state, false);
    }

    /**
     * Creates a role-aware game state event map for WebSocket broadcasting.
     *
     * @param state the game state to convert to event
     * @param revealAllColors whether unrevealed card colors should be included
     * @return a map containing all game state fields with "type" = "GAME_STATE"
     */
    public Map<String, Object> createGameStateEvent(GameState state, boolean revealAllColors) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "GAME_STATE");
        event.put("board", toBoardView(state.getBoard(), revealAllColors));
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
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        for (Player player : room.getPlayers()) {
            if (!player.isConnected()) {
                continue;
            }

            SessionInfo session = sessionManager.getSession(player.getId()).orElse(null);

            if (session == null) {
                continue;
            }

            boolean revealAllColors = player.getRole() == Role.SPYMASTER;
            String destination = "/queue/private-user" + session.getSessionId();
            messagingTemplate.convertAndSend(destination, createGameStateEvent(state, revealAllColors));
        }
    }

    private List<Card> toBoardView(List<Card> board, boolean revealAllColors) {
        if (board == null) {
            return java.util.Collections.emptyList();
        }

        if (revealAllColors) {
            return board;
        }

        return board.stream()
                .map(card -> {
                    if (card.isRevealed()) {
                        return card;
                    }
                    return Card.builder()
                            .word(card.getWord())
                            .color(null)
                            .revealed(false)
                            .selectedBy(null)
                            .build();
                })
                .toList();
    }

    private GameState requireGameStarted(String roomId) {
        GameState state = gameStates.get(roomId);
        if (state == null) {
            throw new IllegalStateException("Game has not started");
        }
        return state;
    }

    private void requireGameNotOver(GameState state) {
        if (state.getWinner() != null || state.getPhase() == GamePhase.GAME_OVER) {
            throw new IllegalStateException("Game is over");
        }
    }

    private Player requirePlayer(Room room, String playerId) {
        return room.getPlayer(playerId)
                .orElseThrow(() -> new IllegalStateException("Player not found"));
    }

    private TurnHistory ensureCurrentTurn(GameState state) {
        if (state.getHistory() == null) {
            state.setHistory(new java.util.ArrayList<>());
        }
        if (state.getHistory().isEmpty()) {
            TurnHistory turn = TurnHistory.builder()
                    .team(state.getCurrentTeam())
                    .clue(state.getCurrentClue())
                    .guessedWords(new java.util.ArrayList<>())
                    .guessedColors(new java.util.ArrayList<>())
                    .build();
            state.getHistory().add(turn);
            return turn;
        }
        TurnHistory last = state.getHistory().get(state.getHistory().size() - 1);
        if (last.getGuessedWords() == null) {
            last.setGuessedWords(new java.util.ArrayList<>());
        }
        if (last.getGuessedColors() == null) {
            last.setGuessedColors(new java.util.ArrayList<>());
        }
        return last;
    }

    private void endTurn(GameState state) {
        state.setCurrentTeam(otherTeam(state.getCurrentTeam()));
        state.setPhase(GamePhase.CLUE);
        state.setCurrentClue(null);
        state.setGuessesRemaining(0);
    }

    private Team otherTeam(Team team) {
        return team == Team.BLUE ? Team.RED : Team.BLUE;
    }
}
