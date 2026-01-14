package com.codenames.controller;

import com.codenames.dto.events.ErrorEvent;
import com.codenames.dto.events.PlayerJoinedEvent;
import com.codenames.dto.events.PlayerLeftEvent;
import com.codenames.dto.events.PlayerUpdatedEvent;
import com.codenames.dto.request.ChangeTeamRequest;
import com.codenames.dto.request.JoinRoomWsRequest;
import com.codenames.model.Player;
import com.codenames.model.Room;
import com.codenames.service.RoomService;
import com.codenames.service.WebSocketSessionManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller for real-time game communication.
 * Handles player joining, team changes, and broadcasts game events.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class GameWebSocketController {

    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager sessionManager;

    /**
     * Handle player joining room via WebSocket.
     * Broadcasts PLAYER_JOINED to all room subscribers and sends ROOM_STATE privately to joining player.
     *
     * @param roomId the room ID to join
     * @param request the join request containing username
     * @param headerAccessor WebSocket session header accessor
     */
    @MessageMapping("/room/{roomId}/join")
    public void joinRoom(
            @DestinationVariable String roomId,
            @Valid @Payload JoinRoomWsRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        log.info("WebSocket join request: roomId={}, username={}, sessionId={}",
                roomId, request.getUsername(), sessionId);

        try {
            // Join room via service
            Room room = roomService.joinRoom(roomId, request.getUsername());

            // Find the player that just joined
            Player player = room.getPlayers().stream()
                    .filter(p -> p.getUsername().equals(request.getUsername()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Player not found after join"));

            // Track session
            sessionManager.registerSession(player.getId(), roomId, sessionId);

            // Store playerId and roomId in session attributes for disconnect handling
            headerAccessor.getSessionAttributes().put("playerId", player.getId());
            headerAccessor.getSessionAttributes().put("roomId", roomId);

            // Broadcast to all players in room (Observer pattern - broadcast)
            broadcastPlayerJoined(roomId, player);

            // Send current room state to the new player privately
            sendRoomStateToUser(sessionId, room);

        } catch (Exception e) {
            log.error("Join room failed: roomId={}, error={}", roomId, e.getMessage());
            sendErrorToUser(sessionId, e.getMessage());
        }
    }

    /**
     * Handle player changing team/role via WebSocket.
     * Broadcasts PLAYER_UPDATED and ROOM_STATE to all room subscribers.
     *
     * @param roomId the room ID
     * @param request the change team request containing team and role
     * @param headerAccessor WebSocket session header accessor
     */
    @MessageMapping("/room/{roomId}/team")
    public void changeTeam(
            @DestinationVariable String roomId,
            @Valid @Payload ChangeTeamRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        String playerId = (String) headerAccessor.getSessionAttributes().get("playerId");
        log.info("Change team request: roomId={}, playerId={}, team={}, role={}",
                roomId, playerId, request.getTeam(), request.getRole());

        try {
            Room room = roomService.changePlayerTeam(roomId, playerId, request.getTeam(), request.getRole());

            Player player = room.getPlayer(playerId)
                    .orElseThrow(() -> new IllegalStateException("Player not found"));

            // Broadcast update to all
            broadcastPlayerUpdated(roomId, player);
            broadcastRoomState(roomId, room);

        } catch (Exception e) {
            log.error("Change team failed: roomId={}, playerId={}, error={}",
                    roomId, playerId, e.getMessage());
            sendErrorToUser(headerAccessor.getSessionId(), e.getMessage());
        }
    }

    // ========== BROADCAST METHODS ==========

    /**
     * Broadcast a message to all subscribers of a room topic.
     *
     * @param roomId the room ID
     * @param event the event to broadcast
     */
    private void broadcastToRoom(String roomId, Object event) {
        String destination = "/topic/room/" + roomId;
        log.debug("Broadcasting to {}: {}", destination, event);
        messagingTemplate.convertAndSend(destination, event);
    }

    /**
     * Broadcast PLAYER_JOINED event to room.
     *
     * @param roomId the room ID
     * @param player the player that joined
     */
    private void broadcastPlayerJoined(String roomId, Player player) {
        broadcastToRoom(roomId, PlayerJoinedEvent.builder()
                .playerId(player.getId())
                .username(player.getUsername())
                .build());
    }

    /**
     * Broadcast PLAYER_UPDATED event to room.
     *
     * @param roomId the room ID
     * @param player the player that was updated
     */
    private void broadcastPlayerUpdated(String roomId, Player player) {
        broadcastToRoom(roomId, PlayerUpdatedEvent.builder()
                .playerId(player.getId())
                .team(player.getTeam())
                .role(player.getRole())
                .build());
    }

    /**
     * Broadcast PLAYER_LEFT event to room.
     *
     * @param roomId the room ID
     * @param playerId the ID of the player that left
     */
    private void broadcastPlayerLeft(String roomId, String playerId) {
        broadcastToRoom(roomId, PlayerLeftEvent.builder()
                .playerId(playerId)
                .build());
    }

    /**
     * Broadcast ROOM_STATE event to room.
     *
     * @param roomId the room ID
     * @param room the room to send state for
     */
    private void broadcastRoomState(String roomId, Room room) {
        broadcastToRoom(roomId, roomService.toRoomState(room));
    }

    // ========== PRIVATE MESSAGE METHODS ==========

    /**
     * Send a message to a specific user's private queue.
     *
     * @param sessionId the WebSocket session ID
     * @param payload the message payload
     */
    private void sendToUser(String sessionId, Object payload) {
        // Send directly to session-specific destination
        // When client subscribes to /user/queue/private, Spring routes it to /queue/private-user{sessionId}
        String destination = "/queue/private-user" + sessionId;
        log.debug("Sending private message to session {}: destination={}, payload={}",
                sessionId, destination, payload);
        messagingTemplate.convertAndSend(destination, payload);
    }

    /**
     * Send room state to a specific user.
     *
     * @param sessionId the WebSocket session ID
     * @param room the room to send state for
     */
    private void sendRoomStateToUser(String sessionId, Room room) {
        sendToUser(sessionId, roomService.toRoomState(room));
    }

    /**
     * Send error message to a specific user.
     *
     * @param sessionId the WebSocket session ID
     * @param message the error message
     */
    private void sendErrorToUser(String sessionId, String message) {
        sendToUser(sessionId, ErrorEvent.builder().message(message).build());
    }

}
