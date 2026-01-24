package com.codenames.controller;

import com.codenames.dto.request.ChangeTeamRequest;
import com.codenames.dto.request.CreateRoomRequest;
import com.codenames.dto.request.JoinRoomRequest;
import com.codenames.dto.response.CardResponse;
import com.codenames.dto.response.GameStateResponse;
import com.codenames.dto.response.RoomResponse;
import com.codenames.model.GameState;
import com.codenames.model.Room;
import com.codenames.service.GameService;
import com.codenames.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for room management endpoints.
 * Handles HTTP requests for creating, joining, and querying rooms.
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RoomService roomService;
    private final GameService gameService;

    /**
     * Creates a new game room.
     * POST /api/rooms
     *
     * @param request the room creation request with admin username
     * @return 201 CREATED with room details
     */
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        log.info("Received create room request for user: {}", request.getUsername());

        Room room = roomService.createRoom(request.getUsername());
        RoomResponse response = roomService.toResponse(room);

        log.info("Room created: {}", response.getRoomId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves room details by ID.
     * GET /api/rooms/{roomId}
     *
     * @param roomId the room ID to retrieve
     * @return 200 OK with room details, or 404 NOT FOUND
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String roomId) {
        log.info("Received get room request: {}", roomId);

        Room room = roomService.getRoom(roomId);
        RoomResponse response = roomService.toResponse(room);

        return ResponseEntity.ok(response);
    }

    /**
     * Checks if a room exists.
     * GET /api/rooms/{roomId}/exists
     *
     * @param roomId the room ID to check
     * @return 200 OK with {"exists": true/false}
     */
    @GetMapping("/{roomId}/exists")
    public ResponseEntity<Map<String, Boolean>> roomExists(@PathVariable String roomId) {
        log.info("Checking if room exists: {}", roomId);

        boolean exists = roomService.roomExists(roomId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Adds a player to an existing room.
     * POST /api/rooms/{roomId}/join
     *
     * @param roomId  the room ID to join
     * @param request the join request with username
     * @return 200 OK with updated room details, or 404/409 on error
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<RoomResponse> joinRoom(
            @PathVariable String roomId,
            @Valid @RequestBody JoinRoomRequest request) {
        log.info("Received join room request: {} for user: {}", roomId, request.getUsername());

        Room room = roomService.joinRoom(roomId, request.getUsername());
        RoomResponse response = roomService.toResponse(room);

        log.info("User {} joined room {}", request.getUsername(), roomId);
        return ResponseEntity.ok(response);
    }

    /**
     * Changes a player's team and role.
     * PUT /api/rooms/{roomId}/players/{playerId}/team
     *
     * @param roomId the room ID
     * @param playerId the player ID to update
     * @param request the team change request
     * @return 200 OK with updated room
     *
     * <p><b>Note:</b> This HTTP endpoint is provided for testing/debugging.
     * In production, this operation is performed via WebSocket: {@code /app/room/{roomId}/team}</p>
     */
    @PutMapping("/{roomId}/players/{playerId}/team")
    public ResponseEntity<RoomResponse> changePlayerTeam(
            @PathVariable String roomId,
            @PathVariable String playerId,
            @Valid @RequestBody ChangeTeamRequest request) {
        log.info("Changing team for player {} in room {}: team={}, role={}",
                playerId, roomId, request.getTeam(), request.getRole());

        Room room = roomService.changePlayerTeam(roomId, playerId, request.getTeam(), request.getRole());
        RoomResponse response = roomService.toResponse(room);

        return ResponseEntity.ok(response);
    }

    /**
     * Starts the game in a room.
     * POST /api/rooms/{roomId}/start
     *
     * @param roomId the room ID to start the game in
     * @return 200 OK with game state, or 400 BAD REQUEST if game can't start
     *
     * <p><b>Note:</b> This HTTP endpoint is provided for testing/debugging.
     * In production, game start will be triggered via WebSocket handler.</p>
     */
    @PostMapping("/{roomId}/start")
    public ResponseEntity<GameStateResponse> startGame(@PathVariable String roomId) {
        log.info("Received start game request for room: {}", roomId);

        GameState gameState = gameService.startGame(roomId);
        GameStateResponse response = toGameStateResponse(gameState, true);

        log.info("Game started in room: {}", roomId);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets the current game state for a room.
     * GET /api/rooms/{roomId}/game
     *
     * @param roomId the room ID
     * @return 200 OK with game state, or 404 if no game in progress
     *
     * <p><b>Note:</b> This HTTP endpoint is provided for testing/debugging.
     * In production, game state is broadcast via WebSocket: {@code /topic/room/{roomId}}</p>
     */
    @GetMapping("/{roomId}/game")
    public ResponseEntity<GameStateResponse> getGameState(@PathVariable String roomId) {
        log.info("Getting game state for room: {}", roomId);

        GameState gameState = gameService.getGameState(roomId);
        if (gameState == null) {
            return ResponseEntity.notFound().build();
        }

        // For testing, show all colors (spymaster view)
        GameStateResponse response = toGameStateResponse(gameState, true);
        return ResponseEntity.ok(response);
    }

    /**
     * Converts GameState to GameStateResponse DTO.
     *
     * @param gameState the game state to convert
     * @param showColors whether to show card colors (spymaster view)
     * @return the game state response DTO
     */
    private GameStateResponse toGameStateResponse(GameState gameState, boolean showColors) {
        return GameStateResponse.builder()
                .board(gameState.getBoard().stream()
                        .map(card -> CardResponse.builder()
                                .word(card.getWord())
                                .color(showColors || card.isRevealed() ? card.getColor() : null)
                                .revealed(card.isRevealed())
                                .selectedBy(card.getSelectedBy())
                                .build())
                        .collect(Collectors.toList()))
                .currentTeam(gameState.getCurrentTeam())
                .phase(gameState.getPhase())
                .currentClue(gameState.getCurrentClue())
                .guessesRemaining(gameState.getGuessesRemaining())
                .blueRemaining(gameState.getBlueRemaining())
                .redRemaining(gameState.getRedRemaining())
                .winner(gameState.getWinner())
                .history(gameState.getHistory())
                .build();
    }
}
