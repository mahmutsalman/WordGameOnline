package com.codenames.controller;

import com.codenames.dto.CreateRoomRequest;
import com.codenames.dto.JoinRoomRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for RoomController.
 * Tests full request/response cycle with Spring context.
 * Uses MockMvc to simulate HTTP requests.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoomControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ========== CREATE ROOM TESTS ==========

    @Test
    void shouldCreateRoomSuccessfully() throws Exception {
        // Arrange
        CreateRoomRequest request = new CreateRoomRequest();
        request.setUsername("TestPlayer");

        // Act & Assert
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").exists())
                .andExpect(jsonPath("$.roomId").isString())
                .andExpect(jsonPath("$.players").isArray())
                .andExpect(jsonPath("$.players.length()").value(1));
    }

    @Test
    void shouldReturnCreatedStatus() throws Exception {
        // Arrange
        CreateRoomRequest request = new CreateRoomRequest();
        request.setUsername("Player1");

        // Act & Assert
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnRoomIdInResponse() throws Exception {
        // Arrange
        CreateRoomRequest request = new CreateRoomRequest();
        request.setUsername("AdminUser");

        // Act & Assert
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.roomId").exists())
                .andExpect(jsonPath("$.roomId").value(org.hamcrest.Matchers.matchesPattern("[A-Z2-9]{5}-[A-Z2-9]{5}")));
    }

    @Test
    void shouldRejectEmptyUsername() throws Exception {
        // Arrange
        CreateRoomRequest request = new CreateRoomRequest();
        request.setUsername("");

        // Act & Assert
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists());
    }

    @Test
    void shouldRejectUsernameTooLong() throws Exception {
        // Arrange
        CreateRoomRequest request = new CreateRoomRequest();
        request.setUsername("ThisUsernameIsWayTooLongForTheValidation");

        // Act & Assert
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists());
    }

    @Test
    void shouldIncludeAdminPlayerInResponse() throws Exception {
        // Arrange
        CreateRoomRequest request = new CreateRoomRequest();
        request.setUsername("AdminUser");

        // Act & Assert
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.players[0].username").value("AdminUser"))
                .andExpect(jsonPath("$.players[0].admin").value(true))
                .andExpect(jsonPath("$.players[0].role").value("SPECTATOR"));
    }

    // ========== GET ROOM TESTS ==========

    @Test
    void shouldGetExistingRoom() throws Exception {
        // Arrange - Create a room first
        CreateRoomRequest createRequest = new CreateRoomRequest();
        createRequest.setUsername("TestUser");

        MvcResult createResult = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String roomId = objectMapper.readTree(responseBody).get("roomId").asText();

        // Act & Assert - Get the room
        mockMvc.perform(get("/api/rooms/" + roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(roomId))
                .andExpect(jsonPath("$.players").isArray());
    }

    @Test
    void shouldReturnNotFoundForInvalidRoom() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/rooms/INVALID-ROOM"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("INVALID-ROOM")));
    }

    // ========== ROOM EXISTS TESTS ==========

    @Test
    void shouldReturnTrueForExistingRoom() throws Exception {
        // Arrange - Create a room
        CreateRoomRequest createRequest = new CreateRoomRequest();
        createRequest.setUsername("User1");

        MvcResult createResult = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();

        String roomId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("roomId").asText();

        // Act & Assert
        mockMvc.perform(get("/api/rooms/" + roomId + "/exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    void shouldReturnFalseForNonexistentRoom() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/rooms/NONEXISTENT/exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    // ========== JOIN ROOM TESTS ==========

    @Test
    void shouldJoinExistingRoom() throws Exception {
        // Arrange - Create a room
        CreateRoomRequest createRequest = new CreateRoomRequest();
        createRequest.setUsername("Admin");

        MvcResult createResult = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();

        String roomId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("roomId").asText();

        // Arrange - Join request
        JoinRoomRequest joinRequest = new JoinRoomRequest();
        joinRequest.setUsername("Player2");

        // Act & Assert
        mockMvc.perform(post("/api/rooms/" + roomId + "/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(roomId))
                .andExpect(jsonPath("$.players.length()").value(2))
                .andExpect(jsonPath("$.players[1].username").value("Player2"));
    }

    @Test
    void shouldReturnConflictForDuplicateUsername() throws Exception {
        // Arrange - Create a room
        CreateRoomRequest createRequest = new CreateRoomRequest();
        createRequest.setUsername("UniqueUser");

        MvcResult createResult = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();

        String roomId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("roomId").asText();

        // Arrange - Try to join with same username
        JoinRoomRequest joinRequest = new JoinRoomRequest();
        joinRequest.setUsername("UniqueUser");

        // Act & Assert
        mockMvc.perform(post("/api/rooms/" + roomId + "/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("UniqueUser")));
    }

    @Test
    void shouldReturnNotFoundWhenJoiningInvalidRoom() throws Exception {
        // Arrange
        JoinRoomRequest joinRequest = new JoinRoomRequest();
        joinRequest.setUsername("Player1");

        // Act & Assert
        mockMvc.perform(post("/api/rooms/INVALID/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldRejectEmptyUsernameOnJoin() throws Exception {
        // Arrange
        JoinRoomRequest joinRequest = new JoinRoomRequest();
        joinRequest.setUsername("");

        // Act & Assert
        mockMvc.perform(post("/api/rooms/ANYROOM/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists());
    }
}
