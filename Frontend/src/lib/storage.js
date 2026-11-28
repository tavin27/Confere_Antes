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
  highContrast: false,
  reduceMotion: false,
  colorBlindMode: false,
};
export const settingsStore = {
  get: (email) => ({ ...DEFAULT_SETTINGS, ...read(`ca:settings:${scope(email)}`, {}) }),
  set: (email, settings) => write(`ca:settings:${scope(email)}`, settings),
};

export const trustedContactsStore = {
  list(email) {
    const value = localStorage.getItem(`ca:trusted-contacts:${scope(email)}`);
    if (!value) return [];
    const contacts = JSON.parse(value);
    if (!Array.isArray(contacts)) throw new Error("Os contatos salvos estão em um formato inválido.");
    return contacts;
  },
  save(email, contacts) {
    localStorage.setItem(`ca:trusted-contacts:${scope(email)}`, JSON.stringify(contacts));
  },
};
