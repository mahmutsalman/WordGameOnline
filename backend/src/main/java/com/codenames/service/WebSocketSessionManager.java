package com.codenames.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe session manager for WebSocket connections.
 * Tracks the mapping between player IDs, session IDs, and room IDs.
 */
@Component
@Slf4j
public class WebSocketSessionManager {

    // playerId -> session info
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    // roomId -> set of playerIds
    private final Map<String, Set<String>> roomPlayers = new ConcurrentHashMap<>();

    // sessionId -> disconnect timestamp (ms)
    private static final long DISCONNECTED_SESSION_TTL_MS = 5 * 60 * 1000L;
    private final Map<String, Long> disconnectedSessions = new ConcurrentHashMap<>();

    /**
     * Session information containing player, room, and WebSocket session IDs.
     */
    @Data
    @AllArgsConstructor
    public static class SessionInfo {
        private String playerId;
        private String roomId;
        private String sessionId;
    }

    /**
     * Register a new WebSocket session for a player.
     *
     * @param playerId  the unique player ID
     * @param roomId    the room ID the player is joining
     * @param sessionId the WebSocket session ID
     */
    public void registerSession(String playerId, String roomId, String sessionId) {
        log.info("Registering session: playerId={}, roomId={}, sessionId={}", playerId, roomId, sessionId);

        // Remove player from old room if they were in one
        SessionInfo oldSession = sessions.get(playerId);
        if (oldSession != null) {
            String oldRoomId = oldSession.getRoomId();
            Set<String> oldRoomPlayerSet = roomPlayers.get(oldRoomId);
            if (oldRoomPlayerSet != null) {
                oldRoomPlayerSet.remove(playerId);
                if (oldRoomPlayerSet.isEmpty()) {
                    roomPlayers.remove(oldRoomId);
                }
            }
        }

        // Register new session
        sessions.put(playerId, new SessionInfo(playerId, roomId, sessionId));
        roomPlayers.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(playerId);
    }

    /**
     * Remove a player's session.
     *
     * @param playerId the player ID to remove
     */
    public void removeSession(String playerId) {
        SessionInfo info = sessions.remove(playerId);
        if (info != null) {
            log.info("Removing session: playerId={}, roomId={}", playerId, info.getRoomId());
            Set<String> players = roomPlayers.get(info.getRoomId());
            if (players != null) {
                players.remove(playerId);
                if (players.isEmpty()) {
                    roomPlayers.remove(info.getRoomId());
                }
            }
        }
    }

    /**
     * Get session information for a player.
     *
     * @param playerId the player ID
     * @return Optional containing SessionInfo if found, empty otherwise
     */
    public Optional<SessionInfo> getSession(String playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    /**
     * Get player ID by WebSocket session ID.
     *
     * @param sessionId the WebSocket session ID
     * @return the player ID if found, null otherwise
     */
    public String getPlayerIdBySessionId(String sessionId) {
        return sessions.values().stream()
                .filter(s -> s.getSessionId().equals(sessionId))
                .map(SessionInfo::getPlayerId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all player IDs in a room.
     *
     * @param roomId the room ID
     * @return Set of player IDs in the room (empty set if room doesn't exist)
     */
    public Set<String> getRoomPlayers(String roomId) {
        return roomPlayers.getOrDefault(roomId, Collections.emptySet());
    }


    /**
     * Mark a session as disconnected.
     * Used to detect disconnect events that can arrive before join/reconnect handlers complete.
     *
     * @param sessionId the WebSocket session ID
     */
    public void markSessionDisconnected(String sessionId) {
        if (sessionId == null) {
            return;
        }

        disconnectedSessions.put(sessionId, System.currentTimeMillis());
        pruneDisconnectedSessions();
    }

    /**
     * Returns true if the session was previously marked disconnected, and removes the marker.
     *
     * @param sessionId the WebSocket session ID
     * @return true if the session was marked disconnected, false otherwise
     */
    public boolean consumeDisconnectedSession(String sessionId) {
        if (sessionId == null) {
            return false;
        }

        pruneDisconnectedSessions();
        return disconnectedSessions.remove(sessionId) != null;
    }

    private void pruneDisconnectedSessions() {
        long cutoff = System.currentTimeMillis() - DISCONNECTED_SESSION_TTL_MS;
        disconnectedSessions.entrySet().removeIf(entry -> entry.getValue() < cutoff);
    }
}
