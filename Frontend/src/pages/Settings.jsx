import { useAccessibility } from "../accessibility/AccessibilityContext.jsx";
import Icon from "../components/Icon.jsx";
import Screen from "../components/Screen.jsx";

function Switch({ label, checked, onChange, disabled }) {
  return (
    <button type="button" role="switch" aria-checked={checked} aria-label={label}
      className="switch" disabled={disabled} onClick={() => onChange(!checked)} />
  );
}

export default function Settings() {
  const { prefs, update } = useAccessibility();

  return (
    <Screen
      sub="Personalize sua experiência"
      title="Configurações"
      headerExtra={<span className="settings-hero-icon"><Icon name="gear" size={54} /></span>}
      heroClassName="hero--settings"
    >
      <section className="card settings-card">
        <div className="card-heading settings-card__heading">
          <span className="card-heading__icon"><Icon name="shield" size={21} /></span>
          <h2>Proteção e alertas</h2>
        </div>
        <ul className="list">
          <li>
            <div className="item">
              <span className="grow"><strong>Alertas de contato suspeito</strong><small>Aviso imediato por notificação</small></span>
              <Switch label="Alertas de contato suspeito" checked={prefs.alerts} onChange={update("alerts")} />
            </div>
          </li>
          <li>
            <div className="item">
              <span className="grow"><strong>Compartilhar denúncias anonimizadas</strong><small>Ajuda a mapear padrões de golpe</small></span>
              <Switch label="Compartilhar denúncias anonimizadas" checked={prefs.shareReports} onChange={update("shareReports")} />
            </div>
          </li>
        </ul>
        <p className="muted small" style={{ marginTop: 8 }}>Preferências salvas neste dispositivo.</p>
      </section>
      <section className="card settings-card">
        <div className="card-heading settings-card__heading">
          <span className="card-heading__icon"><Icon name="gear" size={21} /></span>
          <h2>Acessibilidade</h2>
        </div>
        <ul className="list">
          <li>
            <div className="item">
              <span className="grow"><strong>Alto contraste</strong><small>Aumenta o contraste de textos e controles</small></span>
              <Switch label="Alto contraste" checked={prefs.highContrast} onChange={update("highContrast")} />
            </div>
          </li>
          <li>
            <div className="item">
              <span className="grow"><strong>Reduzir animações</strong><small>Reduz transições e movimentos da interface</small></span>
              <Switch label="Reduzir animações" checked={prefs.reduceMotion} onChange={update("reduceMotion")} />
            </div>
          </li>
          <li>
            <div className="item">
              <span className="grow"><strong>Exibição para daltonismo</strong><small>Usa padrões e símbolos além das cores para indicar o risco</small></span>
              <Switch label="Exibição para daltonismo" checked={prefs.colorBlindMode} onChange={update("colorBlindMode")} />
            </div>
          </li>
        </ul>
        <p className="muted small" style={{ marginTop: 8 }}>Preferências de acessibilidade salvas neste dispositivo.</p>
      </section>
      <section className="card settings-card">
        <div className="card-heading settings-card__heading">
          <span className="card-heading__icon"><Icon name="user" size={21} /></span>
          <h2>Preferências gerais</h2>
        </div>
        <ul className="list" style={{ marginTop: 0 }}>
          <li><div className="item"><span className="grow"><strong>Idioma</strong><small>Português (Brasil)</small></span></div></li>
        </ul>
      </section>
    </Screen>
  );
}
