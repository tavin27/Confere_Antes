import { useEffect, useState } from "react";
import { generatePassword } from "../lib/password.js";

export default function PasswordGenerator({ onGenerate, generatedValue, helpText }) {
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [status, setStatus] = useState("");

  useEffect(() => {
    if (password && generatedValue !== undefined && generatedValue !== password) {
      setPassword("");
      setStatus("");
    }
  }, [generatedValue, password]);

  function generate() {
    try {
      const nextPassword = generatePassword();
      setPassword(nextPassword);
      setError("");
      setStatus("");
      onGenerate?.(nextPassword);
    } catch {
      setError("Não foi possível gerar uma senha segura neste navegador.");
    }
  }

  async function copyPassword() {
    try {
      await navigator.clipboard.writeText(password);
      setStatus("Senha copiada.");
    } catch {
      setStatus("Não foi possível copiar automaticamente. Selecione e copie a senha exibida.");
    }
  }

  return (
    <div className="password-generator">
      <button className="btn btn--outline password-generator__button" type="button" onClick={generate}>
        {password ? "Gerar outra senha" : "Gerar senha aleatória"}
      </button>
      {error && <p className="error" role="alert">{error}</p>}
      {password && (
        <div className="password-generator__result" aria-live="polite">
          <label htmlFor="generated-password">Senha gerada</label>
          <input id="generated-password" className="password-generator__value" type="text"
            value={password} readOnly autoComplete="off" />
          <button className="link-btn link-btn--inline" type="button" onClick={copyPassword}>
            Copiar senha
          </button>
          {helpText && <p className="muted small">{helpText}</p>}
          {status && <p className="password-generator__status" role="status">{status}</p>}
        </div>
      )}
    </div>
  );
}
