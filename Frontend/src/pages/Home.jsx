import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";
import { RiskChip, RiskMeter } from "../components/Risk.jsx";
import Screen from "../components/Screen.jsx";
import SessionRow from "../components/SessionRow.jsx";
import { channelInfo } from "../lib/risk.js";
import { sessionsStore } from "../lib/storage.js";

export default function Home() {
  const { user } = useAuth();
  const [sessions] = useState(() => sessionsStore.list(user.email));
  const lastScored = sessions.find((s) => s.score !== null && s.score !== undefined);

  return (
    <Screen sub={`Olá, ${user.username}`} title="Sua proteção">
      <section className="card stack">
        <h2>Recebeu um contato do “banco”?</h2>
        <p className="muted">Pare, respire e analise o contato antes de instalar, clicar ou autorizar qualquer coisa.</p>
        <Link to="/analisar" className="btn">Analisar contato</Link>
      </section>

      <section className="card stack">
        <div className="row">
          <h2>Score de risco</h2>
          {lastScored && <RiskChip score={lastScored.score} />}
        </div>
        {lastScored ? (
          <>
            <RiskMeter score={lastScored.score} />
            <p className="muted small">Última análise: {channelInfo(lastScored.channel).label} · {lastScored.preview}</p>
          </>
        ) : (
          <p className="muted">Nenhuma avaliação de risco ainda. Descreva um contato para começar.</p>
        )}
      </section>

      <section className="card">
        <div className="row">
          <h2>Histórico de padrões</h2>
          {sessions.length > 3 && <Link to="/historico" className="link-btn">Ver tudo</Link>}
        </div>
        {sessions.length ? (
          <ul className="list">{sessions.slice(0, 3).map((s) => <SessionRow key={s.id} session={s} />)}</ul>
        ) : (
          <p className="muted" style={{ marginTop: 8 }}>Seu histórico aparece aqui depois da primeira análise.</p>
        )}
      </section>
    </Screen>
  );
}
