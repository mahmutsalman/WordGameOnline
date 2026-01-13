# WebSocket Testing Guide for Codenames Game

This guide provides comprehensive instructions for testing the WebSocket functionality of the Codenames game backend.

## Table of Contents
1. [Quick Start with Postman](#quick-start-with-postman)
2. [WebSocket Testing Tools](#websocket-testing-tools)
3. [Test Scenarios](#test-scenarios)
4. [Message Formats](#message-formats)
5. [Troubleshooting](#troubleshooting)

---

## Quick Start with Postman

### Prerequisites
1. Import the `Codenames-WebSocket-Tests.postman_collection.json` into Postman
2. Ensure backend is running on `http://localhost:8080`
3. Run the "REST API Tests" folder to create a room and get a room ID

### Running Tests

**Option 1: Run Full Collection**
```
1. Click "Run collection" button
2. Select all folders
3. Click "Run Codenames - WebSocket Tests"
4. View results with all assertions
```

**Option 2: Run Individual Requests**
```
1. Expand "REST API Tests" folder
2. Run requests in sequence:
   - Create Room (Admin)
   - Get Room Details
   - Join Room (Player 2)
   - Test error scenarios
```

**Option 3: Run Integration Scenario**
```
1. Expand "Integration Scenarios" > "Scenario 1: Complete Room Flow"
2. Run all requests in sequence
3. Verify 5 players in final room state
```

---

## WebSocket Testing Tools

### Option 1: Postman WebSocket Request (Recommended for Quick Tests)

**Step 1: Create WebSocket Connection**
```
1. In Postman, click "New" > "WebSocket Request"
2. Enter URL: ws://localhost:8080/ws
3. Click "Connect"
```

**Step 2: Send STOMP Messages**

Postman supports raw WebSocket, but for STOMP you need to use a STOMP client library or browser-based tools.

### Option 2: Browser-Based STOMP Client (Easiest for Full Testing)

Create an HTML file (`websocket-test.html`) and open in browser:

```html
<!DOCTYPE html>
<html>
<head>
    <title>Codenames WebSocket Tester</title>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .container { max-width: 800px; margin: 0 auto; }
        .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        input, button { margin: 5px; padding: 8px; }
        button { background: #007bff; color: white; border: none; cursor: pointer; border-radius: 3px; }
        button:hover { background: #0056b3; }
        .log { background: #f8f9fa; padding: 10px; margin-top: 10px; height: 200px; overflow-y: auto; font-family: monospace; font-size: 12px; }
        .success { color: green; }
        .error { color: red; }
        .info { color: blue; }
    </style>
</head>
<body>
    <div class="container">
        <h1>Codenames WebSocket Tester</h1>

        <div class="section">
            <h2>Connection</h2>
            <input type="text" id="wsUrl" value="http://localhost:8080/ws" style="width: 300px;">
            <button onclick="connect()">Connect</button>
            <button onclick="disconnect()">Disconnect</button>
            <div id="connectionStatus">Status: Disconnected</div>
        </div>

        <div class="section">
            <h2>Test Actions</h2>
            <div>
                <strong>Room ID:</strong>
                <input type="text" id="roomId" placeholder="Enter Room ID" style="width: 200px;">
                <button onclick="createRoom()">Create Room via REST</button>
            </div>
            <div>
                <strong>Username:</strong>
                <input type="text" id="username" placeholder="Enter Username" value="WebSocketPlayer">
            </div>
            <div>
                <button onclick="subscribeToRoom()">Subscribe to Room</button>
                <button onclick="subscribeToPrivate()">Subscribe to Private Queue</button>
                <button onclick="joinRoom()">Send Join Message</button>
            </div>
            <div>
                <button onclick="testInvalidRoom()">Test Invalid Room</button>
                <button onclick="testDuplicateUsername()">Test Duplicate Username</button>
            </div>
        </div>

        <div class="section">
            <h2>Message Log</h2>
            <button onclick="clearLog()">Clear Log</button>
            <div id="log" class="log"></div>
        </div>
    </div>

    <script>
        let stompClient = null;
        let currentRoomId = null;

        function log(message, type = 'info') {
            const logDiv = document.getElementById('log');
            const timestamp = new Date().toLocaleTimeString();
            const className = type === 'error' ? 'error' : type === 'success' ? 'success' : 'info';
            logDiv.innerHTML += `<div class="${className}">[${timestamp}] ${message}</div>`;
            logDiv.scrollTop = logDiv.scrollHeight;
        }

        function clearLog() {
            document.getElementById('log').innerHTML = '';
        }

        function connect() {
            const wsUrl = document.getElementById('wsUrl').value;
            const socket = new SockJS(wsUrl);
            stompClient = Stomp.over(socket);

            stompClient.connect({}, function(frame) {
                document.getElementById('connectionStatus').innerHTML =
                    '<span class="success">Status: Connected</span>';
                log('✅ Connected to WebSocket', 'success');
                log('Session ID: ' + stompClient.ws._transport.url.split('/')[5], 'info');
            }, function(error) {
                document.getElementById('connectionStatus').innerHTML =
                    '<span class="error">Status: Connection Failed</span>';
                log('❌ Connection error: ' + error, 'error');
            });
        }

        function disconnect() {
            if (stompClient !== null) {
                stompClient.disconnect();
                document.getElementById('connectionStatus').innerHTML = 'Status: Disconnected';
                log('Disconnected from WebSocket', 'info');
            }
        }

        async function createRoom() {
            try {
                const response = await fetch('http://localhost:8080/api/rooms', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username: 'AdminUser' })
                });
                const data = await response.json();
                currentRoomId = data.roomId;
                document.getElementById('roomId').value = data.roomId;
                log('✅ Room created: ' + data.roomId, 'success');
                log('Admin ID: ' + data.players[0].id, 'info');
            } catch (error) {
                log('❌ Failed to create room: ' + error, 'error');
            }
        }

        function subscribeToRoom() {
            const roomId = document.getElementById('roomId').value || currentRoomId;
            if (!roomId) {
                log('❌ Please enter a Room ID first', 'error');
                return;
            }
            if (!stompClient || !stompClient.connected) {
                log('❌ Not connected to WebSocket', 'error');
                return;
            }

            stompClient.subscribe('/topic/room/' + roomId, function(message) {
                log('📨 BROADCAST MESSAGE received:', 'success');
                const parsed = JSON.parse(message.body);
                log(JSON.stringify(parsed, null, 2), 'info');
            });
            log('✅ Subscribed to /topic/room/' + roomId, 'success');
        }

        function subscribeToPrivate() {
            if (!stompClient || !stompClient.connected) {
                log('❌ Not connected to WebSocket', 'error');
                return;
            }

            stompClient.subscribe('/user/queue/private', function(message) {
                log('📬 PRIVATE MESSAGE received:', 'success');
                const parsed = JSON.parse(message.body);
                log(JSON.stringify(parsed, null, 2), 'info');
            });
            log('✅ Subscribed to /user/queue/private', 'success');
        }

        function joinRoom() {
            const roomId = document.getElementById('roomId').value || currentRoomId;
            const username = document.getElementById('username').value;

            if (!roomId || !username) {
                log('❌ Please enter Room ID and Username', 'error');
                return;
            }
            if (!stompClient || !stompClient.connected) {
                log('❌ Not connected to WebSocket', 'error');
                return;
            }

            const payload = { username: username };
            stompClient.send('/app/room/' + roomId + '/join', {}, JSON.stringify(payload));
            log('📤 Sent JOIN message: ' + JSON.stringify(payload), 'info');
        }

        function testInvalidRoom() {
            if (!stompClient || !stompClient.connected) {
                log('❌ Not connected to WebSocket', 'error');
                return;
            }

            const username = document.getElementById('username').value;
            const payload = { username: username };
            stompClient.send('/app/room/INVALID-ROOM/join', {}, JSON.stringify(payload));
            log('📤 Testing invalid room...', 'info');
        }

        function testDuplicateUsername() {
            const roomId = document.getElementById('roomId').value || currentRoomId;
            if (!roomId) {
                log('❌ Please enter a Room ID first', 'error');
                return;
            }
            if (!stompClient || !stompClient.connected) {
                log('❌ Not connected to WebSocket', 'error');
                return;
            }

            // Use a username that's likely to exist (AdminUser from room creation)
            const payload = { username: 'AdminUser' };
            stompClient.send('/app/room/' + roomId + '/join', {}, JSON.stringify(payload));
            log('📤 Testing duplicate username...', 'info');
        }

        // Initialize on load
        window.onload = function() {
            log('WebSocket Tester initialized', 'info');
            log('Click "Connect" to start, then "Create Room via REST" to get a room ID', 'info');
        };
    </script>
</body>
</html>
```

**Usage:**
1. Save the HTML file
2. Open in a web browser
3. Click "Connect"
4. Click "Create Room via REST" to get a room ID
5. Click "Subscribe to Room" and "Subscribe to Private Queue"
6. Click "Send Join Message" to test player joining
7. Observe messages in the log

### Option 3: Node.js Script (For Automated Testing)

Create `websocket-test.js`:

```javascript
const SockJS = require('sockjs-client');
const Stomp = require('stompjs');
const fetch = require('node-fetch');

// Configuration
const WS_URL = 'http://localhost:8080/ws';
const REST_API = 'http://localhost:8080/api';

let stompClient = null;
let roomId = null;

// Create room via REST API
async function createRoom() {
    console.log('📝 Creating room...');
    const response = await fetch(`${REST_API}/rooms`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'AdminUser' })
    });
    const data = await response.json();
    roomId = data.roomId;
    console.log(`✅ Room created: ${roomId}`);
    return roomId;
}

// Connect to WebSocket
function connectWebSocket() {
    return new Promise((resolve, reject) => {
        const socket = new SockJS(WS_URL);
        stompClient = Stomp.over(socket);

        stompClient.connect({}, (frame) => {
            console.log('✅ Connected to WebSocket');
            resolve(stompClient);
        }, (error) => {
            console.error('❌ Connection failed:', error);
            reject(error);
        });
    });
}

// Test player joining
function testPlayerJoin(roomId, username) {
    return new Promise((resolve, reject) => {
        let receivedBroadcast = false;
        let receivedPrivate = false;

        // Subscribe to room topic (broadcast)
        stompClient.subscribe(`/topic/room/${roomId}`, (message) => {
            const data = JSON.parse(message.body);
            console.log('📨 BROADCAST:', data);
            if (data.type === 'PLAYER_JOINED') {
                receivedBroadcast = true;
                checkCompletion();
            }
        });

        // Subscribe to private queue
        stompClient.subscribe('/user/queue/private', (message) => {
            const data = JSON.parse(message.body);
            console.log('📬 PRIVATE:', data);
            if (data.type === 'ROOM_STATE') {
                receivedPrivate = true;
                checkCompletion();
            }
        });

        // Send join message after short delay
        setTimeout(() => {
            console.log(`📤 Sending join message: ${username}`);
            stompClient.send(`/app/room/${roomId}/join`, {},
                JSON.stringify({ username }));
        }, 500);

        // Check if both messages received
        function checkCompletion() {
            if (receivedBroadcast && receivedPrivate) {
                console.log('✅ Test passed: Received both broadcast and private messages');
                resolve();
            }
        }

        // Timeout after 5 seconds
        setTimeout(() => {
            if (!receivedBroadcast || !receivedPrivate) {
                reject(new Error('Timeout: Did not receive expected messages'));
            }
        }, 5000);
    });
}

// Test error scenarios
function testErrorScenarios(roomId) {
    return new Promise((resolve) => {
        let errorsReceived = 0;

        stompClient.subscribe('/user/queue/private', (message) => {
            const data = JSON.parse(message.body);
            if (data.type === 'ERROR') {
                console.log('📬 ERROR received:', data.message);
                errorsReceived++;
                if (errorsReceived === 2) {
                    console.log('✅ Both error tests passed');
                    resolve();
                }
            }
        });

        // Test 1: Invalid room
        setTimeout(() => {
            console.log('📤 Testing invalid room...');
            stompClient.send('/app/room/INVALID-ROOM/join', {},
                JSON.stringify({ username: 'TestUser' }));
        }, 500);

        // Test 2: Duplicate username
        setTimeout(() => {
            console.log('📤 Testing duplicate username...');
            stompClient.send(`/app/room/${roomId}/join`, {},
                JSON.stringify({ username: 'AdminUser' }));
        }, 1500);
    });
}

// Run all tests
async function runTests() {
    try {
        console.log('🚀 Starting WebSocket tests...\n');

        // Step 1: Create room
        roomId = await createRoom();

        // Step 2: Connect WebSocket
        await connectWebSocket();

        // Step 3: Test player join
        console.log('\n🧪 Test 1: Player Join');
        await testPlayerJoin(roomId, 'WebSocketPlayer');

        // Step 4: Test error scenarios
        console.log('\n🧪 Test 2: Error Scenarios');
        await testErrorScenarios(roomId);

        console.log('\n✅ All tests completed successfully!');
        process.exit(0);
    } catch (error) {
        console.error('\n❌ Test failed:', error);
        process.exit(1);
    }
}

// Run tests
runTests();
```

**Setup:**
```bash
npm install sockjs-client stompjs node-fetch
node websocket-test.js
```

---

## Test Scenarios

### Scenario 1: Happy Path - Player Join

**Expected Flow:**
1. Player connects to WebSocket: `ws://localhost:8080/ws`
2. Player subscribes to room topic: `/topic/room/{roomId}`
3. Player subscribes to private queue: `/user/queue/private`
4. Player sends join message to: `/app/room/{roomId}/join`
5. All subscribers receive `PLAYER_JOINED` broadcast
6. New player receives `ROOM_STATE` private message

**Success Criteria:**
- ✅ PLAYER_JOINED event received by all subscribers
- ✅ ROOM_STATE event received by joining player
- ✅ Player appears in room with correct data

### Scenario 2: Error - Invalid Room ID

**Test Steps:**
1. Connect and subscribe to private queue
2. Send join message to non-existent room: `/app/room/INVALID-ROOM/join`
3. Receive ERROR event

**Expected Response:**
```json
{
    "type": "ERROR",
    "message": "Room not found: INVALID-ROOM"
}
```

### Scenario 3: Error - Duplicate Username

**Test Steps:**
1. Create room with username "AdminUser"
2. Connect second WebSocket client
3. Try to join with same username "AdminUser"
4. Receive ERROR event

**Expected Response:**
```json
{
    "type": "ERROR",
    "message": "Username already taken: AdminUser"
}
```

### Scenario 4: Multiple Clients Broadcast

**Test Steps:**
1. Connect 3 WebSocket clients
2. All subscribe to same room topic
3. One client sends join message
4. All 3 clients receive PLAYER_JOINED broadcast

**Success Criteria:**
- ✅ All clients receive the same broadcast message
- ✅ Message delivery is near-instantaneous
- ✅ No message loss

---

## Message Formats

### Client → Server (Send)

**Join Room:**
```
Destination: /app/room/{roomId}/join
Content-Type: application/json
Body: {"username": "PlayerName"}
```

### Server → Client (Receive)

**PLAYER_JOINED (Broadcast):**
```json
{
    "type": "PLAYER_JOINED",
    "playerId": "uuid-here",
    "username": "PlayerName"
}
```

**ROOM_STATE (Private):**
```json
{
    "type": "ROOM_STATE",
    "players": [
        {
            "id": "player-id",
            "username": "PlayerName",
            "team": null,
            "role": "SPECTATOR",
            "connected": true,
            "admin": false
        }
    ],
    "settings": {
        "wordPack": "english",
        "timerSeconds": null
    },
    "canStart": false
}
```

**ERROR (Private):**
```json
{
    "type": "ERROR",
    "message": "Error description here"
}
```

---

## Troubleshooting

### Issue: Cannot Connect to WebSocket

**Solutions:**
1. Verify backend is running: `curl http://localhost:8080/actuator/health`
2. Check WebSocket endpoint: `curl http://localhost:8080/ws/info`
3. Ensure no firewall blocking port 8080
4. Check browser console for errors

### Issue: Messages Not Received

**Solutions:**
1. Verify subscription is active before sending messages
2. Add 500ms delay after subscribing
3. Check destination paths match exactly
4. Verify room ID is correct
5. Check backend logs for errors

### Issue: Authentication/CORS Errors

**Solutions:**
1. WebSocket config allows all origins in development
2. No authentication required for testing
3. If issues persist, check `WebSocketConfig.java`:
   ```java
   registry.addEndpoint("/ws")
           .setAllowedOriginPatterns("*")
           .withSockJS();
   ```

### Issue: STOMP Frame Errors

**Solutions:**
1. Ensure payload is valid JSON
2. Check Content-Type header
3. Verify STOMP protocol version compatibility
4. Use SockJS for browser compatibility

---

## Advanced Testing

### Performance Testing

Test with multiple concurrent connections:

```javascript
// Connect 10 clients simultaneously
const clients = [];
for (let i = 0; i < 10; i++) {
    const client = await connectWebSocket();
    clients.push(client);
}

// All clients join same room
const promises = clients.map((client, index) =>
    testPlayerJoin(roomId, `Player${index}`, client)
);
await Promise.all(promises);
```

### Load Testing

Use Artillery or similar tools:

```yaml
# artillery-test.yml
config:
  target: "ws://localhost:8080"
  phases:
    - duration: 60
      arrivalRate: 5
scenarios:
  - name: "WebSocket Connection Test"
    engine: "socketio"
    flow:
      - emit:
          channel: "/app/room/TEST-ROOM/join"
          data:
            username: "LoadTestUser"
```

Run: `artillery run artillery-test.yml`

---

## Next Steps

After verifying WebSocket functionality:

1. Test team assignment (Step-03 Commit 4)
2. Test disconnect handling
3. Test concurrent operations
4. Integrate with frontend
5. Perform E2E testing

---

## Support

For issues or questions:
- Check backend logs: `tail -f logs/application.log`
- Review test results in Postman
- Check browser DevTools Network tab for WebSocket frames
- Verify Spring Boot actuator endpoints: `/actuator/health`
