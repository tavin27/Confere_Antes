import { describe, expect, it } from "vitest";
import { parseError } from "./http.js";

const res = (status, body) => new Response(body, { status });

describe("parseError", () => {
  it("usa o campo message", async () => {
    const e = await parseError(res(401, JSON.stringify({ message: "Credenciais inválidas" })));
    expect(e.message).toBe("Credenciais inválidas");
  });
  it("lê erros de validação por campo", async () => {
    const e = await parseError(res(400, JSON.stringify({ email: "Informe um e-mail válido" })));
    expect(e.fields.email).toBe("Informe um e-mail válido");
    expect(e.message).toBe("Informe um e-mail válido");
  });
  it("aceita texto puro (HTTP 409)", async () => {
    const e = await parseError(res(409, "E-mail já cadastrado"));
    expect(e.message).toBe("E-mail já cadastrado");
  });
  it("não exibe páginas HTML de erro", async () => {
    const e = await parseError(res(502, "<html>Bad gateway</html>"));
    expect(e.message).toMatch(/indisponível/);
  });
});
