export const CHANNELS = [
  { value: "LIGACAO", label: "Ligação", icon: "phone" },
  { value: "WHATSAPP", label: "WhatsApp", icon: "msg" },
  { value: "SMS", label: "SMS", icon: "msg" },
  { value: "EMAIL", label: "E-mail", icon: "mail" },
];

export const channelInfo = (value) => CHANNELS.find((c) => c.value === value) ?? CHANNELS[0];

export const RISK_LABELS = { low: "Baixo", medium: "Médio", high: "Alto", critical: "Crítico" };

export const clampScore = (score) => Math.max(0, Math.min(100, Math.round(Number(score))));

/** Faixas de exibição para o score (0 a 100) do back-end. Ajuste aqui se a regra do servidor mudar. */
export function riskLevel(score) {
  if (score === null || score === undefined || Number.isNaN(Number(score))) return null;
  const n = Number(score);
  if (n >= 80) return "critical";
  if (n >= 60) return "high";
  if (n >= 30) return "medium";
  return "low";
}
