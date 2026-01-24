# Step-04: Board Generation API Tests

This document describes the Postman collection for testing the board generation and game start functionality implemented in Step-04.

## Overview

The collection tests the complete game setup flow:
1. Create a room with an admin player
2. Add 3 more players (total of 4)
3. Assign team roles to all players
4. Start the game (triggers board generation)
5. Verify the game state and board distribution

## HTTP vs WebSocket Endpoints

> **Important:** Some endpoints in this collection are HTTP-based for testing/debugging convenience. In production, these operations are performed via WebSocket for real-time synchronization.

| Endpoint | HTTP (Testing) | WebSocket (Production) |
|----------|---------------|------------------------|
| Change Team/Role | `PUT /api/rooms/{roomId}/players/{playerId}/team` | `/app/room/{roomId}/team` |
| Start Game | `POST /api/rooms/{roomId}/start` | WebSocket handler (to be added) |
| Get Game State | `GET /api/rooms/{roomId}/game` | Broadcast via `/topic/room/{roomId}` |

## Collection Variables

| Variable | Description | Set By |
|----------|-------------|--------|
| `baseUrl` | API base URL | Default: `http://localhost:8080` |
| `roomId` | Room ID | Request 1 (Create Room) |
| `player1Id` | First player (admin) ID | Request 1 |
| `player2Id` | Second player ID | Request 2 |
| `player3Id` | Third player ID | Request 3 |
| `player4Id` | Fourth player ID | Request 4 |

## Request Flow

### 1. Create Room
```
POST /api/rooms
Body: { "username": "BlueSpymaster" }
```
Creates a new room. The first player becomes the admin and is saved as `player1Id`.

### 2-4. Join Room (Players 2-4)
```
POST /api/rooms/{roomId}/join
Body: { "username": "BlueOperative" | "RedSpymaster" | "RedOperative" }
```
Adds additional players to the room.

### 5-8. Assign Team Roles
```
PUT /api/rooms/{roomId}/players/{playerId}/team
Body: { "team": "BLUE" | "RED", "role": "SPYMASTER" | "OPERATIVE" }
```

Team assignments:
- Player 1 → Blue Spymaster
- Player 2 → Blue Operative
- Player 3 → Red Spymaster
- Player 4 → Red Operative

After all assignments, `canStart` should be `true`.

### 9. Start Game
```
POST /api/rooms/{roomId}/start
```

Triggers board generation with:
- 25 random words from the word pack
- Card colors assigned (9/8/7/1 distribution)
- Starting team randomly selected

**Expected Response:**
```json
{
  "board": [
    { "word": "APPLE", "color": "BLUE", "revealed": false, "selectedBy": null },
    // ... 24 more cards
  ],
  "currentTeam": "BLUE",
  "phase": "GIVING_CLUE",
  "currentClue": null,
  "guessesRemaining": 0,
  "blueRemaining": 9,
  "redRemaining": 8,
  "winner": null,
  "history": []
}
```

### 10. Get Game State
```
GET /api/rooms/{roomId}/game
```

Retrieves the current game state. For testing, returns the spymaster view (all colors visible).

## Board Distribution Rules

The board follows Codenames rules:
- **Starting team**: 9 cards
- **Other team**: 8 cards
- **Neutral**: 7 cards
- **Assassin**: 1 card
- **Total**: 25 cards

The starting team is randomly selected (BLUE or RED), and that team goes first.

## Test Validations

The collection includes comprehensive tests for:

1. **Card Count**: Exactly 25 cards on the board
2. **Color Distribution**: 9/8/7/1 for starting/other/neutral/assassin
3. **Unique Words**: All 25 words are unique
4. **Initial State**: No cards revealed, no clue, no winner
5. **Remaining Counts**: Match the actual card counts
6. **Error Handling**: Already-started games, non-existent rooms

## Running the Collection

1. Start the backend server:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

2. Import `step-04-part-2-3.json` into Postman

3. Run the collection sequentially (requests depend on previous responses)

4. All tests should pass with green checkmarks

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `canStart` is false after role assignments | Ensure each team has exactly 1 Spymaster and at least 1 Operative |
| 404 on start game | Verify the room exists and `roomId` variable is set |
| 400 on start game | Check if game was already started (run with fresh room) |
| Wrong card distribution | Verify starting team - they should have 9 cards |
