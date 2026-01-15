package com.codenames.service;

import com.codenames.model.Player;
import com.codenames.model.Room;
import com.codenames.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Resets persisted player connection flags on server startup.
 *
 * Rooms are persisted for durability across restarts, but WebSocket sessions are not.
 * After a restart, all players should be treated as disconnected until they reconnect.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StartupPlayerConnectionResetter implements ApplicationRunner {

    private final RoomRepository roomRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Room> rooms = roomRepository.findAll();
        if (rooms.isEmpty()) {
            return;
        }

        List<Room> roomsToUpdate = new ArrayList<>();
        int updatedPlayers = 0;

        for (Room room : rooms) {
            boolean roomChanged = false;

            for (Player player : room.getPlayers()) {
                if (player.isConnected()) {
                    player.setConnected(false);
                    roomChanged = true;
                    updatedPlayers++;
                }
            }

            if (roomChanged) {
                roomsToUpdate.add(room);
            }
        }

        if (!roomsToUpdate.isEmpty()) {
            roomRepository.saveAll(roomsToUpdate);
            log.info("Reset player connection flags on startup: roomsUpdated={}, playersUpdated={}",
                    roomsToUpdate.size(), updatedPlayers);
        }
    }
}
