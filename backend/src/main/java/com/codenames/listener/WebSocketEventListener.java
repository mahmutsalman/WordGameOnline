package com.codenames.listener;

import com.codenames.dto.events.PlayerLeftEvent;
import com.codenames.dto.events.RoomStateEvent;
import com.codenames.model.Room;
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

        String sessionId = headers.getSessionId();
        sessionManager.markSessionDisconnected(sessionId);

        Map<String, Object> attrs = headers.getSessionAttributes();

        String playerId = attrs != null ? (String) attrs.get("playerId") : null;
        String roomId = attrs != null ? (String) attrs.get("roomId") : null;

        // Fallback: resolve playerId/roomId via session manager when session attributes are missing
        if (playerId == null && sessionId != null) {
            playerId = sessionManager.getPlayerIdBySessionId(sessionId);
            if (playerId != null && roomId == null) {
                roomId = sessionManager.getSession(playerId)
                        .map(WebSocketSessionManager.SessionInfo::getRoomId)
                        .orElse(null);
            }
        }

        if (playerId == null || roomId == null) {
            return;
        }

        log.info("Player disconnected: playerId={}, roomId={}", playerId, roomId);

        try {
            roomService.markPlayerDisconnected(roomId, playerId);
            sessionManager.removeSession(playerId);

            // Broadcast disconnect
            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    PlayerLeftEvent.builder().playerId(playerId).build()
            );

            // Broadcast updated room state (includes recalculated canStart)
            Room room = roomService.getRoom(roomId);
            RoomStateEvent roomState = roomService.toRoomState(room);
            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    roomState
            );
            log.debug("Broadcast ROOM_STATE after disconnect: roomId={}, canStart={}",
                    roomId, roomState.isCanStart());
        } catch (Exception e) {
            log.error("Error handling disconnect: playerId={}, roomId={}, error={}",
                    playerId, roomId, e.getMessage());
        }
    }
}
