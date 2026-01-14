package com.codenames.repository;

import com.codenames.model.Room;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RoomRepository.
 * Tests CRUD operations using Spring Data JPA with H2 database.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb"
})
class RoomRepositoryTest {

    @Autowired
    private RoomRepository repository;

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
        assertThat(saved.getRoomId()).isEqualTo(room.getRoomId());
        assertThat(found).isPresent();
        assertThat(found.get().getRoomId()).isEqualTo("TEST-12345");
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
                .createdAt(LocalDateTime.now())
                .build();
        repository.save(room);

        // Act
        Optional<Room> found1 = repository.findById("test-12345");
        Optional<Room> found2 = repository.findById("TEST-12345");

        // Assert
        assertThat(found1).isPresent();
        assertThat(found2).isPresent();
        assertThat(found1.get().getRoomId()).isEqualTo(found2.get().getRoomId());
    }

    @Test
    void shouldCheckIfRoomExists() {
        // Arrange
        Room room = Room.builder()
                .roomId("EXIST")
                .adminId("admin")
                .createdAt(LocalDateTime.now())
                .build();
        repository.save(room);

        // Act & Assert
        assertThat(repository.existsById("EXIST")).isTrue();
        assertThat(repository.existsById("NOTEXIST")).isFalse();
    }

    @Test
    void shouldDeleteRoom() {
        // Arrange
        Room room = Room.builder()
                .roomId("DELETE-ME")
                .adminId("admin")
                .createdAt(LocalDateTime.now())
                .build();
        repository.save(room);

        // Act
        repository.deleteById("DELETE-ME");

        // Assert
        assertThat(repository.existsById("DELETE-ME")).isFalse();
    }

    @Test
    void shouldFindAllRooms() {
        // Arrange
        Room room1 = Room.builder()
                .roomId("ROOM-1")
                .adminId("admin")
                .createdAt(LocalDateTime.now())
                .build();
        Room room2 = Room.builder()
                .roomId("ROOM-2")
                .adminId("admin")
                .createdAt(LocalDateTime.now())
                .build();
        repository.save(room1);
        repository.save(room2);

        // Act
        List<Room> rooms = repository.findAll();

        // Assert
        assertThat(rooms).hasSize(2);
        assertThat(rooms.stream().map(Room::getRoomId))
                .containsExactlyInAnyOrder("ROOM-1", "ROOM-2");
    }

    @Test
    void shouldCountRooms() {
        // Arrange
        repository.save(Room.builder()
                .roomId("R1")
                .adminId("admin")
                .createdAt(LocalDateTime.now())
                .build());
        repository.save(Room.builder()
                .roomId("R2")
                .adminId("admin")
                .createdAt(LocalDateTime.now())
                .build());
        repository.save(Room.builder()
                .roomId("R3")
                .adminId("admin")
                .createdAt(LocalDateTime.now())
                .build());

        // Act
        long count = repository.count();

        // Assert
        assertThat(count).isEqualTo(3);
    }

    @Test
    void shouldOverwriteOnDuplicateSave() {
        // Arrange
        Room room1 = Room.builder()
                .roomId("SAME")
                .adminId("admin1")
                .createdAt(LocalDateTime.now())
                .build();
        Room room2 = Room.builder()
                .roomId("SAME")
                .adminId("admin2")
                .createdAt(LocalDateTime.now())
                .build();

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
        Room room = Room.builder()
                .roomId("lower-case")
                .adminId("admin")
                .createdAt(LocalDateTime.now())
                .build();

        // Act
        repository.save(room);

        // Assert
        assertThat(repository.findById("LOWER-CASE")).isPresent();
        assertThat(repository.findById("lower-case")).isPresent();
    }
}
