import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { DEMO_ENABLED, DEMO_TOKEN } from "../config.js";
import { authApi, usersApi } from "../lib/api.js";
import { onUnauthorized, tokenStore } from "../lib/http.js";

const AuthContext = createContext(null);
const DEMO_USER = {
  id: "demo",
  username: "Visitante",
  email: "demo@confereantes.local",
  phone: null,
  createdAt: "2026-01-01T00:00:00Z",
};

export function AuthProvider({ children }) {
  const [state, setState] = useState(() => {
    const token = tokenStore.get();
    const isDemoSession = DEMO_ENABLED && token === DEMO_TOKEN;
    return {
      status: isDemoSession ? "authenticated" : token && token !== DEMO_TOKEN ? "loading" : "anonymous",
      user: isDemoSession ? DEMO_USER : null,
      isDemo: isDemoSession,
    };
  });

  // Restaura a sessão se houver token guardado.
  useEffect(() => {
    const token = tokenStore.get();
    if (!token) return undefined;
    if (token === DEMO_TOKEN) {
      if (!DEMO_ENABLED) tokenStore.set(null);
      return undefined;
    }
    let cancelled = false;
    usersApi
      .me()
      .then((user) => !cancelled && setState({ status: "authenticated", user, isDemo: false }))
      .catch(() => {
        if (cancelled) return;
        tokenStore.set(null);
        setState({ status: "anonymous", user: null, isDemo: false });
      });
    return () => { cancelled = true; };
  }, []);

  // Token rejeitado pela API (expirado/inválido): volta para o login.
  useEffect(() => onUnauthorized(() => setState({ status: "anonymous", user: null, isDemo: false })), []);

  const login = useCallback(async (email, password) => {
    const { token } = await authApi.login(email, password);
    tokenStore.set(token);
    try {
      const user = await usersApi.me();
      setState({ status: "authenticated", user, isDemo: false });
    } catch (err) {
      tokenStore.set(null);
      throw err;
    }
  }, []);

  const loginDemo = useCallback(() => {
    if (!DEMO_ENABLED) throw new Error("O modo de demonstração está desativado.");
    tokenStore.set(DEMO_TOKEN);
    setState({ status: "authenticated", user: DEMO_USER, isDemo: true });
  }, []);

  const logout = useCallback(() => {
    tokenStore.set(null);
    setState({ status: "anonymous", user: null, isDemo: false });
  }, []);

  const value = useMemo(() => ({ ...state, login, loginDemo, logout }), [state, login, loginDemo, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth deve ser usado dentro de <AuthProvider>");
  return ctx;
}
