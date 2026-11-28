import { describe, expect, it } from "vitest";
import { hasEmergencyKeywords } from "./emergency.js";

describe("hasEmergencyKeywords", () => {
  it("detects accented keywords and related danger terms", () => {
    expect(hasEmergencyKeywords("A perseguição começou ontem")).toBe(true);
    expect(hasEmergencyKeywords("Estou sendo perseguido")).toBe(true);
    expect(hasEmergencyKeywords("Alguém está na porta")).toBe(true);
    expect(hasEmergencyKeywords("Minha família está em perigo")).toBe(true);
  });

  it("does not match keywords embedded in unrelated words", () => {
    expect(hasEmergencyKeywords("A porta-voz ligou")).toBe(false);
    expect(hasEmergencyKeywords("O aplicativo está armazenado")).toBe(false);
  });
});
