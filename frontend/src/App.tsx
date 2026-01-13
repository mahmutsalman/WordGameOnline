import { BrowserRouter, Routes, Route } from 'react-router-dom';
import HomePage from './pages/HomePage';
import RoomPage from './pages/RoomPage';

/**
 * Main application component with routing.
 * Step 2: Room REST API - HomePage for creating/joining rooms.
 * RoomPage is a placeholder showing room was created successfully.
 * Step 3 will add: WebSocket, team selection, game start functionality.
 */
function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/room/:roomId" element={<RoomPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
