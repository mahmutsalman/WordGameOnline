import { BrowserRouter, Routes, Route } from 'react-router-dom';
import HomePage from './pages/HomePage';
import RoomPage from './pages/RoomPage';
import GamePage from './pages/GamePage';

/**
 * Main application component with routing.
 * - HomePage: Create/join rooms
 * - RoomPage: Lobby with team selection
 * - GamePage: Active game board with 5x5 card grid
 */
function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/room/:roomId" element={<RoomPage />} />
        <Route path="/room/:roomId/game" element={<GamePage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
