package com.codenames.controller;

import com.codenames.dto.request.CreateRoomRequest;
import com.codenames.dto.request.JoinRoomRequest;
import com.codenames.dto.response.RoomResponse;
import com.codenames.model.Room;
import com.codenames.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
}
