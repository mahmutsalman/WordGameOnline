package com.codenames.listener;

import com.codenames.service.RoomService;
import com.codenames.service.WebSocketSessionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    @Mock
    private RoomService roomService;

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketEventListener eventListener;

    @Test
    void shouldHandleDisconnectAndNotifyRoom() {
        // Arrange
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("playerId", "player-1");
        sessionAttributes.put("roomId", "ROOM-123");

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setSessionAttributes(sessionAttributes);

        Message<byte[]> message = new GenericMessage<>("test".getBytes(), accessor.getMessageHeaders());
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "test-session", null);

        // Act
        eventListener.handleDisconnect(event);

        // Assert
        verify(roomService).markPlayerDisconnected("ROOM-123", "player-1");
        verify(sessionManager).removeSession("player-1");
        verify(messagingTemplate).convertAndSend(
                eq("/topic/room/ROOM-123"),
                any(Object.class));
    }

    @Test
    void shouldHandleDisconnectWithMissingAttributes() {
        // Arrange: No session attributes
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setSessionAttributes(null);

        Message<byte[]> message = new GenericMessage<>("test".getBytes(), accessor.getMessageHeaders());
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "test-session", null);

        // Act
        eventListener.handleDisconnect(event);

        // Assert: Should not throw exception, should not call services
        verify(roomService, never()).markPlayerDisconnected(anyString(), anyString());
        verify(sessionManager, never()).removeSession(anyString());
    }

    @Test
    void shouldHandleDisconnectWithMissingPlayerId() {
        // Arrange: Session attributes exist but no playerId
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("roomId", "ROOM-123");
        // playerId is missing

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setSessionAttributes(sessionAttributes);

        Message<byte[]> message = new GenericMessage<>("test".getBytes(), accessor.getMessageHeaders());
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "test-session", null);

        // Act
        eventListener.handleDisconnect(event);

        // Assert: Should not call services
        verify(roomService, never()).markPlayerDisconnected(anyString(), anyString());
        verify(sessionManager, never()).removeSession(anyString());
    }
}
