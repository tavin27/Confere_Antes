import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";
import { RiskChip, RiskMeter } from "../components/Risk.jsx";
import Icon from "../components/Icon.jsx";
import Screen from "../components/Screen.jsx";
import { channelInfo } from "../lib/risk.js";
import { sessionsStore } from "../lib/storage.js";

export default function Home() {
  const { user } = useAuth();
  const [sessions] = useState(() => sessionsStore.list(user.email));
  const lastScored = sessions.find((s) => s.score !== null && s.score !== undefined);

  return (
    <Screen sub={`Olá, ${user.username}`} title="Sua proteção">
      <section className="card stack">
        <div className="card-heading">
          <span className="card-heading__icon"><Icon name="msg" size={21} /></span>
          <h2>Recebeu um contato do “banco”?</h2>
        </div>
        <p className="muted">Pare, respire e analise o contato antes de instalar, clicar ou autorizar qualquer coisa.</p>
        <Link to="/analisar" className="btn">Analisar contato</Link>
      </section>

      <section className="card stack">
        <div className="row">
          <div className="card-heading">
            <span className="card-heading__icon"><Icon name="shield" size={21} /></span>
            <h2>Score de risco</h2>
          </div>
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

      <section className="card stack">
        <div className="card-heading">
          <span className="card-heading__icon"><Icon name="phone" size={21} /></span>
          <h2>Precisa de ajuda?</h2>
        </div>
        <p className="muted">Em caso de emergência, entre em contato com a Polícia Militar pelo 190.</p>
        <a href="tel:190" className="btn btn--help" aria-label="Ligar para o 190">
          <Icon name="phone" size={20} />
          Ligar para o 190
        </a>
      </section>
    </Screen>
  );
}
