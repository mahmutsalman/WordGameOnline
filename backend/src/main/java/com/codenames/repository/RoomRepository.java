package com.codenames.repository;

import com.codenames.model.Room;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository for in-memory room storage.
 * Uses ConcurrentHashMap for thread-safe operations.
 * Room IDs are normalized to uppercase for case-insensitive lookups.
 */
@Repository
public class RoomRepository {

    /**
     * Thread-safe map storing rooms by ID.
     * Keys are uppercase room IDs.
     */
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    /**
     * Saves a room to the repository.
     * Room ID is normalized to uppercase.
     *
     * @param room the room to save
     * @return the saved room
     */
    public Room save(Room room) {
        rooms.put(room.getRoomId().toUpperCase(), room);
        return room;
    }

    /**
     * Finds a room by ID (case-insensitive).
     *
     * @param roomId the room ID to search for
     * @return Optional containing the room if found, empty otherwise
     */
    public Optional<Room> findById(String roomId) {
        return Optional.ofNullable(rooms.get(roomId.toUpperCase()));
    }

    /**
     * Checks if a room exists by ID (case-insensitive).
     *
     * @param roomId the room ID to check
     * @return true if room exists, false otherwise
     */
    public boolean existsById(String roomId) {
        return rooms.containsKey(roomId.toUpperCase());
    }

    /**
     * Deletes a room by ID (case-insensitive).
     *
     * @param roomId the room ID to delete
     */
    public void deleteById(String roomId) {
        rooms.remove(roomId.toUpperCase());
    }

    /**
     * Returns all rooms in the repository.
     *
     * @return collection of all rooms
     */
    public Collection<Room> findAll() {
        return rooms.values();
    }

    /**
     * Returns the total number of rooms.
     *
     * @return room count
     */
    public int count() {
        return rooms.size();
    }
}
