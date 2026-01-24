package com.codenames.service;

import com.codenames.exception.GameStartException;
import com.codenames.exception.RoomNotFoundException;
import com.codenames.model.Card;
import com.codenames.model.CardColor;
import com.codenames.model.GamePhase;
import com.codenames.model.GameState;
import com.codenames.model.Player;
import com.codenames.model.Role;
import com.codenames.model.Room;
import com.codenames.model.Team;
import com.codenames.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GameService.
 * Tests game orchestration including start game logic.
 */
@DisplayName("GameService")
@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BoardGenerationService boardGenerationService;

    @InjectMocks
    private GameService gameService;

    private Room testRoom;
    private GameState mockGameState;

    @BeforeEach
    void setUp() {
        // Create a valid room with all required roles
        testRoom = Room.builder()
                .roomId("TEST1-ROOM1")
                .adminId("admin-id")
                .createdAt(LocalDateTime.now())
                .players(new ArrayList<>())
                .build();

        // Add required players
        testRoom.getPlayers().add(createPlayer("player1", Team.BLUE, Role.SPYMASTER, true));
        testRoom.getPlayers().add(createPlayer("player2", Team.BLUE, Role.OPERATIVE, true));
        testRoom.getPlayers().add(createPlayer("player3", Team.RED, Role.SPYMASTER, true));
        testRoom.getPlayers().add(createPlayer("player4", Team.RED, Role.OPERATIVE, true));

        // Create mock game state
        List<Card> mockBoard = IntStream.range(0, 25)
                .mapToObj(i -> Card.builder()
                        .word("WORD" + i)
                        .color(CardColor.NEUTRAL)
                        .revealed(false)
                        .build())
                .toList();

        mockGameState = GameState.builder()
                .board(mockBoard)
                .currentTeam(Team.BLUE)
                .phase(GamePhase.CLUE)
                .blueRemaining(9)
                .redRemaining(8)
                .history(new ArrayList<>())
                .build();
    }

    private Player createPlayer(String username, Team team, Role role, boolean connected) {
        return Player.builder()
                .id(username + "-id")
                .username(username)
                .team(team)
                .role(role)
                .connected(connected)
                .build();
    }

    @Nested
    @DisplayName("startGame")
    class StartGame {

        @Test
        @DisplayName("should start game successfully with valid room")
        void shouldStartGameSuccessfully() {
            String roomId = "START-SUCC1";
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
            when(boardGenerationService.initializeGame(eq("english"), any(Team.class)))
                    .thenReturn(mockGameState);

            GameState result = gameService.startGame(roomId);

            assertThat(result).isNotNull();
            assertThat(result.getBoard()).hasSize(25);
            assertThat(testRoom.getGameState()).isEqualTo(result);
        }

        @Test
        @DisplayName("should throw exception when room not found")
        void shouldThrowExceptionWhenRoomNotFound() {
            when(roomRepository.findById("NOTFOUND")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gameService.startGame("NOTFOUND"))
                    .isInstanceOf(RoomNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when missing blue spymaster")
        void shouldThrowExceptionWhenMissingBlueSpymaster() {
            testRoom.getPlayers().removeIf(p ->
                    p.getTeam() == Team.BLUE && p.getRole() == Role.SPYMASTER);
            when(roomRepository.findById("TEST1-ROOM1")).thenReturn(Optional.of(testRoom));

            assertThatThrownBy(() -> gameService.startGame("TEST1-ROOM1"))
                    .isInstanceOf(GameStartException.class)
                    .hasMessageContaining("cannot start");
        }

        @Test
        @DisplayName("should throw exception when missing red spymaster")
        void shouldThrowExceptionWhenMissingRedSpymaster() {
            testRoom.getPlayers().removeIf(p ->
                    p.getTeam() == Team.RED && p.getRole() == Role.SPYMASTER);
            when(roomRepository.findById("TEST1-ROOM1")).thenReturn(Optional.of(testRoom));

            assertThatThrownBy(() -> gameService.startGame("TEST1-ROOM1"))
                    .isInstanceOf(GameStartException.class);
        }

        @Test
        @DisplayName("should throw exception when missing blue operative")
        void shouldThrowExceptionWhenMissingBlueOperative() {
            testRoom.getPlayers().removeIf(p ->
                    p.getTeam() == Team.BLUE && p.getRole() == Role.OPERATIVE);
            when(roomRepository.findById("TEST1-ROOM1")).thenReturn(Optional.of(testRoom));

            assertThatThrownBy(() -> gameService.startGame("TEST1-ROOM1"))
                    .isInstanceOf(GameStartException.class);
        }

        @Test
        @DisplayName("should throw exception when missing red operative")
        void shouldThrowExceptionWhenMissingRedOperative() {
            testRoom.getPlayers().removeIf(p ->
                    p.getTeam() == Team.RED && p.getRole() == Role.OPERATIVE);
            when(roomRepository.findById("TEST1-ROOM1")).thenReturn(Optional.of(testRoom));

            assertThatThrownBy(() -> gameService.startGame("TEST1-ROOM1"))
                    .isInstanceOf(GameStartException.class);
        }

        @Test
        @DisplayName("should not count disconnected players for validation")
        void shouldNotCountDisconnectedPlayers() {
            // Disconnect the blue spymaster
            testRoom.getPlayers().stream()
                    .filter(p -> p.getTeam() == Team.BLUE && p.getRole() == Role.SPYMASTER)
                    .findFirst()
                    .ifPresent(p -> p.setConnected(false));

            when(roomRepository.findById("TEST1-ROOM1")).thenReturn(Optional.of(testRoom));

            assertThatThrownBy(() -> gameService.startGame("TEST1-ROOM1"))
                    .isInstanceOf(GameStartException.class);
        }

        @Test
        @DisplayName("should use english word pack by default")
        void shouldUseEnglishWordPackByDefault() {
            String roomId = "ENGLS-PACK1";
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
            when(boardGenerationService.initializeGame(eq("english"), any(Team.class)))
                    .thenReturn(mockGameState);

            gameService.startGame(roomId);

            verify(boardGenerationService).initializeGame(eq("english"), any(Team.class));
        }

        @Test
        @DisplayName("should randomly select starting team")
        void shouldRandomlySelectStartingTeam() {
            // Track which team was selected for each game
            List<Team> selectedTeams = new ArrayList<>();

            when(boardGenerationService.initializeGame(eq("english"), any(Team.class)))
                    .thenAnswer(invocation -> {
                        Team selectedTeam = invocation.getArgument(1);
                        selectedTeams.add(selectedTeam);
                        return mockGameState;
                    });

            // Run multiple times with unique room IDs to verify randomness
            for (int i = 0; i < 100; i++) {
                String uniqueRoomId = "ROOM" + i + "-TEST1";
                Room uniqueRoom = Room.builder()
                        .roomId(uniqueRoomId)
                        .adminId("admin-id")
                        .createdAt(LocalDateTime.now())
                        .players(new ArrayList<>(testRoom.getPlayers()))
                        .build();

                when(roomRepository.findById(uniqueRoomId)).thenReturn(Optional.of(uniqueRoom));
                gameService.startGame(uniqueRoomId);
            }

            // Count blue and red starts
            long blueStarts = selectedTeams.stream().filter(t -> t == Team.BLUE).count();
            long redStarts = selectedTeams.stream().filter(t -> t == Team.RED).count();

            // With randomness, both should have at least some starts
            // Note: This test is probabilistic but extremely unlikely to fail
            assertThat(blueStarts + redStarts).isEqualTo(100);
            assertThat(blueStarts).isGreaterThan(0);
            assertThat(redStarts).isGreaterThan(0);
        }

        @Test
        @DisplayName("should store game state in room")
        void shouldStoreGameStateInRoom() {
            when(roomRepository.findById("STORE-ROOM1")).thenReturn(Optional.of(testRoom));
            when(boardGenerationService.initializeGame(eq("english"), any(Team.class)))
                    .thenReturn(mockGameState);

            GameState result = gameService.startGame("STORE-ROOM1");

            assertThat(testRoom.getGameState()).isSameAs(result);
        }

        @Test
        @DisplayName("should throw exception when game already started")
        void shouldThrowExceptionWhenGameAlreadyStarted() {
            String roomId = "ALRDY-START";
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
            when(boardGenerationService.initializeGame(eq("english"), any(Team.class)))
                    .thenReturn(mockGameState);

            // Start the game first time - should succeed
            gameService.startGame(roomId);

            // Start the game second time - should throw exception
            assertThatThrownBy(() -> gameService.startGame(roomId))
                    .isInstanceOf(GameStartException.class)
                    .hasMessageContaining("already started");
        }
    }

    @Nested
    @DisplayName("getGameState")
    class GetGameState {

        @Test
        @DisplayName("should return game state when game is in progress")
        void shouldReturnGameStateWhenInProgress() {
            String roomId = "GETST-PROG1";
            // First start the game to store state in memory
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
            when(boardGenerationService.initializeGame(eq("english"), any(Team.class)))
                    .thenReturn(mockGameState);
            gameService.startGame(roomId);

            // Now get the game state
            when(roomRepository.existsById(roomId)).thenReturn(true);
            GameState result = gameService.getGameState(roomId);

            assertThat(result).isEqualTo(mockGameState);
        }

        @Test
        @DisplayName("should return null when game not started")
        void shouldReturnNullWhenGameNotStarted() {
            String roomId = "GETST-NULL1";
            when(roomRepository.existsById(roomId)).thenReturn(true);

            GameState result = gameService.getGameState(roomId);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should throw exception when room not found")
        void shouldThrowExceptionWhenRoomNotFound() {
            when(roomRepository.existsById("NOTFOUND")).thenReturn(false);

            assertThatThrownBy(() -> gameService.getGameState("NOTFOUND"))
                    .isInstanceOf(RoomNotFoundException.class);
        }
    }
}
