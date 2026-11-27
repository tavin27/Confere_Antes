import { request } from "./http.js";
import { DEMO_MODE } from "../config.js";

const DEMO_ALERTS_KEY = "ca:demo-alerts";

function readDemoAlerts() {
  try {
    return JSON.parse(localStorage.getItem(DEMO_ALERTS_KEY) || "{}");
  } catch {
    return {};
  }
}

function writeDemoAlerts(alerts) {
  try { localStorage.setItem(DEMO_ALERTS_KEY, JSON.stringify(alerts)); } catch { /* armazenamento local indisponível */ }
}

function createDemoAlert(contactChannel, contactValue) {
  const id = Date.now();
  const createdAt = new Date().toISOString();
  const alerts = readDemoAlerts();
  alerts[id] = { id, contactChannel, contactValue: contactValue || null, createdAt, messages: [] };
  writeDemoAlerts(alerts);
  return { id, contactChannel, contactValue: contactValue || null, overallRiskScore: null, createdAt };
}

function readDemoMessages(alertId) {
  return readDemoAlerts()[alertId]?.messages ?? [];
}

function saveDemoMessage(alertId, content, sender) {
  const alerts = readDemoAlerts();
  const alert = alerts[alertId] ?? { id: alertId, messages: [] };
  const message = { id: alert.messages.length + 1, content, sender, source: "DEMO", createdAt: new Date().toISOString() };
  alerts[alertId] = { ...alert, messages: [...alert.messages, message] };
  writeDemoAlerts(alerts);
  return message;
}

/*
 * Contrato real do back-end (Spring Boot, Configs/):
 *   POST /auth/login               {email, password}                 -> {token, type}
 *   POST /users                    {username, email, phone?, password} -> UserResponse
 *   GET  /users/me                                                    -> {id, username, email, phone, createdAt}
 *   POST /alerts                   {contactChannel, contactValue?}    -> {id, contactChannel, contactValue, overallRiskScore, createdAt}
 *   GET  /alerts/{id}/messages                                        -> [{id, content, sender, source, createdAt}]
 *   POST /alerts/{id}/messages     {content, sender: USUARIO|CONTATO} -> MessageResponse
 */
export const authApi = {
  login: (email, password) => request("/auth/login", { method: "POST", body: { email, password }, auth: false }),
};

export const usersApi = {
  register: ({ username, email, phone, password }) =>
    request("/users", { method: "POST", body: { username, email, phone: phone || undefined, password }, auth: false }),
  me: () => request("/users/me"),
};

export const alertsApi = {
  create: (contactChannel, contactValue) =>
    DEMO_MODE
      ? Promise.resolve(createDemoAlert(contactChannel, contactValue))
      : request("/alerts", { method: "POST", body: { contactChannel, contactValue: contactValue || undefined } }),
  messages: (alertId) => DEMO_MODE ? Promise.resolve(readDemoMessages(alertId)) : request(`/alerts/${alertId}/messages`),
  sendMessage: (alertId, content, sender = "USUARIO") =>
    DEMO_MODE
      ? Promise.resolve(saveDemoMessage(alertId, content, sender))
      : request(`/alerts/${alertId}/messages`, { method: "POST", body: { content, sender } }),
};
