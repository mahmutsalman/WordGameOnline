package com.codenames.controller;

import com.codenames.dto.request.ChangeTeamRequest;
import com.codenames.dto.request.CreateRoomRequest;
import com.codenames.dto.request.JoinRoomRequest;
import com.codenames.model.Role;
import com.codenames.model.Team;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for game-related endpoints in RoomController.
 * Tests POST /api/rooms/{roomId}/start and GET /api/rooms/{roomId}/game
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("RoomController Game Endpoints")
class RoomControllerGameTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ========== START GAME TESTS ==========

    @Test
    @DisplayName("POST /start should start game when all roles are filled")
    void shouldStartGame() throws Exception {
        // Arrange - create room with all required roles
        String roomId = createRoomWithAllRoles();

        // Act & Assert
        mockMvc.perform(post("/api/rooms/" + roomId + "/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTeam").exists())
                .andExpect(jsonPath("$.phase").value("CLUE"))
                .andExpect(jsonPath("$.board").isArray())
                .andExpect(jsonPath("$.board.length()").value(25))
                .andExpect(jsonPath("$.blueRemaining").exists())
                .andExpect(jsonPath("$.redRemaining").exists());
    }

    @Test
    @DisplayName("POST /start should return 400 when teams not ready")
    void shouldReturnBadRequestWhenTeamsNotReady() throws Exception {
        // Arrange - create room without all roles
        String roomId = createRoomOnly();

        // Act & Assert
        mockMvc.perform(post("/api/rooms/" + roomId + "/start"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /start should return 404 for unknown room")
    void shouldReturnNotFoundForUnknownRoom() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/rooms/NOTEX-ISTS1/start"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    // ========== GET GAME STATE TESTS ==========

    @Test
    @DisplayName("GET /game should return game state after game started")
    void shouldGetGameState() throws Exception {
        // Arrange - create room and start game
        String roomId = createRoomWithAllRoles();
        mockMvc.perform(post("/api/rooms/" + roomId + "/start"));

        // Act & Assert
        mockMvc.perform(get("/api/rooms/" + roomId + "/game"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.board").isArray())
                .andExpect(jsonPath("$.board.length()").value(25))
                .andExpect(jsonPath("$.currentTeam").exists())
                .andExpect(jsonPath("$.phase").exists());
    }

    @Test
    @DisplayName("GET /game should return 404 when game not started")
    void shouldReturnNotFoundWhenGameNotStarted() throws Exception {
        // Arrange - create room without starting game
        String roomId = createRoomOnly();

        // Act & Assert
        mockMvc.perform(get("/api/rooms/" + roomId + "/game"))
                .andExpect(status().isNotFound());
    }

    // ========== HELPER METHODS ==========

    /**
     * Creates a room and returns the room ID.
     */
    private String createRoomOnly() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setUsername("Admin");

        MvcResult result = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("roomId").asText();
    }

    /**
     * Creates a room with all required roles (blue/red spymasters and operatives).
     */
    private String createRoomWithAllRoles() throws Exception {
        // Create room
        String roomId = createRoomOnly();

        // Get admin player ID
        MvcResult roomResult = mockMvc.perform(get("/api/rooms/" + roomId)).andReturn();
        JsonNode roomResponse = objectMapper.readTree(roomResult.getResponse().getContentAsString());
        String adminId = roomResponse.get("players").get(0).get("id").asText();

        // Set admin as blue spymaster
        changePlayerTeam(roomId, adminId, Team.BLUE, Role.SPYMASTER);

        // Join 3 more players and assign roles
        String player2Id = joinAndGetPlayerId(roomId, "Player2");
        changePlayerTeam(roomId, player2Id, Team.BLUE, Role.OPERATIVE);

        String player3Id = joinAndGetPlayerId(roomId, "Player3");
        changePlayerTeam(roomId, player3Id, Team.RED, Role.SPYMASTER);

        String player4Id = joinAndGetPlayerId(roomId, "Player4");
        changePlayerTeam(roomId, player4Id, Team.RED, Role.OPERATIVE);

        return roomId;
    }

    /**
     * Joins a player to a room and returns the player ID.
     */
    private String joinAndGetPlayerId(String roomId, String username) throws Exception {
        JoinRoomRequest request = new JoinRoomRequest();
        request.setUsername(username);

        MvcResult result = mockMvc.perform(post("/api/rooms/" + roomId + "/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        // Find player with matching username
        for (JsonNode player : response.get("players")) {
            if (player.get("username").asText().equals(username)) {
                return player.get("id").asText();
            }
        }
        throw new IllegalStateException("Player not found after join: " + username);
    }

    /**
     * Changes a player's team and role via REST API.
     */
    private void changePlayerTeam(String roomId, String playerId, Team team, Role role)
            throws Exception {
        ChangeTeamRequest request = new ChangeTeamRequest();
        request.setTeam(team);
        request.setRole(role);

        mockMvc.perform(put("/api/rooms/" + roomId + "/players/" + playerId + "/team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
