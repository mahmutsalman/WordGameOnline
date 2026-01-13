package com.codenames.factory;

import com.codenames.model.Player;
import com.codenames.model.Role;
import com.codenames.model.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RoomFactory.
 * Tests room creation, ID generation, and player creation logic.
 */
class RoomFactoryTest {

    private RoomFactory roomFactory;

    @BeforeEach
    void setUp() {
        roomFactory = new RoomFactory();
    }

    /**
     * Test that created room has valid ID format: XXXXX-XXXXX.
     */
    @Test
    void shouldCreateRoomWithValidId() {
        // Arrange & Act
        Room room = roomFactory.create("admin-123", "AdminUser");

        // Assert
        assertThat(room.getRoomId())
                .as("Room ID should match pattern XXXXX-XXXXX")
                .matches("[A-Z2-9]{5}-[A-Z2-9]{5}");
    }

    /**
     * Test that created room has correct admin ID.
     */
    @Test
    void shouldCreateRoomWithAdminId() {
        // Arrange & Act
        Room room = roomFactory.create("admin-xyz", "AdminUser");

        // Assert
        assertThat(room.getAdminId())
                .as("Admin ID should match provided ID")
                .isEqualTo("admin-xyz");
    }

    /**
     * Test that created room has a recent createdAt timestamp.
     */
    @Test
    void shouldCreateRoomWithCreatedAt() {
        // Arrange & Act
        Room room = roomFactory.create("admin-1", "Admin");

        // Assert
        assertThat(room.getCreatedAt())
                .as("CreatedAt should be set and recent")
                .isNotNull()
                .isBeforeOrEqualTo(java.time.LocalDateTime.now());
    }

    /**
     * Test that admin is added as the first player.
     */
    @Test
    void shouldCreateAdminAsFirstPlayer() {
        // Arrange & Act
        Room room = roomFactory.create("admin-123", "AdminUser");

        // Assert
        assertThat(room.getPlayers())
                .as("Room should have exactly one player (admin)")
                .hasSize(1);

        Player admin = room.getPlayers().get(0);
        assertThat(admin.isAdmin())
                .as("First player should be marked as admin")
                .isTrue();
        assertThat(admin.getUsername())
                .as("Admin username should match")
                .isEqualTo("AdminUser");
    }

    /**
     * Test that admin player starts as SPECTATOR role.
     */
    @Test
    void shouldSetAdminAsSpectator() {
        // Arrange & Act
        Room room = roomFactory.create("admin-1", "Admin");
        Player admin = room.getPlayers().get(0);

        // Assert
        assertThat(admin.getRole())
                .as("Admin should start as SPECTATOR")
                .isEqualTo(Role.SPECTATOR);
    }

    /**
     * Test that admin player is marked as connected.
     */
    @Test
    void shouldSetAdminAsConnected() {
        // Arrange & Act
        Room room = roomFactory.create("admin-1", "Admin");
        Player admin = room.getPlayers().get(0);

        // Assert
        assertThat(admin.isConnected())
                .as("Admin should be connected")
                .isTrue();
    }

    /**
     * Test that room IDs are unique across multiple generations.
     */
    @Test
    void shouldGenerateUniqueRoomIds() {
        // Arrange
        Set<String> roomIds = new HashSet<>();

        // Act
        for (int i = 0; i < 100; i++) {
            Room room = roomFactory.create("admin-" + i, "User" + i);
            roomIds.add(room.getRoomId());
        }

        // Assert
        assertThat(roomIds)
                .as("All generated room IDs should be unique")
                .hasSize(100);
    }

    /**
     * Test that room IDs exclude confusing characters (I, O, 0, 1).
     */
    @Test
    void shouldExcludeConfusingCharacters() {
        // Arrange
        Set<Character> confusingChars = Set.of('I', 'O', '0', '1');
        Set<String> roomIds = new HashSet<>();

        // Act
        for (int i = 0; i < 50; i++) {
            Room room = roomFactory.create("admin-" + i, "User");
            roomIds.add(room.getRoomId());
        }

        // Assert
        for (String roomId : roomIds) {
            for (char c : roomId.toCharArray()) {
                if (c != '-') {
                    assertThat(confusingChars)
                            .as("Room ID should not contain confusing characters: " + roomId)
                            .doesNotContain(c);
                }
            }
        }
    }

    /**
     * Test that createPlayer() generates a player with UUID.
     */
    @Test
    void shouldCreatePlayerWithId() {
        // Arrange & Act
        Player player = roomFactory.createPlayer("TestUser");

        // Assert
        assertThat(player.getId())
                .as("Player should have a generated ID")
                .isNotNull()
                .isNotEmpty();
    }

    /**
     * Test that created player starts as SPECTATOR.
     */
    @Test
    void shouldCreatePlayerAsSpectator() {
        // Arrange & Act
        Player player = roomFactory.createPlayer("TestUser");

        // Assert
        assertThat(player.getRole())
                .as("New player should be SPECTATOR")
                .isEqualTo(Role.SPECTATOR);
        assertThat(player.getTeam())
                .as("New player should have no team")
                .isNull();
        assertThat(player.isAdmin())
                .as("New player should not be admin")
                .isFalse();
    }
}
