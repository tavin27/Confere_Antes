import { describe, expect, it } from "vitest";
import { clampScore, riskLevel } from "./risk.js";

describe("riskLevel", () => {
  it("devolve null sem score", () => {
    expect(riskLevel(null)).toBeNull();
    expect(riskLevel(undefined)).toBeNull();
  });
  it("classifica as faixas", () => {
    expect(riskLevel(10)).toBe("low");
    expect(riskLevel(30)).toBe("medium");
    expect(riskLevel(60)).toBe("high");
    expect(riskLevel(87)).toBe("critical");
  });
  it("limita o score entre 0 e 100", () => {
    expect(clampScore(150)).toBe(100);
    expect(clampScore(-3)).toBe(0);
  });
});
