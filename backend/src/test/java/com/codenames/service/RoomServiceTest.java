package com.codenames.service;

import com.codenames.dto.response.RoomResponse;
import com.codenames.exception.RoomNotFoundException;
import com.codenames.exception.UsernameAlreadyExistsException;
import com.codenames.factory.RoomFactory;
import com.codenames.model.Player;
import com.codenames.model.Role;
import com.codenames.model.Room;
import com.codenames.model.Team;
import com.codenames.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RoomService.
 * Uses Mockito to mock dependencies (RoomRepository, RoomFactory).
 * Target: 90%+ code coverage.
 */
@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomFactory roomFactory;

    @InjectMocks
    private RoomService roomService;

    // ========== CREATE ROOM TESTS ==========

    @Test
    void shouldCreateRoom() {
        // Arrange
        Room mockRoom = Room.builder()
                .roomId("TEST-ROOM")
                .adminId("player-1")
                .createdAt(LocalDateTime.now())
                .build();

        when(roomFactory.create(anyString(), eq("Player1")))
                .thenReturn(mockRoom);
        when(roomRepository.save(any(Room.class)))
                .thenReturn(mockRoom);

        // Act
        Room result = roomService.createRoom("Player1");

        // Assert
        assertThat(result.getRoomId()).isEqualTo("TEST-ROOM");
        verify(roomFactory).create(anyString(), eq("Player1"));
        verify(roomRepository).save(mockRoom);
    }

    @Test
    void shouldGenerateUniquePlayerIdWhenCreatingRoom() {
        // Arrange
        Room mockRoom = Room.builder().roomId("TEST").build();
        when(roomFactory.create(anyString(), anyString())).thenReturn(mockRoom);
        when(roomRepository.save(any())).thenReturn(mockRoom);

        // Act
        roomService.createRoom("User");

        // Assert
        verify(roomFactory).create(argThat(id -> id != null && !id.isEmpty()), eq("User"));
    }

    @Test
    void shouldSaveRoomToRepository() {
        // Arrange
        Room mockRoom = Room.builder().roomId("TEST").build();
        when(roomFactory.create(anyString(), anyString())).thenReturn(mockRoom);
        when(roomRepository.save(any())).thenReturn(mockRoom);

        // Act
        roomService.createRoom("User");

        // Assert
        verify(roomRepository, times(1)).save(mockRoom);
    }

    @Test
    void shouldReturnCreatedRoom() {
        // Arrange
        Room mockRoom = Room.builder()
                .roomId("CREATED")
                .adminId("admin-1")
                .build();
        when(roomFactory.create(anyString(), anyString())).thenReturn(mockRoom);
        when(roomRepository.save(any())).thenReturn(mockRoom);

        // Act
        Room result = roomService.createRoom("Admin");

        // Assert
        assertThat(result).isEqualTo(mockRoom);
        assertThat(result.getRoomId()).isEqualTo("CREATED");
    }

    // ========== JOIN ROOM TESTS ==========

    @Test
    void shouldJoinRoom() {
        // Arrange
        Player existingPlayer = Player.builder().id("p1").username("User1").build();
        Room mockRoom = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(java.util.List.of(existingPlayer)))
                .build();

        Player newPlayer = Player.builder().id("p2").username("User2").build();

        when(roomRepository.findById("TEST")).thenReturn(Optional.of(mockRoom));
        when(roomFactory.createPlayer("User2")).thenReturn(newPlayer);

        // Act
        Room result = roomService.joinRoom("TEST", "User2");

        // Assert
        assertThat(result.getPlayers())
                .hasSize(2)
                .contains(existingPlayer, newPlayer);
    }

    @Test
    void shouldAddPlayerToRoom() {
        // Arrange
        Room mockRoom = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>())
                .build();
        Player newPlayer = Player.builder().id("p1").username("NewUser").build();

        when(roomRepository.findById("TEST")).thenReturn(Optional.of(mockRoom));
        when(roomFactory.createPlayer("NewUser")).thenReturn(newPlayer);

        // Act
        Room result = roomService.joinRoom("TEST", "NewUser");

        // Assert
        assertThat(result.getPlayers()).contains(newPlayer);
        verify(roomFactory).createPlayer("NewUser");
    }

    @Test
    void shouldThrowWhenJoiningNonexistentRoom() {
        // Arrange
        when(roomRepository.findById("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> roomService.joinRoom("INVALID", "User"))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessageContaining("INVALID");
    }

    @Test
    void shouldThrowWhenUsernameAlreadyExists() {
        // Arrange
        Player existingPlayer = Player.builder()
                .id("p1")
                .username("ExistingUser")
                .build();
        Room mockRoom = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(java.util.List.of(existingPlayer)))
                .build();

        when(roomRepository.findById("TEST")).thenReturn(Optional.of(mockRoom));

        // Act & Assert
        assertThatThrownBy(() -> roomService.joinRoom("TEST", "ExistingUser"))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining("ExistingUser");
    }

    @Test
    void shouldIgnoreCaseWhenCheckingUsername() {
        // Arrange
        Player existingPlayer = Player.builder()
                .id("p1")
                .username("Player1")
                .build();
        Room mockRoom = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(java.util.List.of(existingPlayer)))
                .build();

        when(roomRepository.findById("TEST")).thenReturn(Optional.of(mockRoom));

        // Act & Assert
        assertThatThrownBy(() -> roomService.joinRoom("TEST", "player1"))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        assertThatThrownBy(() -> roomService.joinRoom("TEST", "PLAYER1"))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    // ========== GET ROOM TESTS ==========

    @Test
    void shouldGetRoom() {
        // Arrange
        Room mockRoom = Room.builder().roomId("FOUND").build();
        when(roomRepository.findById("FOUND")).thenReturn(Optional.of(mockRoom));

        // Act
        Room result = roomService.getRoom("FOUND");

        // Assert
        assertThat(result).isEqualTo(mockRoom);
        assertThat(result.getRoomId()).isEqualTo("FOUND");
    }

    @Test
    void shouldThrowWhenGettingNonexistentRoom() {
        // Arrange
        when(roomRepository.findById("NOTFOUND")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> roomService.getRoom("NOTFOUND"))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessageContaining("NOTFOUND");
    }

    // ========== ROOM EXISTS TESTS ==========

    @Test
    void shouldReturnTrueWhenRoomExists() {
        // Arrange
        when(roomRepository.existsById("EXISTS")).thenReturn(true);

        // Act
        boolean result = roomService.roomExists("EXISTS");

        // Assert
        assertThat(result).isTrue();
        verify(roomRepository).existsById("EXISTS");
    }

    @Test
    void shouldReturnFalseWhenRoomDoesNotExist() {
        // Arrange
        when(roomRepository.existsById("NOTEXIST")).thenReturn(false);

        // Act
        boolean result = roomService.roomExists("NOTEXIST");

        // Assert
        assertThat(result).isFalse();
    }

    // ========== LEAVE ROOM TESTS ==========

    @Test
    void shouldRemovePlayerFromRoom() {
        // Arrange
        Player player1 = Player.builder().id("p1").username("User1").build();
        Player player2 = Player.builder().id("p2").username("User2").build();
        Room mockRoom = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(java.util.List.of(player1, player2)))
                .build();

        when(roomRepository.findById("TEST")).thenReturn(Optional.of(mockRoom));

        // Act
        roomService.leaveRoom("TEST", "p1");

        // Assert
        assertThat(mockRoom.getPlayers())
                .hasSize(1)
                .doesNotContain(player1)
                .contains(player2);
    }

    @Test
    void shouldDeleteRoomWhenEmpty() {
        // Arrange
        Player lastPlayer = Player.builder().id("p1").username("LastUser").build();
        Room mockRoom = Room.builder()
                .roomId("EMPTY-SOON")
                .players(new CopyOnWriteArrayList<>(java.util.List.of(lastPlayer)))
                .build();

        when(roomRepository.findById("EMPTY-SOON")).thenReturn(Optional.of(mockRoom));

        // Act
        roomService.leaveRoom("EMPTY-SOON", "p1");

        // Assert
        assertThat(mockRoom.getPlayers()).isEmpty();
        verify(roomRepository).deleteById("EMPTY-SOON");
    }

    @Test
    void shouldNotDeleteRoomWhenPlayersRemain() {
        // Arrange
        Player player1 = Player.builder().id("p1").username("User1").build();
        Player player2 = Player.builder().id("p2").username("User2").build();
        Room mockRoom = Room.builder()
                .roomId("STILL-ACTIVE")
                .players(new CopyOnWriteArrayList<>(java.util.List.of(player1, player2)))
                .build();

        when(roomRepository.findById("STILL-ACTIVE")).thenReturn(Optional.of(mockRoom));

        // Act
        roomService.leaveRoom("STILL-ACTIVE", "p1");

        // Assert
        verify(roomRepository, never()).deleteById("STILL-ACTIVE");
    }

    // ========== TO RESPONSE TESTS ==========

    @Test
    void shouldConvertToRoomResponse() {
        // Arrange
        Player player = Player.builder()
                .id("p1")
                .username("User1")
                .team(Team.BLUE)
                .role(Role.OPERATIVE)
                .connected(true)
                .admin(false)
                .build();

        Room room = Room.builder()
                .roomId("RESPONSE-TEST")
                .adminId("admin-1")
                .players(new CopyOnWriteArrayList<>(java.util.List.of(player)))
                .build();

        // Act
        RoomResponse response = roomService.toResponse(room);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getRoomId()).isEqualTo("RESPONSE-TEST");
        assertThat(response.getAdminId()).isEqualTo("admin-1");
        assertThat(response.getPlayers()).hasSize(1);
    }

    @Test
    void shouldMapAllPlayers() {
        // Arrange
        Player player1 = Player.builder()
                .id("p1")
                .username("User1")
                .team(Team.BLUE)
                .role(Role.SPYMASTER)
                .connected(true)
                .admin(true)
                .build();

        Player player2 = Player.builder()
                .id("p2")
                .username("User2")
                .team(Team.RED)
                .role(Role.OPERATIVE)
                .connected(false)
                .admin(false)
                .build();

        Room room = Room.builder()
                .roomId("MAP-TEST")
                .adminId("p1")
                .players(new CopyOnWriteArrayList<>(java.util.List.of(player1, player2)))
                .build();

        // Act
        RoomResponse response = roomService.toResponse(room);

        // Assert
        assertThat(response.getPlayers()).hasSize(2);

        // Verify first player mapping
        assertThat(response.getPlayers().get(0).getId()).isEqualTo("p1");
        assertThat(response.getPlayers().get(0).getUsername()).isEqualTo("User1");
        assertThat(response.getPlayers().get(0).getTeam()).isEqualTo(Team.BLUE);
        assertThat(response.getPlayers().get(0).getRole()).isEqualTo(Role.SPYMASTER);
        assertThat(response.getPlayers().get(0).isConnected()).isTrue();
        assertThat(response.getPlayers().get(0).isAdmin()).isTrue();

        // Verify second player mapping
        assertThat(response.getPlayers().get(1).getId()).isEqualTo("p2");
        assertThat(response.getPlayers().get(1).getUsername()).isEqualTo("User2");
        assertThat(response.getPlayers().get(1).getTeam()).isEqualTo(Team.RED);
        assertThat(response.getPlayers().get(1).getRole()).isEqualTo(Role.OPERATIVE);
        assertThat(response.getPlayers().get(1).isConnected()).isFalse();
        assertThat(response.getPlayers().get(1).isAdmin()).isFalse();
    }

    // ========== CHANGE_TEAM TESTS ==========

    @Test
    void shouldChangePlayerTeam() {
        // Arrange
        Player player = Player.builder()
                .id("p1")
                .username("Player1")
                .team(null)
                .role(Role.SPECTATOR)
                .build();

        Room mockRoom = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(java.util.List.of(player)))
                .build();

        when(roomRepository.findById("TEST")).thenReturn(Optional.of(mockRoom));

        // Act
        Room result = roomService.changePlayerTeam("TEST", "p1", Team.BLUE, Role.OPERATIVE);

        // Assert
        assertThat(result.getPlayer("p1")).isPresent();
        Player updatedPlayer = result.getPlayer("p1").get();
        assertThat(updatedPlayer.getTeam()).isEqualTo(Team.BLUE);
        assertThat(updatedPlayer.getRole()).isEqualTo(Role.OPERATIVE);
    }

    @Test
    void shouldThrowExceptionIfSpymasterAlreadyExists() {
        // Arrange: Create room with existing Blue Spymaster
        Player spymaster = Player.builder()
                .id("p1")
                .username("BlueSpymaster")
                .team(Team.BLUE)
                .role(Role.SPYMASTER)
                .build();

        Player player2 = Player.builder()
                .id("p2")
                .username("Player2")
                .team(null)
                .role(Role.SPECTATOR)
                .build();

        Room mockRoom = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(java.util.List.of(spymaster, player2)))
                .build();

        when(roomRepository.findById("TEST")).thenReturn(Optional.of(mockRoom));

        // Act & Assert
        assertThatThrownBy(() ->
            roomService.changePlayerTeam("TEST", "p2", Team.BLUE, Role.SPYMASTER)
        )
        .isInstanceOf(com.codenames.exception.SpymasterAlreadyExistsException.class)
        .hasMessageContaining("already has a spymaster");
    }

    @Test
    void shouldAllowNullTeamForSpectator() {
        // Arrange
        Player player = Player.builder()
                .id("p1")
                .username("Player1")
                .team(Team.BLUE)
                .role(Role.OPERATIVE)
                .build();

        Room mockRoom = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(java.util.List.of(player)))
                .build();

        when(roomRepository.findById("TEST")).thenReturn(Optional.of(mockRoom));

        // Act
        Room result = roomService.changePlayerTeam("TEST", "p1", null, Role.SPECTATOR);

        // Assert
        Player updatedPlayer = result.getPlayer("p1").get();
        assertThat(updatedPlayer.getTeam()).isNull();
        assertThat(updatedPlayer.getRole()).isEqualTo(Role.SPECTATOR);
    }

    @Test
    void shouldThrowExceptionIfPlayerNotFound() {
        // Arrange
        Room mockRoom = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>())
                .build();

        when(roomRepository.findById("TEST")).thenReturn(Optional.of(mockRoom));

        // Act & Assert
        assertThatThrownBy(() ->
            roomService.changePlayerTeam("TEST", "non-existent-id", Team.BLUE, Role.OPERATIVE)
        )
        .isInstanceOf(com.codenames.exception.PlayerNotFoundException.class)
        .hasMessageContaining("Player not found");
    }

    // ========== DISCONNECT TESTS ==========

    @Test
    void shouldMarkPlayerDisconnected() {
        // Arrange
        Player player1 = Player.builder()
                .id("p1")
                .username("Player1")
                .connected(true)
                .build();

        Player player2 = Player.builder()
                .id("p2")
                .username("Player2")
                .connected(true)
                .build();

        Room mockRoom = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(java.util.List.of(player1, player2)))
                .build();

        when(roomRepository.findById("TEST")).thenReturn(Optional.of(mockRoom));

        // Act
        roomService.markPlayerDisconnected("TEST", "p1");

        // Assert: Player should be removed from room
        assertThat(mockRoom.getPlayer("p1")).isEmpty();
        assertThat(mockRoom.getPlayers()).hasSize(1);
        assertThat(mockRoom.getPlayers()).contains(player2);
    }

    @Test
    void shouldDeleteRoomWhenLastPlayerDisconnects() {
        // Arrange: Create room with only one player
        Player lastPlayer = Player.builder()
                .id("p1")
                .username("LastPlayer")
                .connected(true)
                .build();

        Room mockRoom = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(java.util.List.of(lastPlayer)))
                .build();

        when(roomRepository.findById("TEST")).thenReturn(Optional.of(mockRoom));

        // Act: Last player disconnects
        roomService.markPlayerDisconnected("TEST", "p1");

        // Assert: Room should be deleted
        verify(roomRepository).deleteById("TEST");
    }

    @Test
    void shouldNotDeleteRoomIfPlayersRemain() {
        // Arrange
        Player player1 = Player.builder()
                .id("p1")
                .username("Player1")
                .connected(true)
                .build();

        Player player2 = Player.builder()
                .id("p2")
                .username("Player2")
                .connected(true)
                .build();

        Room mockRoom = Room.builder()
                .roomId("TEST")
                .players(new CopyOnWriteArrayList<>(java.util.List.of(player1, player2)))
                .build();

        when(roomRepository.findById("TEST")).thenReturn(Optional.of(mockRoom));

        // Act: Player1 disconnects (player2 remains)
        roomService.markPlayerDisconnected("TEST", "p1");

        // Assert: Room should still exist
        verify(roomRepository, never()).deleteById("TEST");
        assertThat(mockRoom.getPlayers()).hasSize(1);
        assertThat(mockRoom.getPlayers()).contains(player2);
    }
}
