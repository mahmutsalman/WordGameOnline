package com.codenames.dto.events;

import com.codenames.dto.response.PlayerResponse;
import com.codenames.model.GameSettings;
import com.codenames.model.Role;
import com.codenames.model.Team;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EventDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========== PLAYER_JOINED_EVENT TESTS ==========

    @Test
    void shouldSerializePlayerJoinedEvent() throws Exception {
        // Test JSON serialization with fixed "type" field
        PlayerJoinedEvent event = PlayerJoinedEvent.builder()
                .playerId("player-123")
                .username("TestUser")
                .build();

        String json = objectMapper.writeValueAsString(event);
        assertThat(json).contains("\"type\":\"PLAYER_JOINED\"");
        assertThat(json).contains("\"playerId\":\"player-123\"");
        assertThat(json).contains("\"username\":\"TestUser\"");
    }

    @Test
    void shouldHaveCorrectTypeForPlayerJoinedEvent() {
        PlayerJoinedEvent event = PlayerJoinedEvent.builder()
                .playerId("player-123")
                .username("TestUser")
                .build();

        assertThat(event.getType()).isEqualTo("PLAYER_JOINED");
    }

    @Test
    void shouldBuildPlayerJoinedEventWithAllFields() {
        PlayerJoinedEvent event = PlayerJoinedEvent.builder()
                .playerId("player-456")
                .username("AnotherUser")
                .build();

        assertThat(event.getPlayerId()).isEqualTo("player-456");
        assertThat(event.getUsername()).isEqualTo("AnotherUser");
    }

    // ========== PLAYER_LEFT_EVENT TESTS ==========

    @Test
    void shouldSerializePlayerLeftEvent() throws Exception {
        PlayerLeftEvent event = PlayerLeftEvent.builder()
                .playerId("player-123")
                .build();

        String json = objectMapper.writeValueAsString(event);
        assertThat(json).contains("\"type\":\"PLAYER_LEFT\"");
        assertThat(json).contains("\"playerId\":\"player-123\"");
    }

    @Test
    void shouldHaveCorrectTypeForPlayerLeftEvent() {
        PlayerLeftEvent event = PlayerLeftEvent.builder()
                .playerId("player-123")
                .build();

        assertThat(event.getType()).isEqualTo("PLAYER_LEFT");
    }

    @Test
    void shouldBuildPlayerLeftEventWithPlayerId() {
        PlayerLeftEvent event = PlayerLeftEvent.builder()
                .playerId("player-789")
                .build();

        assertThat(event.getPlayerId()).isEqualTo("player-789");
    }

    // ========== PLAYER_UPDATED_EVENT TESTS ==========

    @Test
    void shouldSerializePlayerUpdatedEvent() throws Exception {
        PlayerUpdatedEvent event = PlayerUpdatedEvent.builder()
                .playerId("player-123")
                .team(Team.BLUE)
                .role(Role.OPERATIVE)
                .build();

        String json = objectMapper.writeValueAsString(event);
        assertThat(json).contains("\"type\":\"PLAYER_UPDATED\"");
        assertThat(json).contains("\"playerId\":\"player-123\"");
        assertThat(json).contains("\"team\":\"BLUE\"");
        assertThat(json).contains("\"role\":\"OPERATIVE\"");
    }

    @Test
    void shouldHaveCorrectTypeForPlayerUpdatedEvent() {
        PlayerUpdatedEvent event = PlayerUpdatedEvent.builder()
                .playerId("player-123")
                .team(Team.RED)
                .role(Role.SPYMASTER)
                .build();

        assertThat(event.getType()).isEqualTo("PLAYER_UPDATED");
    }

    @Test
    void shouldBuildPlayerUpdatedEventWithAllFields() {
        PlayerUpdatedEvent event = PlayerUpdatedEvent.builder()
                .playerId("player-999")
                .team(Team.BLUE)
                .role(Role.OPERATIVE)
                .build();

        assertThat(event.getPlayerId()).isEqualTo("player-999");
        assertThat(event.getTeam()).isEqualTo(Team.BLUE);
        assertThat(event.getRole()).isEqualTo(Role.OPERATIVE);
    }

    @Test
    void shouldAllowNullTeamInPlayerUpdatedEvent() {
        PlayerUpdatedEvent event = PlayerUpdatedEvent.builder()
                .playerId("player-123")
                .team(null)
                .role(Role.SPECTATOR)
                .build();

        assertThat(event.getTeam()).isNull();
        assertThat(event.getRole()).isEqualTo(Role.SPECTATOR);
    }

    // ========== ROOM_STATE_EVENT TESTS ==========

    @Test
    void shouldSerializeRoomStateEvent() throws Exception {
        PlayerResponse player = PlayerResponse.builder()
                .id("p1")
                .username("User1")
                .team(Team.BLUE)
                .role(Role.OPERATIVE)
                .connected(true)
                .admin(false)
                .build();

        RoomStateEvent event = RoomStateEvent.builder()
                .players(List.of(player))
                .settings(new GameSettings("english", null))
                .canStart(false)
                .build();

        String json = objectMapper.writeValueAsString(event);
        assertThat(json).contains("\"type\":\"ROOM_STATE\"");
        assertThat(json).contains("\"canStart\":false");
        assertThat(json).contains("\"username\":\"User1\"");
    }

    @Test
    void shouldHaveCorrectTypeForRoomStateEvent() {
        RoomStateEvent event = RoomStateEvent.builder()
                .players(List.of())
                .settings(new GameSettings("english", null))
                .canStart(false)
                .build();

        assertThat(event.getType()).isEqualTo("ROOM_STATE");
    }

    @Test
    void shouldBuildRoomStateEventWithMultiplePlayers() {
        PlayerResponse player1 = PlayerResponse.builder()
                .id("p1")
                .username("User1")
                .team(Team.BLUE)
                .role(Role.SPYMASTER)
                .connected(true)
                .admin(true)
                .build();

        PlayerResponse player2 = PlayerResponse.builder()
                .id("p2")
                .username("User2")
                .team(Team.RED)
                .role(Role.OPERATIVE)
                .connected(true)
                .admin(false)
                .build();

        RoomStateEvent event = RoomStateEvent.builder()
                .players(List.of(player1, player2))
                .settings(new GameSettings("english", 60))
                .canStart(true)
                .build();

        assertThat(event.getPlayers()).hasSize(2);
        assertThat(event.isCanStart()).isTrue();
        assertThat(event.getSettings().getWordPack()).isEqualTo("english");
        assertThat(event.getSettings().getTimerSeconds()).isEqualTo(60);
    }

    // ========== ERROR_EVENT TESTS ==========

    @Test
    void shouldSerializeErrorEvent() throws Exception {
        ErrorEvent event = ErrorEvent.builder()
                .message("Something went wrong")
                .build();

        String json = objectMapper.writeValueAsString(event);
        assertThat(json).contains("\"type\":\"ERROR\"");
        assertThat(json).contains("\"message\":\"Something went wrong\"");
    }

    @Test
    void shouldHaveCorrectTypeForErrorEvent() {
        ErrorEvent event = ErrorEvent.builder()
                .message("Error message")
                .build();

        assertThat(event.getType()).isEqualTo("ERROR");
    }

    @Test
    void shouldBuildErrorEventWithMessage() {
        ErrorEvent event = ErrorEvent.builder()
                .message("Room not found")
                .build();

        assertThat(event.getMessage()).isEqualTo("Room not found");
    }
}
