package com.codenames.controller;

import com.codenames.model.Role;
import com.codenames.model.Room;
import com.codenames.model.Team;
import com.codenames.service.GameService;
import com.codenames.service.RoomService;
import com.fasterxml.jackson.databind.JsonNode;
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

    @Autowired
    private GameService gameService;

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
    void shouldTreatDuplicateUsernameJoinAsReconnect() throws Exception {
        // Arrange: Create room and join with first player (via REST, marked as connected=true)
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

        // Act: Try to join with same username as connected player
        // This is treated as reconnect (e.g., player establishing WebSocket after REST join,
        // or StrictMode race condition)
        session.send("/app/room/" + roomId + "/join", Map.of("username", "ExistingPlayer"));

        // Assert: Should receive ROOM_STATE (treated as reconnect, not error)
        String response = privateMessages.poll(10, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response).contains("\"type\":\"ROOM_STATE\"");
        assertThat(response).contains("ExistingPlayer");
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

        // Consume the ROOM_STATE broadcast that follows PLAYER_JOINED
        String joinRoomState = broadcastMessages.poll(10, TimeUnit.SECONDS);
        assertThat(joinRoomState).contains("ROOM_STATE");

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
        broadcastMessages.poll(10, TimeUnit.SECONDS); // Consume ROOM_STATE broadcast

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

        // Wait for ROOM_STATE from reconnect (broadcast to all)
        String broadcastRoomState = broadcastMessages.poll(10, TimeUnit.SECONDS);
        assertThat(broadcastRoomState).contains("ROOM_STATE");

        // Wait for ROOM_STATE from reconnect (private to reconnecting player)
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
        session2Messages.poll(10, TimeUnit.SECONDS); // Consume ROOM_STATE broadcast

        // Act: Session 1 disconnects
        session1.disconnect();

        // Assert: Session 2 receives PLAYER_LEFT event
        String leftMessage = session2Messages.poll(10, TimeUnit.SECONDS);
        assertThat(leftMessage).isNotNull();
        assertThat(leftMessage).contains("\"type\":\"PLAYER_LEFT\"");
    }

    // ========== GAME START BROADCASTING TESTS (Step-04 Part 5) ==========

    @Test
    void shouldBroadcastGameStateWhenGameStarts() throws Exception {
        // Arrange: Create room with all required roles
        String roomId = createRoomWithAllRoles();

        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> gameMessages = new LinkedBlockingQueue<>();

        // Subscribe to game-specific topic
        session.subscribe("/topic/room/" + roomId + "/game", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = new String((byte[]) payload);
                gameMessages.add(message);
            }
        });

        Thread.sleep(1000); // Ensure subscription is registered

        // Act: Start the game via service (simulates REST API call)
        gameService.startGame(roomId);

        // Assert: Verify GAME_STATE event received
        String gameStateMsg = gameMessages.poll(10, TimeUnit.SECONDS);
        assertThat(gameStateMsg).isNotNull();
        assertThat(gameStateMsg).contains("\"type\":\"GAME_STATE\"");
        assertThat(gameStateMsg).contains("\"board\"");
        assertThat(gameStateMsg).contains("\"currentTeam\"");
        assertThat(gameStateMsg).contains("\"phase\"");
    }

    @Test
    void shouldBroadcastGameStateWithCorrectBoardStructure() throws Exception {
        // Arrange: Create room with all required roles
        String roomId = createRoomWithAllRoles();

        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> gameMessages = new LinkedBlockingQueue<>();

        session.subscribe("/topic/room/" + roomId + "/game", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = new String((byte[]) payload);
                gameMessages.add(message);
            }
        });

        Thread.sleep(1000);

        // Act: Start the game
        gameService.startGame(roomId);

        // Assert: Verify board has 25 cards
        String gameStateMsg = gameMessages.poll(10, TimeUnit.SECONDS);
        assertThat(gameStateMsg).isNotNull();

        JsonNode gameState = objectMapper.readTree(gameStateMsg);
        JsonNode board = gameState.get("board");
        assertThat(board.isArray()).isTrue();
        assertThat(board.size()).isEqualTo(25);

        // Verify each card has required fields
        for (JsonNode card : board) {
            assertThat(card.has("word")).isTrue();
            assertThat(card.has("color")).isTrue();
            assertThat(card.has("revealed")).isTrue();
        }
    }

    @Test
    void shouldBroadcastGameStateWithCorrectCardDistribution() throws Exception {
        // Arrange: Create room with all required roles
        String roomId = createRoomWithAllRoles();

        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> gameMessages = new LinkedBlockingQueue<>();

        session.subscribe("/topic/room/" + roomId + "/game", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = new String((byte[]) payload);
                gameMessages.add(message);
            }
        });

        Thread.sleep(1000);

        // Act: Start the game
        gameService.startGame(roomId);

        // Assert: Verify card distribution (9 starting, 8 other, 7 neutral, 1 assassin)
        String gameStateMsg = gameMessages.poll(10, TimeUnit.SECONDS);
        assertThat(gameStateMsg).isNotNull();

        JsonNode gameState = objectMapper.readTree(gameStateMsg);
        JsonNode board = gameState.get("board");
        String currentTeam = gameState.get("currentTeam").asText();

        int blueCount = 0, redCount = 0, neutralCount = 0, assassinCount = 0;
        for (JsonNode card : board) {
            String color = card.get("color").asText();
            switch (color) {
                case "BLUE" -> blueCount++;
                case "RED" -> redCount++;
                case "NEUTRAL" -> neutralCount++;
                case "ASSASSIN" -> assassinCount++;
            }
        }

        // Starting team has 9, other has 8
        if ("BLUE".equals(currentTeam)) {
            assertThat(blueCount).isEqualTo(9);
            assertThat(redCount).isEqualTo(8);
        } else {
            assertThat(blueCount).isEqualTo(8);
            assertThat(redCount).isEqualTo(9);
        }
        assertThat(neutralCount).isEqualTo(7);
        assertThat(assassinCount).isEqualTo(1);
    }

    @Test
    void shouldBroadcastGameStateWithCorrectRemainingCounts() throws Exception {
        // Arrange: Create room with all required roles
        String roomId = createRoomWithAllRoles();

        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> gameMessages = new LinkedBlockingQueue<>();

        session.subscribe("/topic/room/" + roomId + "/game", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = new String((byte[]) payload);
                gameMessages.add(message);
            }
        });

        Thread.sleep(1000);

        // Act: Start the game
        gameService.startGame(roomId);

        // Assert: Verify remaining counts match card distribution
        String gameStateMsg = gameMessages.poll(10, TimeUnit.SECONDS);
        assertThat(gameStateMsg).isNotNull();

        JsonNode gameState = objectMapper.readTree(gameStateMsg);
        int blueRemaining = gameState.get("blueRemaining").asInt();
        int redRemaining = gameState.get("redRemaining").asInt();
        String currentTeam = gameState.get("currentTeam").asText();

        // Starting team has 9 remaining, other has 8
        if ("BLUE".equals(currentTeam)) {
            assertThat(blueRemaining).isEqualTo(9);
            assertThat(redRemaining).isEqualTo(8);
        } else {
            assertThat(blueRemaining).isEqualTo(8);
            assertThat(redRemaining).isEqualTo(9);
        }
    }

    @Test
    void shouldBroadcastGameStateToAllSubscribers() throws Exception {
        // Arrange: Create room with all required roles
        String roomId = createRoomWithAllRoles();

        StompSession session1 = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
        StompSession session2 = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> messages1 = new LinkedBlockingQueue<>();
        BlockingQueue<String> messages2 = new LinkedBlockingQueue<>();

        // Both subscribe to game topic
        session1.subscribe("/topic/room/" + roomId + "/game", new StompFrameHandler() {
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

        session2.subscribe("/topic/room/" + roomId + "/game", new StompFrameHandler() {
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

        Thread.sleep(1000);

        // Act: Start the game
        gameService.startGame(roomId);

        // Assert: Both clients receive the broadcast
        String msg1 = messages1.poll(10, TimeUnit.SECONDS);
        String msg2 = messages2.poll(10, TimeUnit.SECONDS);

        assertThat(msg1).isNotNull().contains("GAME_STATE");
        assertThat(msg2).isNotNull().contains("GAME_STATE");

        // Verify both received identical game state
        JsonNode state1 = objectMapper.readTree(msg1);
        JsonNode state2 = objectMapper.readTree(msg2);
        assertThat(state1.get("currentTeam")).isEqualTo(state2.get("currentTeam"));
        assertThat(state1.get("blueRemaining")).isEqualTo(state2.get("blueRemaining"));
        assertThat(state1.get("redRemaining")).isEqualTo(state2.get("redRemaining"));
    }

    @Test
    void shouldBroadcastGameStateWithCluePhase() throws Exception {
        // Arrange: Create room with all required roles
        String roomId = createRoomWithAllRoles();

        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> gameMessages = new LinkedBlockingQueue<>();

        session.subscribe("/topic/room/" + roomId + "/game", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = new String((byte[]) payload);
                gameMessages.add(message);
            }
        });

        Thread.sleep(1000);

        // Act: Start the game
        gameService.startGame(roomId);

        // Assert: Phase should be CLUE
        String gameStateMsg = gameMessages.poll(10, TimeUnit.SECONDS);
        assertThat(gameStateMsg).isNotNull();

        JsonNode gameState = objectMapper.readTree(gameStateMsg);
        assertThat(gameState.get("phase").asText()).isEqualTo("CLUE");
        assertThat(gameState.get("winner").isNull()).isTrue();
    }

    // ========== HELPER METHODS ==========

    /**
     * Creates a room with all required roles for game start:
     * - Blue Spymaster, Blue Operative
     * - Red Spymaster, Red Operative
     */
    private String createRoomWithAllRoles() {
        // Create room with admin
        Room room = roomService.createRoom("Admin");
        String roomId = room.getRoomId();
        String adminId = room.getPlayers().get(0).getId();

        // Join 3 more players
        Room afterJoin1 = roomService.joinRoom(roomId, "Player2");
        String player2Id = afterJoin1.getPlayers().stream()
                .filter(p -> "Player2".equals(p.getUsername()))
                .findFirst().orElseThrow().getId();

        Room afterJoin2 = roomService.joinRoom(roomId, "Player3");
        String player3Id = afterJoin2.getPlayers().stream()
                .filter(p -> "Player3".equals(p.getUsername()))
                .findFirst().orElseThrow().getId();

        Room afterJoin3 = roomService.joinRoom(roomId, "Player4");
        String player4Id = afterJoin3.getPlayers().stream()
                .filter(p -> "Player4".equals(p.getUsername()))
                .findFirst().orElseThrow().getId();

        // Assign roles
        roomService.changePlayerTeam(roomId, adminId, Team.BLUE, Role.SPYMASTER);
        roomService.changePlayerTeam(roomId, player2Id, Team.BLUE, Role.OPERATIVE);
        roomService.changePlayerTeam(roomId, player3Id, Team.RED, Role.SPYMASTER);
        roomService.changePlayerTeam(roomId, player4Id, Team.RED, Role.OPERATIVE);

        return roomId;
    }
}
