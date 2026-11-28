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

function mapMessages(list) {
  return list.map((message) => ({
    id: `m${message.id}`,
    from: message.source === "AUTOMATICO" ? "bot" : message.sender === "USUARIO" ? "user" : "contact",
    text: message.content,
  }));
}

function needsAiReply(list) {
  let lastUserMessage = -1;
  let lastAiReply = -1;
  list.forEach((message, index) => {
    if (message.sender === "USUARIO") lastUserMessage = index;
    if (message.source === "AUTOMATICO") lastAiReply = index;
  });
  return lastUserMessage > lastAiReply;
}

export default function Chat() {
  const { alertId: param } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [alertId, setAlertId] = useState(param ? Number(param) : null);
  const [messages, setMessages] = useState(param ? [] : [GREETING]);
  const [draft, setDraft] = useState("");
  const [busy, setBusy] = useState(false);
  const [awaitingAi, setAwaitingAi] = useState(false);
  const [loading, setLoading] = useState(Boolean(param));
  const [error, setError] = useState("");
  const [pending, setPending] = useState(null); // { text, bubbleId } aguardando a escolha do canal
  const [listening, setListening] = useState(false);
  const [voiceSupported, setVoiceSupported] = useState(true);
  const [interimText, setInterimText] = useState("");
  const [callMode, setCallMode] = useState(false);
  const recognitionRef = useRef(null);
  const createdHere = useRef(null);
  const endRef = useRef(null);

  // Mantém referência ao draft atual para uso dentro de callbacks (evita closure stale)
  const draftRef = useRef("");
  useEffect(() => { draftRef.current = draft; }, [draft]);
  const listeningRef = useRef(false);
  const callModeRef = useRef(false);
  const alertIdRef = useRef(null);
  const busyRef = useRef(false);
  const pendingRef = useRef(null);
  const awaitingAiRef = useRef(false);

  // Mantém refs sincronizados com os estados para uso em callbacks assíncronos
  useEffect(() => { alertIdRef.current = alertId; }, [alertId]);
  useEffect(() => { busyRef.current = busy; }, [busy]);
  useEffect(() => { pendingRef.current = pending; }, [pending]);
  useEffect(() => { awaitingAiRef.current = awaitingAi; }, [awaitingAi]);

  // Verifica suporte na montagem
  useEffect(() => {
    if (!window.SpeechRecognition && !window.webkitSpeechRecognition) {
      setVoiceSupported(false);
    }
  }, []);

  function createAndStartRecognition(isCallMode = false) {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) return;

    // Sempre criar nova instância: SpeechRecognition não pode ser .start()ada após onend disparar
    const recognition = new SpeechRecognition();
    recognition.lang = "pt-BR";
    recognition.continuous = isCallMode; // contínuo no modo ligação
    recognition.interimResults = true; // Habilita feedback em tempo real
    recognition.maxAlternatives = 1;

    let silenceTimeout;
    const resetSilenceTimeout = () => {
      if (isCallMode) return; // modo ligação não tem timeout de silêncio
      clearTimeout(silenceTimeout);
      silenceTimeout = setTimeout(() => {
        if (listeningRef.current) recognition.stop();
      }, 3000);
    };

    recognition.onstart = () => {
      listeningRef.current = true;
      setListening(true);
      if (!isCallMode) resetSilenceTimeout();
    };

    recognition.onresult = (event) => {
      if (!isCallMode) resetSilenceTimeout();
      let finalText = "";
      let interim = "";
      for (let i = event.resultIndex; i < event.results.length; i++) {
        if (event.results[i].isFinal) {
          finalText += event.results[i][0].transcript;
        } else {
          interim += event.results[i][0].transcript;
        }
      }
      if (finalText) {
        setInterimText("");
        if (isCallMode) {
          // Modo ligação: envia cada frase final imediatamente
          sendCallSentence(finalText.trim());
        } else {
          setDraft((prev) => prev.trim() ? `${prev.trim()} ${finalText}` : finalText);
        }
      } else if (interim) {
        setInterimText(interim);
      }
    };

    recognition.onerror = (event) => {
      clearTimeout(silenceTimeout);
      // aborted: stop() chamado enquanto a instância inicializava — onend dispara logo após e reinicia
      if (event.error === "aborted") return;
      // no-speech em modo ligação é normal (silêncio entre frases), não encerra
      if (isCallMode && event.error === "no-speech") return;
      listeningRef.current = false;
      setListening(false);
      setInterimText("");
      if (event.error !== "no-speech" && event.error !== "network") {
        setError(`Erro na captura de voz: ${event.error}`);
      }
    };

    recognition.onend = () => {
      clearTimeout(silenceTimeout);
      setInterimText("");
      if (isCallMode && callModeRef.current) {
        // Modo ligação: browsers mobile encerram mesmo com continuous=true;
        // reinicia automaticamente enquanto o modo estiver ativo
        setTimeout(() => {
          if (callModeRef.current) createAndStartRecognition(true);
        }, 300);
        return;
      }
      listeningRef.current = false;
      setListening(false);
      // Auto-submit no modo normal
      const captured = draftRef.current.trim();
      if (captured) {
        submitRef.current(captured);
      }
    };

    recognitionRef.current = recognition;
    recognition.start();
  }

  function toggleVoice() {
    if (listening) {
      recognitionRef.current?.stop();
    } else {
      setError("");
      createAndStartRecognition(false);
    }
  }

  function toggleCallMode() {
    if (callMode) {
      // Encerrar modo ligação
      callModeRef.current = false;
      setCallMode(false);
      recognitionRef.current?.stop();
      listeningRef.current = false;
      setListening(false);
      setInterimText("");
    } else {
      setError("");
      callModeRef.current = true;
      setCallMode(true);
      // Se ainda não tem sessão, abre automaticamente com canal LIGACAO
      if (!alertIdRef.current) {
        startCallSession();
      } else {
        createAndStartRecognition(true);
      }
    }
  }

  async function startCallSession() {
    setBusy(true);
    busyRef.current = true;
    setError("");
    let created;
    try {
      created = await alertsApi.create("LIGACAO");
    } catch (err) {
      setError(err.message);
      callModeRef.current = false;
      setCallMode(false);
      setBusy(false);
      busyRef.current = false;
      return;
    }
    const score = created.overallRiskScore ?? null;
    createdHere.current = created.id;
    setAlertId(created.id);
    alertIdRef.current = created.id;
    sessionsStore.upsert(user.email, {
      id: created.id, channel: "LIGACAO", createdAt: created.createdAt, preview: "Monitoramento de ligação", score,
    });
    navigate(`/analisar/${created.id}`, { replace: true });
    setBusy(false);
    busyRef.current = false;
    createAndStartRecognition(true);
  }

  async function sendCallSentence(text) {
    if (!text || busyRef.current || awaitingAiRef.current || pendingRef.current) return;
    const id = alertIdRef.current;
    if (!id) return;
    busyRef.current = true;
    setBusy(true);
    setError("");
    const bubbleId = localId();
    setMessages((list) => [...list, { id: bubbleId, from: "user", text }]);
    try {
      await alertsApi.sendMessage(id, text);
      awaitingAiRef.current = true;
      setAwaitingAi(true);
    } catch (err) {
      setMessages((list) => list.filter((m) => m.id !== bubbleId));
      setError(err.message);
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  }

  // Abre uma sessão existente (histórico) ou reinicia ao pedir uma nova análise.
  useEffect(() => {
    if (!param) {
      setAlertId(null);
      setMessages([GREETING]);
      setPending(null);
      setAwaitingAi(false);
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
      .then(async (list) => {
        if (cancelled) return;
        setMessages(mapMessages(list));
        setAwaitingAi(needsAiReply(list));
        try {
          const alert = await alertsApi.get(id);
          if (cancelled) return;
          sessionsStore.upsert(user.email, { id, score: alert.overallRiskScore ?? null });
        } catch {
          // Falha ao atualizar o score não deve impedir a abertura da conversa.
        }
      })
      .catch((err) => !cancelled && setError(err.message))
      .finally(() => !cancelled && setLoading(false));
    return () => { cancelled = true; };
  }, [param, user.email]);

  // Polling: busca novas mensagens (respostas da IA) a cada 3 segundos quando há uma sessão ativa
  useEffect(() => {
    if (!alertId || !awaitingAi || busy || loading) return undefined;
    let cancelled = false;
    let attempts = 0;
    const maxAttempts = 24;
    const poll = async () => {
      attempts += 1;
      try {
        const list = await alertsApi.messages(alertId);
        if (cancelled) return;
        setMessages(mapMessages(list));
        if (!needsAiReply(list)) {
          try {
            const alert = await alertsApi.get(alertId);
            if (cancelled) return;
            const score = alert.overallRiskScore ?? null;
            sessionsStore.upsert(user.email, { id: alertId, score });
            setMessages((current) => {
              // Atualiza bolhas "registered" existentes e também anexa o score
              // à última mensagem da IA (bot) para exibição imediata.
              let lastBotIndex = -1;
              current.forEach((m, i) => { if (m.from === "bot") lastBotIndex = i; });
              return current.map((message, index) => {
                if (message.kind === "registered") return { ...message, score };
                if (score !== null && index === lastBotIndex && message.from === "bot") {
                  return { ...message, score };
                }
                return message;
              });
            });
          } catch {
            // A conversa continua disponível mesmo que a atualização do score falhe.
          }
          if (!cancelled) setAwaitingAi(false);
        } else if (attempts >= maxAttempts) {
          setAwaitingAi(false);
          setError("A IA ainda nÃ£o respondeu. Tente novamente mais tarde ou confira os logs do backend.");
        }
      } catch {
        if (!cancelled && attempts >= maxAttempts) {
          setAwaitingAi(false);
          setError("NÃ£o foi possÃ­vel obter a resposta da IA. Confira a conexÃ£o e os logs do backend.");
        }
      }
    };
    const interval = setInterval(poll, 2500);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [alertId, awaitingAi, busy, loading, user.email]);

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
    alertIdRef.current = created.id;
    setPending(null);
    sessionsStore.upsert(user.email, {
      id: created.id, channel, createdAt: created.createdAt, preview: truncate(content, 80), score,
    });
    navigate(`/analisar/${created.id}`, { replace: true });
    try {
      await alertsApi.sendMessage(created.id, content);
      setAwaitingAi(true);
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
      setAwaitingAi(true);
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
    if (!content || busy || awaitingAi || pending || loading) return;
    if (alertId) return sendMessage(content);
    if (channel) return startSession(channel, content);
    // Sem canal ainda: mostra o relato e pergunta por onde chegou.
    setError("");
    setDraft("");
    setPending({ text: content, bubbleId: push({ from: "user", text: content }) });
  }

  const chooseChannel = (channel) => startSession(channel, pending.text, pending.bubbleId);

  // Ref para submit: evita closure stale no onend da SpeechRecognition
  const submitRef = useRef(null);
  submitRef.current = submit;
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
            ) : (
              <>
                {m.from === "bot" && m.score !== null && m.score !== undefined && (
                  <div className="bubble__risk">
                    Avaliação de risco: <RiskChip score={m.score} />
                    <RiskMeter score={m.score} />
                  </div>
                )}
                {m.text}
              </>
            )}
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
        {awaitingAi && <p className="muted" role="status">A IA está analisando sua mensagem...</p>}
        {error && <p className="error" role="alert">{error}</p>}
        {voiceSupported && (
          <button
            type="button"
            className={`call-monitor${callMode ? " call-monitor--active" : ""}`}
            aria-label={callMode ? "Parar monitoramento da ligação" : "Monitorar ligação ao vivo"}
            aria-pressed={callMode}
            disabled={loading || Boolean(pending)}
            onClick={toggleCallMode}
          >
            <Icon name="mic" size={18} />
            {callMode ? "Parar monitoramento" : "📞 Monitorar ligação ao vivo"}
            {callMode && <span className="mic__pulse" aria-hidden="true" />}
          </button>
        )}
        {callMode && (
          <p className="call-monitor__status" role="status">
            🔴 Monitorando ao vivo — coloque no viva-voz e fale
            {interimText && <span className="call-monitor__interim"> "{interimText}"</span>}
          </p>
        )}
        <form className="composer" onSubmit={(e) => { e.preventDefault(); submit(draft); }}>
          {voiceSupported && !callMode && (
            <button
              type="button"
              className={`mic${listening ? " mic--active" : ""}`}
              aria-label={listening ? "Parar de ouvir" : "Falar em vez de digitar"}
              aria-pressed={listening}
              disabled={busy || awaitingAi || Boolean(pending) || loading}
              onClick={toggleVoice}
            >
              <Icon name="mic" size={22} />
              {listening && <span className="mic__pulse" aria-hidden="true" />}
            </button>
          )}
          <input value={interimText || draft} onChange={(e) => { setDraft(e.target.value); setInterimText(""); }} maxLength={10000}
            aria-label="Descreva o alerta" disabled={Boolean(pending) || loading || awaitingAi || callMode}
            className={interimText ? "input--interim" : undefined}
            placeholder={callMode ? "Transcrevendo ao vivo…" : listening ? "Ouvindo… fale agora" : pending ? "Escolha o canal acima" : "Descreva o alerta: quem ligou, o que pediu…"} />
          <button className="send" aria-label="Enviar" disabled={!draft.trim() || busy || awaitingAi || Boolean(pending) || loading || callMode}>
            <Icon name="send" size={22} />
          </button>
        </form>
        {listening && !callMode && <p className="muted" role="status">🎙️ Ouvindo… fale claramente. Toque no microfone para parar.</p>}
      </div>
      <BottomNav />
    </div>
  );
}
