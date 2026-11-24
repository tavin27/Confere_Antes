// O back-end ainda não lista alertas nem guarda preferências; por isso o
// histórico e as preferências ficam neste dispositivo, separados por e-mail.
const read = (key, fallback) => {
  try {
    const value = localStorage.getItem(key);
    return value ? JSON.parse(value) : fallback;
  } catch {
    return fallback;
  }
};
const write = (key, value) => {
  try { localStorage.setItem(key, JSON.stringify(value)); } catch { /* armazenamento cheio ou bloqueado */ }
};
const scope = (email) => String(email).toLowerCase();

export const sessionsStore = {
  list: (email) => read(`ca:sessions:${scope(email)}`, []),
  upsert(email, session) {
    const all = this.list(email);
    const previous = all.find((s) => s.id === session.id);
    const rest = all.filter((s) => s.id !== session.id);
    write(`ca:sessions:${scope(email)}`, [{ ...previous, ...session }, ...rest].slice(0, 50));
  },
};

const DEFAULT_SETTINGS = {
  alerts: true,
  shareReports: false,
  fontSize: "normal",
  highContrast: false,
  reduceMotion: false,
  colorBlindMode: false,
};
export const settingsStore = {
  get: (email) => ({ ...DEFAULT_SETTINGS, ...read(`ca:settings:${scope(email)}`, {}) }),
  set: (email, settings) => write(`ca:settings:${scope(email)}`, settings),
};
