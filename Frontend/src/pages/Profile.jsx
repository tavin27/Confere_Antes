import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";
import Icon from "../components/Icon.jsx";
import Screen from "../components/Screen.jsx";

const ITEMS = [
  { title: "Dados pessoais", sub: "Nome, e-mail e celular", to: "/perfil/dados" },
  { title: "Contatos de confiança", sub: "Pessoas avisadas em caso de alerta" },
  { title: "Denúncias registradas", sub: "Canal, discurso e resultado", to: "/historico" },
  { title: "Segurança da conta", sub: "Senha e verificação" },
];

export default function Profile() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  return (
    <Screen sub="Seu perfil" title={user.username}
      headerExtra={<div className="avatar avatar--big" aria-hidden="true">{user.username.charAt(0).toUpperCase()}</div>}>
      <section className="card">
        <ul className="list" style={{ marginTop: 0 }}>
          {ITEMS.map(({ title, sub, to }) => (
            <li key={title}>
              {to ? (
                <Link to={to} className="item">
                  <span className="grow"><strong>{title}</strong><small>{sub}</small></span>
                  <Icon name="chev" size={20} />
                </Link>
              ) : (
                <div className="item item--disabled" aria-disabled="true">
                  <span className="grow"><strong>{title}</strong><small>{sub}</small></span>
                  <span className="badge">Em breve</span>
                </div>
              )}
            </li>
          ))}
        </ul>
      </section>
      <button className="btn btn--outline" onClick={() => { logout(); navigate("/login", { replace: true }); }}>
        Sair da conta
      </button>
    </Screen>
  );
}
