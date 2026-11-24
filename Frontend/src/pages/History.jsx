import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";
import Screen from "../components/Screen.jsx";
import SessionRow from "../components/SessionRow.jsx";
import { sessionsStore } from "../lib/storage.js";

export default function History() {
  const { user } = useAuth();
  const [sessions] = useState(() => sessionsStore.list(user.email));
  return (
    <Screen sub="Denúncias registradas" title="Histórico" backTo="/perfil">
      <section className="card">
        {sessions.length ? (
          <ul className="list">{sessions.map((s) => <SessionRow key={s.id} session={s} />)}</ul>
        ) : (
          <div className="stack">
            <p className="muted">Você ainda não registrou nenhuma análise neste dispositivo.</p>
            <Link to="/analisar" className="btn">Analisar contato</Link>
          </div>
        )}
      </section>
    </Screen>
  );
}
