# Postman Testing Resources

Comprehensive testing resources for the Codenames Game Backend API and WebSocket functionality.

## 📁 Files in This Directory

### 1. `step-04-part-4.json` (NEW)
**Step-04 Part 4: Tests CardFactory and GameStateFactory** through the game start endpoint.

**Features:**
- ✅ Complete game setup flow (create room → join players → assign roles → start game)
- ✅ Validates CardFactory creates 25 cards with proper distribution
- ✅ Validates GameStateFactory creates game state with random starting team
- ✅ Tests board distribution: 9 starting team, 8 other team, 7 neutral, 1 assassin
- ✅ Tests words are uppercase, cards are unrevealed, no initial clue/winner

**Test Coverage:**
- Setup (8 requests): Create room, join 4 players, assign team/roles
- Start Game: Validates CardFactory and GameStateFactory output
- Verification: Game state persistence, error cases

### 2. `Codenames-WebSocket-Tests.postman_collection.json`
Complete Postman collection with automated tests for REST API and WebSocket endpoints.

**Features:**
- ✅ Automated test assertions
- ✅ Dynamic variables (room IDs stored automatically)
- ✅ Pre-configured base URL (localhost:8080)
- ✅ Integration test scenarios
- ✅ Error case validation

**Test Coverage:**
- REST API Tests (5 requests)
- Integration Scenarios (complete room flow)
- WebSocket connection info endpoint

### 2. `WebSocket-Testing-Guide.md`
Detailed guide for testing WebSocket functionality with multiple tools and approaches.

**Includes:**
- Browser-based HTML tester (copy-paste ready)
- Node.js automated test script
- Message format documentation
- Troubleshooting guide
- Advanced testing strategies

---

## 🚀 Quick Start

### Step 1: Start Backend
```bash
cd backend
mvn spring-boot:run
```

Verify backend is running:
```bash
curl http://localhost:8080/actuator/health
```

### Step 2: Import Postman Collection

**Option A: Via Postman Desktop/Web**
1. Open Postman
2. Click "Import" button
3. Select `Codenames-WebSocket-Tests.postman_collection.json`
4. Collection appears in left sidebar

**Option B: Via Command Line (if using Newman)**
```bash
npm install -g newman
newman run Codenames-WebSocket-Tests.postman_collection.json
```

### Step 3: Run Tests

**REST API Tests:**
1. Expand "REST API Tests" folder
2. Click "Run" on the folder
3. Watch all tests execute with assertions
4. All tests should pass ✅

**Integration Scenario:**
1. Expand "Integration Scenarios" > "Scenario 1: Complete Room Flow"
2. Run all requests sequentially
3. Final request shows 5 players in room

### Step 4: Test WebSocket (Optional)

Choose your preferred method from `WebSocket-Testing-Guide.md`:

**Easiest: Browser HTML Tester**
1. Copy HTML code from guide
2. Save as `websocket-test.html`
3. Open in browser
4. Click "Connect" then "Create Room via REST"
5. Test player joining and real-time updates

**Automated: Node.js Script**
1. Copy Node.js script from guide
2. Install dependencies: `npm install sockjs-client stompjs node-fetch`
3. Run: `node websocket-test.js`
4. View automated test results

---

## 📊 Test Structure

### Game Start API Tests (Factory Tests)

```
Game-Start-API/
├── 1. Setup - Create Room                    ✅ Creates room + stores IDs
├── 2-4. Setup - Join 3 Players               ✅ Adds players for all roles
├── 5-8. Setup - Assign Teams/Roles           ✅ Blue/Red Spymaster + Operative
├── 9. Start Game - Tests GameStateFactory    ✅ 25 cards, correct distribution
│   └── Tests: word uppercase, cards unrevealed, 9+8+7+1 distribution
│   └── Tests: phase=CLUE, no winner, no clue, empty history
├── 10. Get Game State - Verify Persistence   ✅ State stored correctly
├── 11. Start Game Again - Should Fail        ✅ 400 already started
├── 12. Start Game - Missing Roles            ✅ 400 cannot start
└── 13. Get Game State - No Game              ✅ 404 not found
```

### REST API Tests

```
REST API Tests/
├── 1. Create Room (Admin)          ✅ Creates room + stores roomId
├── 2. Get Room Details             ✅ Validates room structure
├── 3. Join Room (Player 2)         ✅ Adds player + stores player2Id
├── 4. Join Room (Duplicate - Fail) ✅ Tests 409 error
└── 5. Get Non-Existent Room        ✅ Tests 404 error
```

### Integration Scenarios

```
Integration Scenarios/
└── Scenario 1: Complete Room Flow/
    ├── 1.1 Create New Room
    ├── 1.2 Join Room - Player 1 (BlueSpymaster)
    ├── 1.3 Join Room - Player 2 (BlueOperative)
    ├── 1.4 Join Room - Player 3 (RedSpymaster)
    ├── 1.5 Join Room - Player 4 (RedOperative)
    └── 1.6 Verify Final Room State (5 players)
```

---

## ✅ Expected Results

### All Tests Passing

When you run the collection, you should see:

```
Codenames - WebSocket Tests
├─ REST API Tests
│  ├─ ✅ Create Room (Admin) - 6 assertions passed
│  ├─ ✅ Get Room Details - 2 assertions passed
│  ├─ ✅ Join Room (Player 2) - 3 assertions passed
│  ├─ ✅ Join Room (Duplicate - Fail) - 2 assertions passed
│  └─ ✅ Get Non-Existent Room - 2 assertions passed
└─ Integration Scenarios
   └─ ✅ Scenario 1: Complete Room Flow - 10 assertions passed

Total: 25 assertions passed
```

### Sample Room Response

After creating a room, you should see:

```json
{
    "roomId": "ABCDE-12345",
    "players": [
        {
            "id": "uuid-here",
            "username": "AdminUser",
            "team": null,
            "role": "SPECTATOR",
            "connected": true,
            "admin": true
        }
    ],
    "settings": {
        "wordPack": "english",
        "timerSeconds": null
    },
    "canStart": false,
    "adminId": "uuid-here"
}
```

---

## 🔧 Configuration

### Collection Variables

The collection uses these variables (automatically set):

| Variable | Description | Example |
|----------|-------------|---------|
| `baseUrl` | Backend API base URL | `http://localhost:8080` |
| `roomId` | Current test room ID | `ABCDE-12345` |
| `adminId` | Admin player ID | `uuid` |
| `player2Id` | Second player ID | `uuid` |
| `scenarioRoomId` | Integration scenario room | `FGHIJ-67890` |

**To change base URL:**
1. Click collection name
2. Go to "Variables" tab
3. Change `baseUrl` current value
4. Save

---

## 🧪 Test Assertions

Each request includes automated assertions:

### Example: Create Room Assertions

```javascript
✅ Status code is 200
✅ Response has required fields (roomId, players, settings, canStart, adminId)
✅ Admin player created correctly (username, admin flag, role)
✅ Room ID format is correct (XXXXX-XXXXX pattern)
```

### Example: Error Handling Assertions

```javascript
✅ Status code is 409 Conflict (for duplicate username)
✅ Error message contains 'already taken'
✅ Status code is 404 Not Found (for non-existent room)
✅ Error message contains 'not found'
```

---

## 🐛 Troubleshooting

### Issue: "Could not get any response"

**Solution:**
- Verify backend is running: `curl http://localhost:8080/actuator/health`
- Check `baseUrl` variable is set to `http://localhost:8080`
- Ensure no other service is using port 8080

### Issue: Tests fail with "roomId is undefined"

**Solution:**
- Run "Create Room" request first
- This sets the `roomId` variable for subsequent requests
- Or run the entire "REST API Tests" folder in sequence

### Issue: WebSocket tests don't work

**Solution:**
- Postman has limited WebSocket support
- Use the HTML browser tester from `WebSocket-Testing-Guide.md`
- Or use the Node.js automated script
- Real WebSocket testing requires STOMP protocol support

### Issue: 404 errors on all requests

**Solution:**
- Verify backend is running
- Check backend logs for startup errors
- Ensure database is accessible
- Verify port 8080 is not blocked

---

## 📈 Advanced Usage

### Running with Newman (CLI)

```bash
# Install Newman
npm install -g newman

# Run entire collection
newman run Codenames-WebSocket-Tests.postman_collection.json

# Run with specific environment
newman run Codenames-WebSocket-Tests.postman_collection.json \
    --env-var "baseUrl=http://staging-server:8080"

# Generate HTML report
newman run Codenames-WebSocket-Tests.postman_collection.json \
    --reporters cli,html \
    --reporter-html-export report.html
```

### CI/CD Integration

Add to your GitHub Actions workflow:

```yaml
- name: Run API Tests
  run: |
    npm install -g newman
    newman run backend/postman/Codenames-WebSocket-Tests.postman_collection.json \
        --reporters cli,json \
        --reporter-json-export test-results.json
```

---

## 📝 Next Steps

After running these tests successfully:

1. ✅ Verify all REST API endpoints work
2. ✅ Test WebSocket real-time functionality (see guide)
3. ⏳ Test team assignment (upcoming in Step-03 Commit 4)
4. ⏳ Test disconnect handling
5. ⏳ Integrate frontend WebSocket client
6. ⏳ Run E2E tests with frontend + backend

---

## 📚 Additional Resources

- **Backend Documentation**: `../README.md`
- **WebSocket Testing Guide**: `WebSocket-Testing-Guide.md` (this directory)
- **API Endpoints**: See backend controller classes
- **WebSocket Config**: `src/main/java/com/codenames/config/WebSocketConfig.java`

---

## 🤝 Contributing

To add new tests:

1. Open collection in Postman
2. Add new request
3. Add test assertions in "Tests" tab
4. Export updated collection
5. Update this README

Test assertion template:

```javascript
pm.test("Description of what to verify", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.field).to.eql(expectedValue);
});
```

---

## ✨ Summary

You now have:
- ✅ Complete REST API test suite with assertions
- ✅ Integration test scenarios
- ✅ WebSocket testing guide with 3 different approaches
- ✅ Troubleshooting documentation
- ✅ CI/CD integration examples

**Ready to test!** Start with importing the Postman collection and running the REST API tests. Then move on to WebSocket testing using the guide.
