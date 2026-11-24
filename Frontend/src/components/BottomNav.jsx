import { NavLink } from "react-router-dom";
import Icon from "./Icon.jsx";

const ITEMS = [
  { to: "/", icon: "home", label: "Início", end: true },
  { to: "/perfil", icon: "user", label: "Perfil" },
  { to: "/ajustes", icon: "gear", label: "Ajustes" },
];

export default function BottomNav() {
  return (
    <nav className="nav" aria-label="Principal">
      {ITEMS.map(({ to, icon, label, end }) => (
        <NavLink key={to} to={to} end={end} className={({ isActive }) => `nav__item${isActive ? " active" : ""}`}>
          <Icon name={icon} />
          {label}
        </NavLink>
      ))}
    </nav>
  );
}
