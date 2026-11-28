import { describe, expect, it } from "vitest";
import { generatePassword } from "./password.js";

describe("generatePassword", () => {
  it("creates a 20-character password with each character class", () => {
    const password = generatePassword();
    expect(password).toHaveLength(20);
    expect(password).toMatch(/[A-Z]/);
    expect(password).toMatch(/[a-z]/);
    expect(password).toMatch(/[0-9]/);
    expect(password).toMatch(/[!@#$%&*+\-=?_]/);
  });

  it("rejects lengths outside the supported range", () => {
    expect(() => generatePassword(3)).toThrow(RangeError);
    expect(() => generatePassword(73)).toThrow(RangeError);
  });
});
