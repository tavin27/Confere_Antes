import { Navigate, Route, Routes } from "react-router-dom";
import { PublicOnly, RequireAuth } from "./auth/Guards.jsx";
import Chat from "./pages/Chat.jsx";
import History from "./pages/History.jsx";
import Home from "./pages/Home.jsx";
import Login from "./pages/Login.jsx";
import PersonalData from "./pages/PersonalData.jsx";
import Profile from "./pages/Profile.jsx";
import Register from "./pages/Register.jsx";
import Settings from "./pages/Settings.jsx";

export default function App() {
  return (
    <Routes>
      <Route element={<PublicOnly />}>
        <Route path="/login" element={<Login />} />
        <Route path="/cadastro" element={<Register />} />
      </Route>
      <Route element={<RequireAuth />}>
        <Route path="/" element={<Home />} />
        <Route path="/analisar/:alertId?" element={<Chat />} />
        <Route path="/historico" element={<History />} />
        <Route path="/perfil" element={<Profile />} />
        <Route path="/perfil/dados" element={<PersonalData />} />
        <Route path="/ajustes" element={<Settings />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
