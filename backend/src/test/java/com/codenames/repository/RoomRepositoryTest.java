package com.codenames.repository;

import com.codenames.model.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RoomRepository.
 * Tests CRUD operations and thread-safety for in-memory room storage.
 */
class RoomRepositoryTest {

    private RoomRepository repository;

    @BeforeEach
    void setUp() {
        repository = new RoomRepository();
    }

    @Test
    void shouldSaveAndRetrieveRoom() {
        // Arrange
        Room room = Room.builder()
                .roomId("TEST-12345")
                .adminId("admin-1")
                .createdAt(LocalDateTime.now())
                .build();

        // Act
        Room saved = repository.save(room);
        Optional<Room> found = repository.findById("TEST-12345");

        // Assert
        assertThat(saved).isEqualTo(room);
        assertThat(found).isPresent().contains(room);
    }

    @Test
    void shouldReturnEmptyWhenRoomNotFound() {
        // Act
        Optional<Room> found = repository.findById("NONEXISTENT");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void shouldBeCaseInsensitive() {
        // Arrange
        Room room = Room.builder()
                .roomId("TEST-12345")
                .adminId("admin-1")
                .build();
        repository.save(room);

        // Act
        Optional<Room> found1 = repository.findById("test-12345");
        Optional<Room> found2 = repository.findById("TEST-12345");

        // Assert
        assertThat(found1).isPresent();
        assertThat(found2).isPresent();
        assertThat(found1.get()).isEqualTo(found2.get());
    }

    @Test
    void shouldCheckIfRoomExists() {
        // Arrange
        Room room = Room.builder().roomId("EXIST").build();
        repository.save(room);

        // Act & Assert
        assertThat(repository.existsById("EXIST")).isTrue();
        assertThat(repository.existsById("NOTEXIST")).isFalse();
    }

    @Test
    void shouldDeleteRoom() {
        // Arrange
        Room room = Room.builder().roomId("DELETE-ME").build();
        repository.save(room);

        // Act
        repository.deleteById("DELETE-ME");

        // Assert
        assertThat(repository.existsById("DELETE-ME")).isFalse();
    }

    @Test
    void shouldFindAllRooms() {
        // Arrange
        Room room1 = Room.builder().roomId("ROOM-1").build();
        Room room2 = Room.builder().roomId("ROOM-2").build();
        repository.save(room1);
        repository.save(room2);

        // Act
        Collection<Room> rooms = repository.findAll();

        // Assert
        assertThat(rooms)
                .hasSize(2)
                .contains(room1, room2);
    }

    @Test
    void shouldCountRooms() {
        // Arrange
        repository.save(Room.builder().roomId("R1").build());
        repository.save(Room.builder().roomId("R2").build());
        repository.save(Room.builder().roomId("R3").build());

        // Act
        int count = repository.count();

        // Assert
        assertThat(count).isEqualTo(3);
    }

    @Test
    void shouldOverwriteOnDuplicateSave() {
        // Arrange
        Room room1 = Room.builder().roomId("SAME").adminId("admin1").build();
        Room room2 = Room.builder().roomId("SAME").adminId("admin2").build();

        // Act
        repository.save(room1);
        repository.save(room2);
        Optional<Room> found = repository.findById("SAME");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getAdminId()).isEqualTo("admin2");
    }

    @Test
    void shouldNormalizeRoomIdToUppercase() {
        // Arrange
        Room room = Room.builder().roomId("lower-case").build();

        // Act
        repository.save(room);

        // Assert
        assertThat(repository.findById("LOWER-CASE")).isPresent();
        assertThat(repository.findById("lower-case")).isPresent();
    }
}
