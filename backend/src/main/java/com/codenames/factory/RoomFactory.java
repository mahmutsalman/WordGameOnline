package com.codenames.factory;

import com.codenames.model.Player;
import com.codenames.model.Role;
import com.codenames.model.Room;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Factory for creating Room and Player instances.
 * Uses Factory pattern to encapsulate room and player creation logic.
 * Generates unique room IDs using SecureRandom.
 */
@Component
public class RoomFactory {

    /**
     * Character set for room ID generation.
     * Excludes confusing characters: I, O, 0, 1.
     */
    private static final String ROOM_ID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /**
     * Secure random generator for room IDs.
     */
    private final SecureRandom random = new SecureRandom();

    /**
     * Creates a new room with the specified admin.
     * The admin is added as the first player with SPECTATOR role.
     *
     * @param adminId       unique ID for the admin player
     * @param adminUsername display name for the admin
     * @return newly created room with admin player
     */
    public Room create(String adminId, String adminUsername) {
        Player admin = Player.builder()
                .id(adminId)
                .username(adminUsername)
                .team(null)
                .role(Role.SPECTATOR)
                .connected(true)
                .admin(true)
                .build();

        return Room.builder()
                .roomId(generateRoomId())
                .adminId(adminId)
                .createdAt(LocalDateTime.now())
                .settings(new com.codenames.model.GameSettings())
                .players(new CopyOnWriteArrayList<>(List.of(admin)))
                .build();
    }

    /**
     * Generates a unique room ID.
     * Format: XXXXX-XXXXX (5 characters, hyphen, 5 characters).
     * Uses only uppercase letters and digits 2-9 to avoid confusion.
     *
     * @return generated room ID
     */
    private String generateRoomId() {
        StringBuilder sb = new StringBuilder();

        // First 5 characters
        for (int i = 0; i < 5; i++) {
            sb.append(ROOM_ID_CHARS.charAt(random.nextInt(ROOM_ID_CHARS.length())));
        }

        sb.append("-");

        // Second 5 characters
        for (int i = 0; i < 5; i++) {
            sb.append(ROOM_ID_CHARS.charAt(random.nextInt(ROOM_ID_CHARS.length())));
        }

        return sb.toString();
    }

    /**
     * Creates a new player with generated UUID.
     * Player starts as SPECTATOR with no team assigned.
     *
     * @param username display name for the player
     * @return newly created player
     */
    public Player createPlayer(String username) {
        return Player.builder()
                .id(UUID.randomUUID().toString())
                .username(username)
                .team(null)
                .role(Role.SPECTATOR)
                .connected(true)
                .admin(false)
                .build();
    }
}
