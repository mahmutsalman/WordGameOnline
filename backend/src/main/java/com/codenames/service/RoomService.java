package com.codenames.service;

import com.codenames.dto.response.PlayerResponse;
import com.codenames.dto.response.RoomResponse;
import com.codenames.exception.RoomNotFoundException;
import com.codenames.exception.UsernameAlreadyExistsException;
import com.codenames.factory.RoomFactory;
import com.codenames.model.Player;
import com.codenames.model.Room;
import com.codenames.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for room management.
 * Handles business logic for creating, joining, and managing game rooms.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomFactory roomFactory;

    /**
     * Creates a new room with the specified admin username.
     * Generates a unique player ID for the admin and persists the room.
     *
     * @param username the admin's username
     * @return the newly created room
     */
    public Room createRoom(String username) {
        String playerId = UUID.randomUUID().toString();
        log.info("Creating room with admin: {} (ID: {})", username, playerId);

        Room room = roomFactory.create(playerId, username);
        Room savedRoom = roomRepository.save(room);

        log.info("Room created successfully: {}", savedRoom.getRoomId());
        return savedRoom;
    }

    /**
     * Adds a player to an existing room.
     * Validates that the room exists and the username is not already taken.
     *
     * @param roomId   the room ID to join
     * @param username the player's username
     * @return the updated room with the new player
     * @throws RoomNotFoundException if the room doesn't exist
     * @throws UsernameAlreadyExistsException if the username is already taken
     */
    public Room joinRoom(String roomId, String username) {
        log.info("Player {} attempting to join room {}", username, roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        // Check if username already exists (case-insensitive)
        boolean usernameTaken = room.getPlayers().stream()
                .anyMatch(p -> p.getUsername().equalsIgnoreCase(username));

        if (usernameTaken) {
            log.warn("Username {} already exists in room {}", username, roomId);
            throw new UsernameAlreadyExistsException(username);
        }

        Player newPlayer = roomFactory.createPlayer(username);
        room.getPlayers().add(newPlayer);

        log.info("Player {} (ID: {}) joined room {}", username, newPlayer.getId(), roomId);
        return room;
    }

    /**
     * Retrieves a room by ID.
     *
     * @param roomId the room ID to retrieve
     * @return the room
     * @throws RoomNotFoundException if the room doesn't exist
     */
    public Room getRoom(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));
    }

    /**
     * Checks if a room exists.
     *
     * @param roomId the room ID to check
     * @return true if the room exists, false otherwise
     */
    public boolean roomExists(String roomId) {
        return roomRepository.existsById(roomId);
    }

    /**
     * Removes a player from a room.
     * If the room becomes empty, it is automatically deleted.
     *
     * @param roomId   the room ID
     * @param playerId the player ID to remove
     */
    public void leaveRoom(String roomId, String playerId) {
        log.info("Player {} leaving room {}", playerId, roomId);

        Room room = getRoom(roomId);
        room.getPlayers().removeIf(p -> p.getId().equals(playerId));

        // Delete room if empty
        if (room.getPlayers().isEmpty()) {
            log.info("Room {} is now empty, deleting", roomId);
            roomRepository.deleteById(roomId);
        } else {
            log.info("Player {} left room {} ({} players remaining)",
                    playerId, roomId, room.getPlayers().size());
        }
    }

    /**
     * Converts a Room entity to a RoomResponse DTO.
     * Maps all players to PlayerResponse DTOs.
     *
     * @param room the room to convert
     * @return the room response DTO
     */
    public RoomResponse toResponse(Room room) {
        List<PlayerResponse> players = room.getPlayers().stream()
                .map(p -> PlayerResponse.builder()
                        .id(p.getId())
                        .username(p.getUsername())
                        .team(p.getTeam())
                        .role(p.getRole())
                        .connected(p.isConnected())
                        .admin(p.isAdmin())
                        .build())
                .collect(Collectors.toList());

        return RoomResponse.builder()
                .roomId(room.getRoomId())
                .players(players)
                .settings(room.getSettings())
                .canStart(room.canStart())
                .adminId(room.getAdminId())
                .build();
    }
}
