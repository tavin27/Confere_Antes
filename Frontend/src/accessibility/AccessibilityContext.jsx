import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useAuth } from "../auth/AuthContext.jsx";
import { settingsStore } from "../lib/storage.js";

const AccessibilityContext = createContext(null);
const DEFAULTS = { fontSize: "normal", highContrast: false, reduceMotion: false, colorBlindMode: false };

export function AccessibilityProvider({ children }) {
  const { user } = useAuth();
  const [prefs, setPrefs] = useState(DEFAULTS);

  useEffect(() => {
    setPrefs(user?.email ? settingsStore.get(user.email) : DEFAULTS);
  }, [user?.email]);

  useEffect(() => {
    const root = document.documentElement;
    root.dataset.fontSize = prefs.fontSize;
    root.dataset.highContrast = String(prefs.highContrast);
    root.dataset.reduceMotion = String(prefs.reduceMotion);
    root.dataset.colorBlind = String(prefs.colorBlindMode);
  }, [prefs.fontSize, prefs.highContrast, prefs.reduceMotion, prefs.colorBlindMode]);

  const update = useCallback((key) => (value) => {
    setPrefs((current) => {
      const next = { ...current, [key]: value };
      if (user?.email) settingsStore.set(user.email, { ...settingsStore.get(user.email), ...next });
      return next;
    });
  }, [user?.email]);

  const value = useMemo(() => ({ prefs, update }), [prefs, update]);
  return <AccessibilityContext.Provider value={value}>{children}</AccessibilityContext.Provider>;
}

export function useAccessibility() {
  const context = useContext(AccessibilityContext);
  if (!context) throw new Error("useAccessibility deve ser usado dentro de <AccessibilityProvider>");
  return context;
}