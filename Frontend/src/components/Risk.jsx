import { RISK_LABELS, clampScore, riskLevel } from "../lib/risk.js";

export function RiskChip({ score }) {
  const level = riskLevel(score);
  return level ? <span className={`chip chip--${level}`}>{RISK_LABELS[level]}</span> : null;
}

export function RiskMeter({ score }) {
  const value = clampScore(score);
  const level = riskLevel(value);
  return (
    <div className={`meter meter--${level}`} role="meter" aria-label="Score de risco"
      aria-valuemin={0} aria-valuemax={100} aria-valuenow={value}
      aria-valuetext={`${value} de 100, risco ${RISK_LABELS[level].toLowerCase()}`}>
      <div style={{ width: `${value}%` }} />
    </div>
  );
}
