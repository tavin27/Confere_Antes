import { useAccessibility } from "../accessibility/AccessibilityContext.jsx";
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
    <Screen sub="Preferências" title="Configurações">
      <section className="card">
        <h2>Proteção e alertas</h2>
        <ul className="list">
          <li>
            <div className="item">
              <span className="grow"><strong>Alertas de contato suspeito</strong><small>Aviso imediato por notificação</small></span>
              <Switch label="Alertas de contato suspeito" checked={prefs.alerts} onChange={update("alerts")} />
            </div>
          </li>
          <li>
            <div className="item item--disabled">
              <span className="grow"><strong>Confirmação em duas etapas no login</strong><small>Código de verificação ao entrar · Em breve</small></span>
              <Switch label="Confirmação em duas etapas no login (em breve)" checked={false} onChange={() => {}} disabled />
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
      <section className="card">
        <h2>Acessibilidade</h2>
        <ul className="list">
          <li>
            <label className="accessibility-select" htmlFor="font-size">
              <span className="grow"><strong>Tamanho do texto</strong><small>Ajuste a leitura em todas as telas</small></span>
              <select id="font-size" value={prefs.fontSize} onChange={(event) => update("fontSize")(event.target.value)}>
                <option value="normal">Padrão</option>
                <option value="large">Grande</option>
                <option value="x-large">Muito grande</option>
              </select>
            </label>
          </li>
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
      <section className="card">
        <ul className="list" style={{ marginTop: 0 }}>
          <li><div className="item"><span className="grow"><strong>Idioma</strong><small>Português (Brasil)</small></span></div></li>
          <li><div className="item item--disabled" aria-disabled="true"><span className="grow"><strong>Privacidade e dados</strong></span><span className="badge">Em breve</span></div></li>
          <li><div className="item item--disabled" aria-disabled="true"><span className="grow"><strong>Ajuda e suporte</strong></span><span className="badge">Em breve</span></div></li>
        </ul>
      </section>
    </Screen>
  );
}
