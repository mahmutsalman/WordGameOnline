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

    // ========== PLAYER_UPDATED EVENT TESTS ==========

    @Test
    void shouldReceivePlayerUpdatedEventWhenChangingTeam() throws Exception {
        // Arrange: Create room and join with player
        Room room = roomService.createRoom("Admin");
        String roomId = room.getRoomId();

        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> privateMessages = new LinkedBlockingQueue<>();
        BlockingQueue<String> broadcastMessages = new LinkedBlockingQueue<>();

        // Subscribe to private queue for ROOM_STATE after join
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

        // Subscribe to room topic for broadcasts
        session.subscribe("/topic/room/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = new String((byte[]) payload);
                broadcastMessages.add(message);
            }
        });

        Thread.sleep(1000); // Ensure subscriptions are registered

        // Act: Join room first
        session.send("/app/room/" + roomId + "/join", Map.of("username", "Player1"));

        // Wait for join to complete
        String joinBroadcast = broadcastMessages.poll(10, TimeUnit.SECONDS);
        assertThat(joinBroadcast).contains("PLAYER_JOINED");

        String roomState = privateMessages.poll(10, TimeUnit.SECONDS);
        assertThat(roomState).contains("ROOM_STATE");

        // Act: Change team
        session.send("/app/room/" + roomId + "/team",
                Map.of("team", "BLUE", "role", "OPERATIVE"));

        // Assert: Verify PLAYER_UPDATED event received
        String updateMessage = broadcastMessages.poll(10, TimeUnit.SECONDS);
        assertThat(updateMessage).isNotNull();
        assertThat(updateMessage).contains("\"type\":\"PLAYER_UPDATED\"");
        assertThat(updateMessage).contains("\"team\":\"BLUE\"");
        assertThat(updateMessage).contains("\"role\":\"OPERATIVE\"");
    }

    @Test
    void shouldRejectSpymasterIfAlreadyExistsOnTeam() throws Exception {
        // Arrange: Create room with existing Blue Spymaster
        Room room = roomService.createRoom("Admin");
        String roomId = room.getRoomId();
        Room roomWithSpymaster = roomService.joinRoom(roomId, "BlueSpymaster");
        String blueSpymasterId = roomWithSpymaster.getPlayers().stream()
                .filter(p -> "BlueSpymaster".equals(p.getUsername()))
                .findFirst()
                .orElseThrow()
                .getId();
        roomService.changePlayerTeam(roomId,
                blueSpymasterId,
                com.codenames.model.Team.BLUE,
                com.codenames.model.Role.SPYMASTER);

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

        session.subscribe("/topic/room/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // Consume broadcasts
            }
        });

        Thread.sleep(1000);

        // Act: Join as new player
        session.send("/app/room/" + roomId + "/join", Map.of("username", "Player2"));

        // Wait for join to complete and consume ROOM_STATE from join
        String roomStateFromJoin = privateMessages.poll(10, TimeUnit.SECONDS);
        assertThat(roomStateFromJoin).isNotNull();
        assertThat(roomStateFromJoin).contains("\"type\":\"ROOM_STATE\"");

        // Act: Try to become Blue Spymaster (should fail)
        session.send("/app/room/" + roomId + "/team",
                Map.of("team", "BLUE", "role", "SPYMASTER"));

        // Assert: Verify ERROR event received
        String errorMsg = privateMessages.poll(10, TimeUnit.SECONDS);
        assertThat(errorMsg).isNotNull();
        assertThat(errorMsg).contains("\"type\":\"ERROR\"");
        assertThat(errorMsg).contains("already has a spymaster");
    }

    @Test
    void shouldAllowSpectatorMode() throws Exception {
        // Test that player can become spectator (team = null, role = SPECTATOR)
        Room room = roomService.createRoom("Admin");
        String roomId = room.getRoomId();

        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> broadcastMessages = new LinkedBlockingQueue<>();

        session.subscribe("/user/queue/private", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // Consume private messages
            }
        });

        session.subscribe("/topic/room/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = new String((byte[]) payload);
                broadcastMessages.add(message);
            }
        });

        Thread.sleep(1000);

        // Join room
        session.send("/app/room/" + roomId + "/join", Map.of("username", "Player1"));
        broadcastMessages.poll(10, TimeUnit.SECONDS); // Consume PLAYER_JOINED

        // Act: Become spectator
        session.send("/app/room/" + roomId + "/team",
                Map.of("role", "SPECTATOR")); // team = null (omitted)

        // Assert: Verify PLAYER_UPDATED with null team
        String updateMessage = broadcastMessages.poll(10, TimeUnit.SECONDS);
        assertThat(updateMessage).isNotNull();
        assertThat(updateMessage).contains("\"type\":\"PLAYER_UPDATED\"");
        assertThat(updateMessage).contains("\"role\":\"SPECTATOR\"");
    }

    // ========== RECONNECT PLAYER TESTS ==========

    @Test
    void shouldReconnectExistingPlayerAndStoreSessionPlayerId() throws Exception {
        // Arrange: Create room via REST API (simulates creator flow)
        Room room = roomService.createRoom("Admin");
        String roomId = room.getRoomId();
        String playerId = room.getPlayers().get(0).getId(); // Admin player ID

        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> privateMessages = new LinkedBlockingQueue<>();

        // Subscribe to private queue to receive ROOM_STATE after reconnect
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

        Thread.sleep(1000); // Ensure subscription is registered

        // Act: Send reconnect request
        session.send("/app/room/" + roomId + "/reconnect",
                Map.of("playerId", playerId));

        // Assert: Should receive ROOM_STATE
        String message = privateMessages.poll(10, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message).contains("\"type\":\"ROOM_STATE\"");
        assertThat(message).contains("\"players\"");
        assertThat(message).contains("\"settings\"");
        assertThat(message).contains("\"canStart\"");
    }

    @Test
    void shouldRejectReconnectForNonExistentPlayer() throws Exception {
        // Arrange: Create room
        Room room = roomService.createRoom("Admin");
        String roomId = room.getRoomId();

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

        Thread.sleep(1000);

        // Act: Try to reconnect with invalid playerId
        session.send("/app/room/" + roomId + "/reconnect",
                Map.of("playerId", "INVALID_PLAYER_ID"));

        // Assert: Should receive ERROR
        String errorMsg = privateMessages.poll(10, TimeUnit.SECONDS);
        assertThat(errorMsg).isNotNull();
        assertThat(errorMsg).contains("\"type\":\"ERROR\"");
        assertThat(errorMsg).contains("Player not found");
    }

    @Test
    void shouldAllowTeamChangeAfterReconnect() throws Exception {
        // Arrange: Create room via REST API (simulates creator flow)
        Room room = roomService.createRoom("Admin");
        String roomId = room.getRoomId();
        String playerId = room.getPlayers().get(0).getId();

        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> privateMessages = new LinkedBlockingQueue<>();
        BlockingQueue<String> broadcastMessages = new LinkedBlockingQueue<>();

        // Subscribe to private queue
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

        // Subscribe to room topic for broadcasts
        session.subscribe("/topic/room/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = new String((byte[]) payload);
                broadcastMessages.add(message);
            }
        });

        Thread.sleep(1000);

        // Act: Reconnect first
        session.send("/app/room/" + roomId + "/reconnect",
                Map.of("playerId", playerId));

        // Wait for ROOM_STATE from reconnect
        String roomState = privateMessages.poll(10, TimeUnit.SECONDS);
        assertThat(roomState).contains("ROOM_STATE");

        // Act: Change team (should work now because playerId is in session)
        session.send("/app/room/" + roomId + "/team",
                Map.of("team", "BLUE", "role", "OPERATIVE"));

        // Assert: Should receive PLAYER_UPDATED broadcast
        String updateMessage = broadcastMessages.poll(10, TimeUnit.SECONDS);
        assertThat(updateMessage).isNotNull();
        assertThat(updateMessage).contains("\"type\":\"PLAYER_UPDATED\"");
        assertThat(updateMessage).contains("\"team\":\"BLUE\"");
        assertThat(updateMessage).contains("\"role\":\"OPERATIVE\"");
    }

    // ========== DISCONNECT HANDLING TESTS ==========

    @Test
    void shouldBroadcastPlayerLeftEventOnDisconnect() throws Exception {
        // Arrange: Create room with two players
        Room room = roomService.createRoom("Admin");
        String roomId = room.getRoomId();

        StompSession session1 = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
        StompSession session2 = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> session2Messages = new LinkedBlockingQueue<>();

        // Session 2 subscribes to room
        session2.subscribe("/topic/room/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = new String((byte[]) payload);
                session2Messages.add(message);
            }
        });

        session1.subscribe("/topic/room/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // Consume
            }
        });

        session1.subscribe("/user/queue/private", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // Consume
            }
        });

        Thread.sleep(1000);

        // Both join
        session1.send("/app/room/" + roomId + "/join", Map.of("username", "Player1"));
        Thread.sleep(500);
        session2Messages.poll(10, TimeUnit.SECONDS); // Consume Player1 joined

        // Act: Session 1 disconnects
        session1.disconnect();

        // Assert: Session 2 receives PLAYER_LEFT event
        String leftMessage = session2Messages.poll(10, TimeUnit.SECONDS);
        assertThat(leftMessage).isNotNull();
        assertThat(leftMessage).contains("\"type\":\"PLAYER_LEFT\"");
    }
}
