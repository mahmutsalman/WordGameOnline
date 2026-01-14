# Scenario 2: WebSocket Team Management & Disconnect - Testing Guide

**Status**: Ready for Testing
**Date**: 2026-01-14
**Commit**: Commit 4 - PLAYER_LEFT, PLAYER_UPDATED events and disconnect handling

---

## Overview

This scenario tests the new WebSocket functionality implemented in Commit 4:
- **changeTeam** endpoint (`/app/room/{roomId}/team`)
- **PLAYER_UPDATED** event broadcasting
- **PLAYER_LEFT** event on disconnect
- Spymaster validation (only one per team)
- Spectator mode support
- Automatic disconnect handling

---

## Prerequisites

1. **Backend Running**: `mvn spring-boot:run` (on port 8080)
2. **Postman Installed**: Import `Codenames-WebSocket-Tests.postman_collection.json`
3. **WebSocket Client**: Use one of:
   - Postman's WebSocket feature (recommended)
   - Online tool: https://websocketking.com/
   - Browser console with STOMP.js
   - Desktop client: Insomnia, wscat, etc.

---

## Quick Start

### Step 1: Setup Room via REST API

In Postman, navigate to:
```
Integration Scenarios > Scenario 2: WebSocket Team Management & Disconnect
```

**Run these requests in order:**

1. **2.1 Create New Room for WS Tests**
   - Creates a new room with admin "WSAdmin"
   - Saves room ID to collection variable `wsTestRoomId`
   - **Expected**: Status 201, room created

2. **2.2 Add Player via REST**
   - Adds "WSPlayer1" to the room
   - Saves player ID to collection variable `wsPlayer1Id`
   - **Expected**: Status 201, 2 players in room

3. **WebSocket Testing Instructions**
   - GET request that displays full testing guide in description
   - Also retrieves current room state
   - Copy the `wsTestRoomId` from console output

---

### Step 2: Connect WebSocket Client

#### Using Postman WebSocket Feature:

1. Create new **WebSocket Request**
2. URL: `ws://localhost:8080/ws`
3. Protocol: **STOMP over SockJS**
4. Click **Connect**

#### Using Online Tool (websocketking.com):

1. Open https://websocketking.com/
2. URL: `ws://localhost:8080/ws`
3. Select **STOMP** protocol
4. Click **Connect**

---

### Step 3: Subscribe to Events

After connecting, subscribe to these destinations:

**Subscribe 1 - Room Broadcasts:**
```
Destination: /topic/room/{wsTestRoomId}
```
Replace `{wsTestRoomId}` with actual room ID from Step 1.

**Subscribe 2 - Private Messages:**
```
Destination: /user/queue/private
```

---

### Step 4: Test Scenarios

## Test 1: Join Room via WebSocket ✅

**Send Message:**
```json
Destination: /app/room/{wsTestRoomId}/join
Payload: {"username": "WSPlayer2"}
```

**Expected Results:**
- ✅ PLAYER_JOINED broadcast to `/topic/room/{wsTestRoomId}`:
  ```json
  {"type":"PLAYER_JOINED","playerId":"...","username":"WSPlayer2"}
  ```
- ✅ ROOM_STATE sent to `/user/queue/private`:
  ```json
  {"type":"ROOM_STATE","players":[...],"settings":{...},"canStart":false}
  ```

**Verification:**
- Check console log for both events
- Verify player count is now 3 (Admin + Player1 + Player2)

---

## Test 2: Change Team to Blue Operative ✅

**Send Message:**
```json
Destination: /app/room/{wsTestRoomId}/team
Payload: {"team": "BLUE", "role": "OPERATIVE"}
```

**Expected Results:**
- ✅ PLAYER_UPDATED broadcast:
  ```json
  {"type":"PLAYER_UPDATED","playerId":"...","team":"BLUE","role":"OPERATIVE"}
  ```
- ✅ ROOM_STATE broadcast:
  ```json
  {"type":"ROOM_STATE","players":[...],"settings":{...},"canStart":false}
  ```

**Verification:**
- Your player now has team=BLUE, role=OPERATIVE
- All connected clients receive both events

---

## Test 3: Change Team to Blue Spymaster ✅

**Send Message:**
```json
Destination: /app/room/{wsTestRoomId}/team
Payload: {"team": "BLUE", "role": "SPYMASTER"}
```

**Expected Results:**
- ✅ PLAYER_UPDATED broadcast:
  ```json
  {"type":"PLAYER_UPDATED","playerId":"...","team":"BLUE","role":"SPYMASTER"}
  ```
- ✅ ROOM_STATE broadcast with updated player info

**Verification:**
- Your player is now Blue Spymaster
- canStart remains false (need Red team members)

---

## Test 4: Try Duplicate Spymaster (Should Fail) ❌

**Prerequisites:**
- Connect a second WebSocket client
- Have them join as "WSPlayer3"
- Try to make them Blue Spymaster

**Send Message (from WSPlayer3):**
```json
Destination: /app/room/{wsTestRoomId}/team
Payload: {"team": "BLUE", "role": "SPYMASTER"}
```

**Expected Results:**
- ✅ ERROR event sent privately to WSPlayer3:
  ```json
  {"type":"ERROR","message":"Team BLUE already has a spymaster"}
  ```
- ❌ NO PLAYER_UPDATED broadcast (request rejected)

**Verification:**
- Only one Blue Spymaster exists in room
- Error received only by requesting player

---

## Test 5: Become Spectator ✅

**Send Message:**
```json
Destination: /app/room/{wsTestRoomId}/team
Payload: {"role": "SPECTATOR"}
```

**Note:** Omit `team` field or set to null for spectator

**Expected Results:**
- ✅ PLAYER_UPDATED broadcast:
  ```json
  {"type":"PLAYER_UPDATED","playerId":"...","team":null,"role":"SPECTATOR"}
  ```
- ✅ ROOM_STATE broadcast with updated player info

**Verification:**
- Player has team=null, role=SPECTATOR
- Can rejoin a team later if desired

---

## Test 6: Disconnect Handling ✅

**Action:**
- Close one WebSocket connection (WSPlayer2 or WSPlayer3)

**Expected Results:**
- ✅ PLAYER_LEFT broadcast to remaining players:
  ```json
  {"type":"PLAYER_LEFT","playerId":"..."}
  ```
- ✅ Player removed from room
- ✅ ROOM_STATE updated (player count decreased)

**Verification:**
- GET `/api/rooms/{wsTestRoomId}` - player no longer in list
- If last player disconnects, room is deleted (404 on GET)

---

## Test 7: Empty Room Deletion ✅

**Action:**
1. Have all players disconnect
2. Try to GET the room via REST API

**Expected Results:**
- ✅ Room deleted when last player leaves
- ✅ GET returns 404 Not Found

**Verification:**
```bash
curl http://localhost:8080/api/rooms/{wsTestRoomId}
# Should return: {"message":"Room not found: ..."}
```

---

## Event Types Reference

### PLAYER_JOINED
```json
{
  "type": "PLAYER_JOINED",
  "playerId": "uuid",
  "username": "string"
}
```
**Broadcast to:** `/topic/room/{roomId}` (all subscribers)

---

### PLAYER_UPDATED
```json
{
  "type": "PLAYER_UPDATED",
  "playerId": "uuid",
  "team": "BLUE|RED|null",
  "role": "SPYMASTER|OPERATIVE|SPECTATOR"
}
```
**Broadcast to:** `/topic/room/{roomId}` (all subscribers)

---

### PLAYER_LEFT
```json
{
  "type": "PLAYER_LEFT",
  "playerId": "uuid"
}
```
**Broadcast to:** `/topic/room/{roomId}` (all subscribers)

---

### ROOM_STATE
```json
{
  "type": "ROOM_STATE",
  "players": [
    {
      "id": "uuid",
      "username": "string",
      "team": "BLUE|RED|null",
      "role": "SPYMASTER|OPERATIVE|SPECTATOR",
      "connected": true,
      "admin": false
    }
  ],
  "settings": {
    "wordPack": "english"
  },
  "canStart": false
}
```
**Sent to:**
- `/user/queue/private-user{sessionId}` (private, after join)
- `/topic/room/{roomId}` (broadcast, after team changes)

---

### ERROR
```json
{
  "type": "ERROR",
  "message": "Error description"
}
```
**Sent to:** `/user/queue/private-user{sessionId}` (private only)

**Common Errors:**
- "Room not found: {roomId}"
- "Username already taken: {username}"
- "Team {team} already has a spymaster"
- "Player not found: {playerId}"

---

## Success Criteria

### All Tests Must Pass:
- ✅ Join room via WebSocket receives PLAYER_JOINED + ROOM_STATE
- ✅ Change team broadcasts PLAYER_UPDATED + ROOM_STATE
- ✅ Duplicate spymaster rejected with ERROR event
- ✅ Spectator mode works (team=null)
- ✅ Disconnect broadcasts PLAYER_LEFT
- ✅ Empty room deletion works (404 on GET)
- ✅ All events have correct structure and fields

### Integration Tests:
- ✅ 159 backend tests passing (confirmed)
- ✅ WebSocket integration tests pass
- ✅ Real-time synchronization works across multiple clients

---

## Troubleshooting

### WebSocket Connection Fails
**Symptom:** Can't connect to `ws://localhost:8080/ws`

**Solutions:**
- Verify backend is running: `curl http://localhost:8080/actuator/health`
- Check port 8080 is not in use: `lsof -i :8080`
- Try SockJS fallback: `http://localhost:8080/ws/info` should return JSON

### Events Not Received
**Symptom:** Not seeing broadcast events

**Solutions:**
- Verify subscription destination matches exactly: `/topic/room/{roomId}`
- Check STOMP protocol is enabled (not raw WebSocket)
- Ensure sessionId is correct for private messages
- Check console logs for connection errors

### Spymaster Validation Not Working
**Symptom:** Can add multiple spymasters to same team

**Solutions:**
- Verify backend code has spymaster validation in RoomService:718-193
- Check exception is thrown: SpymasterAlreadyExistsException
- Verify error is caught in controller and ERROR event sent

### Room Not Deleted on Disconnect
**Symptom:** Empty room still exists after all players leave

**Solutions:**
- Check WebSocketEventListener is registered as @Component
- Verify SessionDisconnectEvent is being caught
- Check RoomService.markPlayerDisconnected() deletes empty rooms
- Look for errors in backend logs

---

## Additional Testing Tools

### Using cURL (REST only):
```bash
# Create room
curl -X POST http://localhost:8080/api/rooms \
  -H "Content-Type: application/json" \
  -d '{"username":"CurlAdmin"}'

# Get room
curl http://localhost:8080/api/rooms/{roomId}

# Join room
curl -X POST http://localhost:8080/api/rooms/{roomId}/join \
  -H "Content-Type: application/json" \
  -d '{"username":"CurlPlayer"}'
```

### Using wscat (WebSocket CLI):
```bash
# Install
npm install -g wscat

# Connect
wscat -c ws://localhost:8080/ws -s stomp
```

---

## Next Steps

After successful Scenario 2 testing:

1. ✅ Verify all tests pass
2. ✅ Document any issues found
3. ✅ Commit changes (Commit 4)
4. 🚀 **Next**: Frontend implementation (Commits 5-7)
   - WebSocket service with STOMP.js
   - useRoomWebSocket hook
   - RoomPage integration with real-time updates

---

## Test Results Log

**Date**: _____________
**Tester**: _____________

| Test | Status | Notes |
|------|--------|-------|
| Join Room via WS | ⬜ | |
| Change to Operative | ⬜ | |
| Change to Spymaster | ⬜ | |
| Duplicate Spymaster Rejection | ⬜ | |
| Spectator Mode | ⬜ | |
| Disconnect Handling | ⬜ | |
| Empty Room Deletion | ⬜ | |

**Overall Status**: ⬜ PASS / ⬜ FAIL

**Issues Found:**
-
-
-

**Sign-off**: _____________
