package com.codenames.repository;

import com.codenames.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Room entity persistence using Spring Data JPA.
 * Backed by H2 database for durability across application restarts.
 * Room IDs are case-insensitive for lookups.
 *
 * Spring Data JPA automatically implements:
 * - save(Room room)
 * - findById(String roomId)
 * - existsById(String roomId)
 * - deleteById(String roomId)
 * - findAll()
 * - count()
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, String> {

    /**
     * Finds a room by ID (case-insensitive).
     * Overrides default findById to support case-insensitive lookup.
     *
     * @param roomId the room ID to search for
     * @return Optional containing the room if found, empty otherwise
     */
    @Query("SELECT r FROM Room r WHERE UPPER(r.roomId) = UPPER(:roomId)")
    Optional<Room> findById(@Param("roomId") String roomId);

    /**
     * Checks if a room exists by ID (case-insensitive).
     * Overrides default existsById to support case-insensitive lookup.
     *
     * @param roomId the room ID to check
     * @return true if room exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Room r WHERE UPPER(r.roomId) = UPPER(:roomId)")
    boolean existsById(@Param("roomId") String roomId);
}
