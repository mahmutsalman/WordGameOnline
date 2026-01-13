package com.codenames.controller;

import com.codenames.model.Room;
import com.codenames.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameWebSocketControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RoomService roomService;

    private WebSocketStompClient stompClient;
    private BlockingQueue<String> messages;
    private String wsUrl;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        messages = new LinkedBlockingQueue<>();
        wsUrl = "ws://localhost:" + port + "/ws";
        objectMapper = new ObjectMapper();

        StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
        stompClient = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(webSocketClient))));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void cleanup() {
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    // ========== PLAYER_JOINED EVENT TESTS ==========

    @Test
    void shouldReceivePlayerJoinedEventWhenPlayerJoinsViaWebSocket() throws Exception {
        // Arrange: Create room via REST API first
        Room room = roomService.createRoom("Admin");
        String roomId = room.getRoomId();

        // Act: Connect via WebSocket
        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        // Subscribe to room topic - use byte[] for payload type
        session.subscribe("/topic/room/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;  // Receive as raw bytes and convert to String
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = new String((byte[]) payload);
                messages.add(message);
            }
        });

        // Small delay to ensure subscription is registered
        Thread.sleep(1000);

        // Send join message
        session.send("/app/room/" + roomId + "/join", Map.of("username", "NewPlayer"));

        // Assert: Verify PLAYER_JOINED event received
        String message = messages.poll(10, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message).contains("\"type\":\"PLAYER_JOINED\"");
        assertThat(message).contains("\"username\":\"NewPlayer\"");
        assertThat(message).contains("\"playerId\"");
    }

    @Test
    void shouldBroadcastPlayerJoinedToAllSubscribers() throws Exception {
        // Arrange: Create room and connect two clients
        Room room = roomService.createRoom("Admin");
        String roomId = room.getRoomId();

        StompSession session1 = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
        StompSession session2 = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> messages1 = new LinkedBlockingQueue<>();
        BlockingQueue<String> messages2 = new LinkedBlockingQueue<>();

        // Both subscribe to room topic
        session1.subscribe("/topic/room/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = new String((byte[]) payload);
                messages1.add(message);
            }
        });

        session2.subscribe("/topic/room/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = new String((byte[]) payload);
                messages2.add(message);
            }
        });

        // Small delay to ensure subscriptions are registered
        Thread.sleep(1000);

        // Act: Session 1 sends join message - Spring converts Map to JSON
        session1.send("/app/room/" + roomId + "/join", Map.of("username", "Player1"));

        // Assert: Both clients receive the broadcast
        String msg1 = messages1.poll(10, TimeUnit.SECONDS);
        String msg2 = messages2.poll(10, TimeUnit.SECONDS);

        assertThat(msg1).isNotNull().contains("PLAYER_JOINED");
        assertThat(msg2).isNotNull().contains("PLAYER_JOINED");
    }

    // ========== ROOM_STATE EVENT TESTS ==========

    @Test
    void shouldReceiveRoomStateEventWhenJoining() throws Exception {
        // Arrange: Create room
        Room room = roomService.createRoom("Admin");
        String roomId = room.getRoomId();

        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> privateMessages = new LinkedBlockingQueue<>();

        // Subscribe to private queue for ROOM_STATE
        session.subscribe("/user/queue/private", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = new String((byte[]) payload);
                privateMessages.add(message);
            }
        });

        // Subscribe to room topic for PLAYER_JOINED
        session.subscribe("/topic/room/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = new String((byte[]) payload);
                messages.add(message);
            }
        });

        // Small delay to ensure subscriptions are registered
        Thread.sleep(1000);

        // Act: Send join message - Spring converts Map to JSON
        session.send("/app/room/" + roomId + "/join", Map.of("username", "TestPlayer"));

        // Assert: Verify ROOM_STATE received in private queue
        String roomStateMsg = privateMessages.poll(10, TimeUnit.SECONDS);
        assertThat(roomStateMsg).isNotNull();
        assertThat(roomStateMsg).contains("\"type\":\"ROOM_STATE\"");
        assertThat(roomStateMsg).contains("\"players\"");
        assertThat(roomStateMsg).contains("\"settings\"");
        assertThat(roomStateMsg).contains("\"canStart\"");

        // Also verify PLAYER_JOINED was broadcast
        String playerJoinedMsg = messages.poll(10, TimeUnit.SECONDS);
        assertThat(playerJoinedMsg).isNotNull();
        assertThat(playerJoinedMsg).contains("PLAYER_JOINED");
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    void shouldReceiveErrorEventForInvalidRoomId() throws Exception {
        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> privateMessages = new LinkedBlockingQueue<>();

        session.subscribe("/user/queue/private", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = new String((byte[]) payload);
                privateMessages.add(message);
            }
        });

        // Small delay to ensure subscription is registered
        Thread.sleep(1000);

        // Act: Try to join non-existent room
        session.send("/app/room/INVALID-ROOM/join", Map.of("username", "TestPlayer"));

        // Assert: Verify ERROR event received
        String errorMsg = privateMessages.poll(10, TimeUnit.SECONDS);
        assertThat(errorMsg).isNotNull();
        assertThat(errorMsg).contains("\"type\":\"ERROR\"");
        assertThat(errorMsg).contains("\"message\"");
    }

    @Test
    void shouldReceiveErrorEventForDuplicateUsername() throws Exception {
        // Arrange: Create room and join with first player
        Room room = roomService.createRoom("Admin");
        String roomId = room.getRoomId();
        roomService.joinRoom(roomId, "ExistingPlayer");

        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> privateMessages = new LinkedBlockingQueue<>();

        session.subscribe("/user/queue/private", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = new String((byte[]) payload);
                privateMessages.add(message);
            }
        });

        // Small delay to ensure subscription is registered
        Thread.sleep(1000);

        // Act: Try to join with duplicate username
        session.send("/app/room/" + roomId + "/join", Map.of("username", "ExistingPlayer"));

        // Assert: Verify ERROR event received
        String errorMsg = privateMessages.poll(10, TimeUnit.SECONDS);
        assertThat(errorMsg).isNotNull();
        assertThat(errorMsg).contains("\"type\":\"ERROR\"");
        assertThat(errorMsg).contains("\"message\"");
    }
}
