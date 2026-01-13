package com.codenames.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WebSocketSessionManagerTest {

    private WebSocketSessionManager sessionManager;

    @BeforeEach
    void setup() {
        sessionManager = new WebSocketSessionManager();
    }

    // ========== REGISTRATION TESTS ==========

    @Test
    void shouldRegisterSession() {
        sessionManager.registerSession("player-1", "ROOM-123", "session-1");

        Optional<WebSocketSessionManager.SessionInfo> info = sessionManager.getSession("player-1");

        assertThat(info).isPresent();
        assertThat(info.get().getPlayerId()).isEqualTo("player-1");
        assertThat(info.get().getRoomId()).isEqualTo("ROOM-123");
        assertThat(info.get().getSessionId()).isEqualTo("session-1");
    }

    @Test
    void shouldRegisterMultiplePlayersInSameRoom() {
        sessionManager.registerSession("player-1", "ROOM-123", "session-1");
        sessionManager.registerSession("player-2", "ROOM-123", "session-2");

        Set<String> players = sessionManager.getRoomPlayers("ROOM-123");

        assertThat(players).containsExactlyInAnyOrder("player-1", "player-2");
    }

    @Test
    void shouldRegisterPlayersInDifferentRooms() {
        sessionManager.registerSession("player-1", "ROOM-123", "session-1");
        sessionManager.registerSession("player-2", "ROOM-456", "session-2");

        Set<String> room123Players = sessionManager.getRoomPlayers("ROOM-123");
        Set<String> room456Players = sessionManager.getRoomPlayers("ROOM-456");

        assertThat(room123Players).containsExactly("player-1");
        assertThat(room456Players).containsExactly("player-2");
    }

    @Test
    void shouldOverwriteSessionIfPlayerIdAlreadyExists() {
        sessionManager.registerSession("player-1", "ROOM-123", "session-old");
        sessionManager.registerSession("player-1", "ROOM-456", "session-new");

        Optional<WebSocketSessionManager.SessionInfo> info = sessionManager.getSession("player-1");

        assertThat(info).isPresent();
        assertThat(info.get().getRoomId()).isEqualTo("ROOM-456");
        assertThat(info.get().getSessionId()).isEqualTo("session-new");
    }

    // ========== REMOVAL TESTS ==========

    @Test
    void shouldRemoveSession() {
        sessionManager.registerSession("player-1", "ROOM-123", "session-1");

        sessionManager.removeSession("player-1");

        assertThat(sessionManager.getSession("player-1")).isEmpty();
        assertThat(sessionManager.getRoomPlayers("ROOM-123")).isEmpty();
    }

    @Test
    void shouldRemoveOnlySpecificPlayerFromRoom() {
        sessionManager.registerSession("player-1", "ROOM-123", "session-1");
        sessionManager.registerSession("player-2", "ROOM-123", "session-2");

        sessionManager.removeSession("player-1");

        assertThat(sessionManager.getSession("player-1")).isEmpty();
        assertThat(sessionManager.getRoomPlayers("ROOM-123")).containsExactly("player-2");
    }

    @Test
    void shouldHandleRemovalOfNonExistentSession() {
        // Should not throw exception
        sessionManager.removeSession("non-existent");

        // Verify nothing breaks
        assertThat(sessionManager.getSession("non-existent")).isEmpty();
    }

    // ========== LOOKUP TESTS ==========

    @Test
    void shouldGetPlayerIdBySessionId() {
        sessionManager.registerSession("player-1", "ROOM-123", "session-1");

        String playerId = sessionManager.getPlayerIdBySessionId("session-1");

        assertThat(playerId).isEqualTo("player-1");
    }

    @Test
    void shouldReturnNullForNonExistentSessionId() {
        String playerId = sessionManager.getPlayerIdBySessionId("non-existent");

        assertThat(playerId).isNull();
    }

    @Test
    void shouldGetSessionByPlayerId() {
        sessionManager.registerSession("player-1", "ROOM-123", "session-1");

        Optional<WebSocketSessionManager.SessionInfo> info = sessionManager.getSession("player-1");

        assertThat(info).isPresent();
        assertThat(info.get().getPlayerId()).isEqualTo("player-1");
    }

    @Test
    void shouldReturnEmptyOptionalForNonExistentPlayer() {
        Optional<WebSocketSessionManager.SessionInfo> info = sessionManager.getSession("non-existent");

        assertThat(info).isEmpty();
    }

    @Test
    void shouldGetRoomPlayers() {
        sessionManager.registerSession("player-1", "ROOM-123", "session-1");
        sessionManager.registerSession("player-2", "ROOM-123", "session-2");
        sessionManager.registerSession("player-3", "ROOM-123", "session-3");

        Set<String> players = sessionManager.getRoomPlayers("ROOM-123");

        assertThat(players).hasSize(3);
        assertThat(players).containsExactlyInAnyOrder("player-1", "player-2", "player-3");
    }

    @Test
    void shouldReturnEmptySetForNonExistentRoom() {
        Set<String> players = sessionManager.getRoomPlayers("non-existent");

        assertThat(players).isEmpty();
    }

    // ========== CONCURRENCY TESTS ==========

    @Test
    void shouldBeThreadSafe() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 100; i++) {
            final int index = i;
            executor.submit(() -> {
                sessionManager.registerSession("player-" + index, "ROOM-123", "session-" + index);
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(sessionManager.getRoomPlayers("ROOM-123")).hasSize(100);
    }

    @Test
    void shouldHandleConcurrentRegistrationAndRemoval() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // Register 50 players
        for (int i = 0; i < 50; i++) {
            final int index = i;
            executor.submit(() -> {
                sessionManager.registerSession("player-" + index, "ROOM-123", "session-" + index);
            });
        }

        // Remove 25 players concurrently
        for (int i = 0; i < 25; i++) {
            final int index = i;
            executor.submit(() -> {
                sessionManager.removeSession("player-" + index);
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        Set<String> players = sessionManager.getRoomPlayers("ROOM-123");
        assertThat(players.size()).isGreaterThanOrEqualTo(25);
        assertThat(players.size()).isLessThanOrEqualTo(50);
    }
}
