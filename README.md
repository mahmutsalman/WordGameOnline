# Codenames Online - Multiplayer Word Game

A real-time multiplayer implementation of the Codenames board game using Spring Boot, React, TypeScript, and WebSocket.

## Project Overview

Codenames is a team-based word association game where two teams (Blue and Red) compete to identify their secret agents (words) on a 5×5 grid based on one-word clues given by their spymasters.

## Tech Stack

### Backend
- **Java 17** - Programming language
- **Spring Boot 3.2.1** - Application framework
- **Spring WebSocket** - Real-time communication via STOMP over SockJS
- **Maven** - Build tool and dependency management
- **JUnit 5** - Testing framework
- **JaCoCo** - Code coverage
- **Checkstyle** - Code quality analysis

### Frontend
- **React 19** - UI framework
- **TypeScript** - Type-safe JavaScript
- **Vite** - Build tool and dev server
- **Tailwind CSS 4** - Utility-first CSS framework
- **Zustand** - State management
- **@stomp/stompjs** - WebSocket client
- **Vitest** - Testing framework
- **ESLint** - Code linting
- **Prettier** - Code formatting

## Project Structure

```
WordGameOnline/
├── backend/              # Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/codenames/
│   │   │   │   ├── CodenamesApplication.java
│   │   │   │   ├── config/
│   │   │   │   ├── repository/
│   │   │   │   └── ...
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── wordpacks/
│   │   │           └── english.txt
│   │   └── test/
│   ├── pom.xml
│   └── checkstyle.xml
│
└── frontend/             # React TypeScript application
    ├── src/
    │   ├── App.tsx
    │   ├── main.tsx
    │   ├── index.css
    │   └── types/
    ├── tests/
    ├── package.json
    ├── vite.config.ts
    ├── tailwind.config.js
    └── tsconfig.json
```

## Prerequisites

Make sure you have the following installed:

- **Java 17+** - [Download](https://adoptium.net/)
- **Maven 3.8+** - [Download](https://maven.apache.org/download.cgi)
- **Node.js 18+** - [Download](https://nodejs.org/)
- **npm 9+** (comes with Node.js)

Verify installations:
```bash
java -version        # Should show 17 or higher
mvn -version         # Should show 3.8 or higher
node -version        # Should show 18 or higher
npm -version         # Should show 9 or higher
```

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd WordGameOnline
```

### 2. Backend Setup

Navigate to the backend directory:
```bash
cd backend
```

Install dependencies and build:
```bash
mvn clean install
```

Run tests:
```bash
mvn test
```

Run Checkstyle:
```bash
mvn checkstyle:check
```

Start the backend server:
```bash
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

**WebSocket endpoint:** `ws://localhost:8080/ws`

### 3. Frontend Setup

Navigate to the frontend directory:
```bash
cd frontend
```

Install dependencies:
```bash
npm install
```

Run tests:
```bash
npm test
```

Run linting:
```bash
npm run lint
```

Format code:
```bash
npm run format
```

Build for production:
```bash
npm run build
```

Start the development server:
```bash
npm run dev
```

The frontend will start on `http://localhost:5173`

## Development Workflow

### Running Both Services

**Terminal 1 - Backend:**
```bash
cd backend
mvn spring-boot:run
```

**Terminal 2 - Frontend:**
```bash
cd frontend
npm run dev
```

Access the application at `http://localhost:5173`

### Code Quality

**Backend:**
```bash
# Run tests with coverage
mvn test

# View coverage report
open backend/target/site/jacoco/index.html

# Run Checkstyle
mvn checkstyle:check
```

**Frontend:**
```bash
# Run linting
npm run lint

# Auto-fix linting issues
npm run lint -- --fix

# Check code formatting
npm run format:check

# Format code
npm run format

# Run tests
npm test

# Run tests with coverage
npm run test:coverage
```

## Design Patterns & Principles

This project follows SOLID principles and implements several design patterns:

### Design Patterns
- **Singleton Pattern**: `WordRepository` (loaded once at startup)
- **Factory Pattern**: (to be implemented in later steps)
- **Observer Pattern**: WebSocket subscriptions for real-time updates
- **Strategy Pattern**: (to be implemented in later steps)

### SOLID Principles
- **Single Responsibility**: Separate classes for config, repository, service, controller
- **Open/Closed**: Extensible through interfaces
- **Liskov Substitution**: Proper interface implementations
- **Interface Segregation**: Separate interfaces for different roles
- **Dependency Inversion**: Dependency injection via Spring

See `localResources/steps/step-00-design-patterns-solid.md` for detailed explanations.

## Architecture

### Backend Architecture
```
┌─────────────────────────────────────┐
│         Spring Boot App             │
│  ┌───────────────────────────────┐  │
│  │    REST Controllers           │  │
│  │    (/api/rooms/*)            │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │    WebSocket Controllers      │  │
│  │    (STOMP over SockJS)       │  │
│  │    /ws endpoint              │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │    Services                   │  │
│  │    (Game Logic)              │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │    Repositories               │  │
│  │    (WordRepository)          │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

### Frontend Architecture
```
┌─────────────────────────────────────┐
│         React App (Vite)            │
│  ┌───────────────────────────────┐  │
│  │    Pages/Screens              │  │
│  │    (React Router)             │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │    Components                 │  │
│  │    (UI + Game Components)     │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │    State Management           │  │
│  │    (Zustand)                  │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │    Services                   │  │
│  │    (API + WebSocket)          │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

### Communication Flow
```
Frontend (React)  ←─HTTP/REST─→  Backend (Spring Boot)
       ↓                                  ↓
   WebSocket Client ←──STOMP/SockJS──→ WebSocket Server
       ↓                                  ↓
   State Updates                    Broadcast to Clients
```

## Step-by-Step Implementation

This project is being built following a structured step-by-step approach:

- **Step 0**: Design Patterns & SOLID Principles ✅
- **Step 1**: Project Setup & Infrastructure ✅ (Current)
- **Step 2**: Room Creation & Joining (REST API)
- **Step 3**: WebSocket & Player Management
- **Step 4**: Lobby UI
- **Step 5**: Game Board Generation
- **Step 6**: Spymaster Clue System
- **Step 7**: Operative Guessing
- **Step 8**: Turn Logic & Win Conditions
- **Step 9**: Game Over & Restart
- **Step 10**: Polish & Deployment

See `localResources/steps/` for detailed step documentation.

## Step 1 Completion Checklist

- [x] Backend starts successfully on port 8080
- [x] Frontend starts successfully on port 5173
- [x] WebSocket endpoint `/ws` is accessible
- [x] WordRepository loads 400+ words from english.txt
- [x] All backend tests pass (6 tests)
- [x] All frontend tests pass
- [x] Checkstyle passes with no violations
- [x] ESLint passes with no errors
- [x] Vite proxy correctly forwards `/api` and `/ws` to backend
- [x] No compilation errors in TypeScript
- [x] Tailwind CSS loads and custom colors work

## Features (Planned)

### Phase 1: MVP
- Room creation and joining
- Real-time player management
- 5×5 game board with 25 words
- Spymaster and Operative roles
- Turn-based gameplay
- Win/loss detection
- Basic UI

### Phase 2: Enhancements
- Turn timer
- Game history/log
- Sound effects
- Animations
- Mobile responsive design
- Copy room link

### Phase 3: Advanced
- In-game chat
- Custom word packs
- User accounts
- Statistics tracking
- Color-blind mode
- Multiple game modes

## Testing

### Backend Testing
```bash
cd backend
mvn test                    # Run all tests
mvn test -Dtest=WordRepositoryTest  # Run specific test
```

**Test Coverage:**
- Context loading tests
- WordRepository tests (5 test cases)
- Code coverage reports via JaCoCo

### Frontend Testing
```bash
cd frontend
npm test                    # Run tests in watch mode
npm run test:coverage       # Run tests with coverage
```

**Test Setup:**
- Vitest for unit testing
- React Testing Library for component testing
- jsdom for DOM simulation

## Troubleshooting

### Backend Issues

**Port 8080 already in use:**
```bash
# Find and kill the process using port 8080
lsof -i :8080
kill -9 <PID>
```

**Maven build fails:**
```bash
# Clean and rebuild
mvn clean install -U
```

### Frontend Issues

**Port 5173 already in use:**
```bash
# Find and kill the process
lsof -i :5173
kill -9 <PID>
```

**npm install fails:**
```bash
# Clear npm cache and reinstall
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

**Tailwind not working:**
- Verify `@tailwindcss/postcss` is installed
- Check `postcss.config.js` uses `@tailwindcss/postcss`
- Ensure `index.css` has Tailwind directives

## Contributing

1. Follow the design patterns and SOLID principles outlined in Step 0
2. Write tests for new features
3. Run code quality checks before committing
4. Follow the existing code style
5. Update documentation as needed

## License

This project is for educational purposes.

## Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev/)
- [Vite Documentation](https://vitejs.dev/)
- [Tailwind CSS Documentation](https://tailwindcss.com/)
- [Original Codenames Game](https://czechgames.com/en/codenames/)

## Acknowledgments

- Based on the Codenames board game by Vlaada Chvátil (Czech Games Edition)
- Inspired by online implementations like [codenames.game](https://codenames.game)
