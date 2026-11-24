import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";
import BottomNav from "../components/BottomNav.jsx";
import Icon from "../components/Icon.jsx";
import { DEMO_MODE } from "../config.js";
import { RiskChip, RiskMeter } from "../components/Risk.jsx";
import { alertsApi } from "../lib/api.js";
import { truncate } from "../lib/format.js";
import { CHANNELS } from "../lib/risk.js";
import { sessionsStore } from "../lib/storage.js";

const GREETING = {
  id: "greeting",
  from: "bot",
  text: "Oi! Conte o que aconteceu: quem entrou em contato, por qual canal e o que pediram. Não precisa de dados pessoais.",
};
const SUGGESTIONS = [
  { text: "Ligaram dizendo que sou do banco e pediram para instalar um aplicativo", channel: "LIGACAO" },
  { text: "Recebi um link no WhatsApp sobre uma compra que não fiz", channel: "WHATSAPP" },
  { text: "Pediram o código que chegou por SMS", channel: "SMS" },
];
const SAFETY_TIP =
  "Enquanto isso: não instale aplicativos, não informe senhas, tokens ou códigos e confirme pelo número oficial do banco, que está no seu cartão.";

let sequence = 0;
const localId = () => `l${++sequence}`;

export default function Chat() {
  const { alertId: param } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [alertId, setAlertId] = useState(param ? Number(param) : null);
  const [messages, setMessages] = useState(param ? [] : [GREETING]);
  const [draft, setDraft] = useState("");
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(Boolean(param));
  const [error, setError] = useState("");
  const [pending, setPending] = useState(null); // { text, bubbleId } aguardando a escolha do canal
  const createdHere = useRef(null);
  const endRef = useRef(null);

  // Abre uma sessão existente (histórico) ou reinicia ao pedir uma nova análise.
  useEffect(() => {
    if (!param) {
      setAlertId(null);
      setMessages([GREETING]);
      setPending(null);
      setLoading(false);
      return undefined;
    }
    const id = Number(param);
    if (createdHere.current === id) {
      createdHere.current = null; // sessão criada nesta tela: o estado já está completo
      return undefined;
    }
    let cancelled = false;
    setAlertId(id);
    setLoading(true);
    setError("");
    alertsApi
      .messages(id)
      .then((list) => {
        if (cancelled) return;
        setMessages(list.map((m) => ({ id: `m${m.id}`, from: m.sender === "USUARIO" ? "user" : "contact", text: m.content })));
      })
      .catch((err) => !cancelled && setError(err.message))
      .finally(() => !cancelled && setLoading(false));
    return () => { cancelled = true; };
  }, [param]);

  useEffect(() => { endRef.current?.scrollIntoView({ block: "end" }); }, [messages, pending]);

  const push = (message) => {
    const id = localId();
    setMessages((list) => [...list, { id, ...message }]);
    return id;
  };
  const drop = (id) => setMessages((list) => list.filter((m) => m.id !== id));

  async function startSession(channel, content, existingBubble) {
    setBusy(true);
    setError("");
    const bubbleId = existingBubble ?? push({ from: "user", text: content });
    let created;
    try {
      created = await alertsApi.create(channel);
    } catch (err) {
      if (!existingBubble) { drop(bubbleId); setDraft(content); }
      setError(err.message);
      setBusy(false);
      return;
    }
    const score = created.overallRiskScore ?? null;
    createdHere.current = created.id;
    setAlertId(created.id);
    setPending(null);
    sessionsStore.upsert(user.email, {
      id: created.id, channel, createdAt: created.createdAt, preview: truncate(content, 80), score,
    });
    navigate(`/analisar/${created.id}`, { replace: true });
    try {
      await alertsApi.sendMessage(created.id, content);
      push({ from: "bot", kind: "registered", score });
    } catch (err) {
      drop(bubbleId);
      setDraft(content);
      setError(`A sessão foi criada, mas sua mensagem não foi enviada: ${err.message}`);
    } finally {
      setBusy(false);
    }
  }

  async function sendMessage(content) {
    setBusy(true);
    setError("");
    setDraft("");
    const bubbleId = push({ from: "user", text: content });
    try {
      await alertsApi.sendMessage(alertId, content);
    } catch (err) {
      drop(bubbleId);
      setDraft(content);
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  function submit(text, channel) {
    const content = text.trim();
    if (!content || busy || pending || loading) return;
    if (alertId) return sendMessage(content);
    if (channel) return startSession(channel, content);
    // Sem canal ainda: mostra o relato e pergunta por onde chegou.
    setError("");
    setDraft("");
    setPending({ text: content, bubbleId: push({ from: "user", text: content }) });
  }

  const chooseChannel = (channel) => startSession(channel, pending.text, pending.bubbleId);
  const showSuggestions = !alertId && !pending && !loading && messages.length <= 1;

  return (
    <div className="app">
      <a className="skip-link" href="#main-content">Pular para a conversa</a>
      <header className="chat-head">
        <Link to="/" className="back" aria-label="Voltar"><Icon name="back" /></Link>
        <div>
          <h1>Analisar contato</h1>
          <p>Descreva o alerta e eu avalio o risco</p>
        </div>
      </header>

      <div className="chat-body" id="main-content" tabIndex="-1" role="log" aria-live="polite" aria-label="Conversa">
        {DEMO_MODE && <p className="notice small" role="status">Demonstração local: a conversa e a análise são simuladas.</p>}
        {loading && <p className="muted" role="status">Carregando conversa…</p>}
        {messages.map((m) => (
          <div key={m.id} className={`bubble bubble--${m.from}`}>
            {m.from === "contact" && <span className="bubble__who">Contato</span>}
            {m.kind === "registered" ? (
              <>
                {m.score !== null && m.score !== undefined ? (
                  <>Avaliação de risco: <RiskChip score={m.score} /><RiskMeter score={m.score} /></>
                ) : (
                  <>Relato registrado na sessão #{alertId}. A avaliação de risco ainda não está disponível.{"\n\n"}</>
                )}
                {SAFETY_TIP}
              </>
            ) : m.text}
          </div>
        ))}
        {pending && (
          <div className="bubble bubble--bot">
            Por qual canal isso chegou?
            <div className="suggestions suggestions--inline">
              {CHANNELS.map((c) => (
                <button key={c.value} className="suggestion" disabled={busy} onClick={() => chooseChannel(c.value)}>{c.label}</button>
              ))}
            </div>
          </div>
        )}
        {showSuggestions && (
          <div className="suggestions">
            {SUGGESTIONS.map((s) => (
              <button key={s.text} className="suggestion" disabled={busy} onClick={() => submit(s.text, s.channel)}>{s.text}</button>
            ))}
          </div>
        )}
        <div ref={endRef} />
      </div>

      <div className="chat-foot">
        <p className="chat-note">Nunca informe senha, token ou código neste chat.</p>
        {error && <p className="error" role="alert">{error}</p>}
        <form className="composer" onSubmit={(e) => { e.preventDefault(); submit(draft); }}>
          <input value={draft} onChange={(e) => setDraft(e.target.value)} maxLength={10000}
            aria-label="Descreva o alerta" disabled={Boolean(pending) || loading}
            placeholder={pending ? "Escolha o canal acima" : "Descreva o alerta: quem ligou, o que pediu…"} />
          <button className="send" aria-label="Enviar" disabled={!draft.trim() || busy || Boolean(pending) || loading}>
            <Icon name="send" size={22} />
          </button>
        </form>
      </div>
      <BottomNav />
    </div>
  );
}
