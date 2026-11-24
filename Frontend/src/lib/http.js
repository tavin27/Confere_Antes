import { API_BASE } from "../config.js";

export class ApiError extends Error {
  constructor(message, { status = 0, fields = null } = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fields = fields; // erros de validação por campo (HTTP 400)
  }
}

const TOKEN_KEY = "ca:token";
const hasStorage = typeof sessionStorage !== "undefined";
let memoryToken = null;

// O JWT fica em sessionStorage: some ao fechar a aba.
export const tokenStore = {
  get() {
    if (!memoryToken && hasStorage) memoryToken = sessionStorage.getItem(TOKEN_KEY);
    return memoryToken;
  },
  set(value) {
    memoryToken = value || null;
    if (!hasStorage) return;
    if (memoryToken) sessionStorage.setItem(TOKEN_KEY, memoryToken);
    else sessionStorage.removeItem(TOKEN_KEY);
  },
};

const listeners = new Set();
export function onUnauthorized(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

const FALLBACK = {
  401: "Sua sessão expirou. Entre novamente.",
  403: "Você não tem permissão para isso.",
  404: "Não encontramos o que você procurou.",
};

/** Converte as respostas de erro do Spring Boot (JSON, mapa de campos ou texto) em ApiError. */
export async function parseError(res) {
  let raw = "";
  try { raw = await res.text(); } catch { /* corpo ilegível */ }
  let data = null;
  try { data = raw ? JSON.parse(raw) : null; } catch { /* não é JSON */ }

  if (data && typeof data === "object") {
    if (typeof data.message === "string" && data.message) {
      return new ApiError(data.message, { status: res.status });
    }
    const entries = Object.entries(data).filter(([, v]) => typeof v === "string");
    if (entries.length) {
      return new ApiError(entries[0][1], { status: res.status, fields: Object.fromEntries(entries) });
    }
  }
  const text = raw.trim();
  if (text && !data && text.length < 300 && !text.startsWith("<")) {
    return new ApiError(text, { status: res.status });
  }
  const fallback = FALLBACK[res.status]
    ?? (res.status >= 500
      ? "O servidor está indisponível no momento. Tente novamente em instantes."
      : "Não foi possível concluir. Tente novamente.");
  return new ApiError(fallback, { status: res.status });
}

export async function request(path, { method = "GET", body, auth = true, signal } = {}) {
  const headers = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const token = auth ? tokenStore.get() : null;
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal,
    });
  } catch (err) {
    if (err?.name === "AbortError") throw err;
    throw new ApiError("Não foi possível conectar ao servidor. Verifique sua conexão e tente de novo.");
  }

  if (!res.ok) {
    if (res.status === 401 && token) {
      tokenStore.set(null);
      listeners.forEach((l) => l());
    }
    throw await parseError(res);
  }
  if (res.status === 204) return null;
  return (res.headers.get("content-type") || "").includes("json") ? res.json() : res.text();
}
