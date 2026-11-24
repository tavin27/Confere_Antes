import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { DEMO_MODE } from "../config.js";
import { authApi, usersApi } from "../lib/api.js";
import { onUnauthorized, tokenStore } from "../lib/http.js";

const AuthContext = createContext(null);
const DEMO_TOKEN = "local-demo-session";
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
    const isDemoSession = DEMO_MODE && token === DEMO_TOKEN;
    return {
      status: isDemoSession ? "authenticated" : token ? "loading" : "anonymous",
      user: isDemoSession ? DEMO_USER : null,
    };
  });

  // Restaura a sessão se houver token guardado.
  useEffect(() => {
    const token = tokenStore.get();
    if (!token || (DEMO_MODE && token === DEMO_TOKEN)) return undefined;
    let cancelled = false;
    usersApi
      .me()
      .then((user) => !cancelled && setState({ status: "authenticated", user }))
      .catch(() => {
        if (cancelled) return;
        tokenStore.set(null);
        setState({ status: "anonymous", user: null });
      });
    return () => { cancelled = true; };
  }, []);

  // Token rejeitado pela API (expirado/inválido): volta para o login.
  useEffect(() => onUnauthorized(() => setState({ status: "anonymous", user: null })), []);

  const login = useCallback(async (email, password) => {
    const { token } = await authApi.login(email, password);
    tokenStore.set(token);
    try {
      const user = await usersApi.me();
      setState({ status: "authenticated", user });
    } catch (err) {
      tokenStore.set(null);
      throw err;
    }
  }, []);

  const loginDemo = useCallback(() => {
    if (!DEMO_MODE) throw new Error("O modo de demonstração está desativado.");
    tokenStore.set(DEMO_TOKEN);
    setState({ status: "authenticated", user: DEMO_USER });
  }, []);

  const logout = useCallback(() => {
    tokenStore.set(null);
    setState({ status: "anonymous", user: null });
  }, []);

  const value = useMemo(() => ({ ...state, login, loginDemo, logout }), [state, login, loginDemo, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth deve ser usado dentro de <AuthProvider>");
  return ctx;
}
