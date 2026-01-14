package com.codenames.listener;

import com.codenames.dto.events.PlayerLeftEvent;
import com.codenames.service.RoomService;
import com.codenames.service.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

/**
 * Listens for WebSocket session lifecycle events.
 * Handles player disconnection and broadcasts updates to room.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final RoomService roomService;
    private final WebSocketSessionManager sessionManager;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attrs = headers.getSessionAttributes();

        if (attrs == null) {
            return;
        }

        String playerId = (String) attrs.get("playerId");
        String roomId = (String) attrs.get("roomId");

        if (playerId != null && roomId != null) {
            log.info("Player disconnected: playerId={}, roomId={}", playerId, roomId);

            try {
                roomService.markPlayerDisconnected(roomId, playerId);
                sessionManager.removeSession(playerId);

                // Broadcast disconnect
                messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId,
                        PlayerLeftEvent.builder().playerId(playerId).build()
                );
            } catch (Exception e) {
                log.error("Error handling disconnect: playerId={}, roomId={}, error={}",
                        playerId, roomId, e.getMessage());
            }
        }
    }
}
