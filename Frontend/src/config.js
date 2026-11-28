// Configuração lida em tempo de build. O padrão "/api" usa o proxy (Vite/nginx).
export const API_BASE = (import.meta.env?.VITE_API_BASE_URL || "/api").replace(/\/$/, "");
export const DEMO_ENABLED = import.meta.env?.VITE_DEMO_MODE === "true";
export const DEMO_TOKEN = "local-demo-session";
