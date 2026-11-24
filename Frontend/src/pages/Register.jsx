import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";
import Field from "../components/Field.jsx";
import Icon from "../components/Icon.jsx";
import { DEMO_MODE } from "../config.js";
import { usersApi } from "../lib/api.js";

export default function Register() {
  const { login } = useAuth();
  const [form, setForm] = useState({ username: "", email: "", phone: "", password: "", confirm: "" });
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const set = (key) => (event) => setForm((f) => ({ ...f, [key]: event.target.value }));

  async function onSubmit(event) {
    event.preventDefault();
    if (busy) return;
    setError("");
    if (form.password !== form.confirm) {
      setFieldErrors({ confirm: "As senhas não conferem." });
      return;
    }
    setFieldErrors({});
    setBusy(true);
    const email = form.email.trim();
    try {
      await usersApi.register({ username: form.username.trim(), email, phone: form.phone.trim(), password: form.password });
    } catch (err) {
      setFieldErrors(err.fields ?? {});
      setError(err.fields ? "Confira os campos destacados." : err.message);
      setBusy(false);
      return;
    }
    try {
      await login(email, form.password); // entra direto após criar a conta
    } catch (err) {
      setError(`Conta criada, mas não foi possível entrar: ${err.message}`);
      setBusy(false);
    }
  }

  return (
    <div className="app">
      <a className="skip-link" href="#main-content">Pular para o conteúdo</a>
      <div className="scroll">
        <header className="hero hero--login">
          <div className="logo"><Icon name="shield" size={34} /></div>
          <h1 className="hero__title">Criar conta</h1>
          <p>Leva menos de um minuto.</p>
        </header>
        <main className="content content--login" id="main-content" tabIndex="-1">
          {DEMO_MODE ? (
            <section className="card stack">
              <h2>Cadastro indisponível na demonstração</h2>
              <p className="muted">Volte ao login e escolha “Entrar em modo demonstração”. Nenhuma conta real será criada.</p>
              <Link to="/login" className="btn">Voltar ao login</Link>
            </section>
          ) : (
          <form className="card stack" onSubmit={onSubmit}>
            <Field label="Nome de usuário" value={form.username} onChange={set("username")} autoComplete="name" required error={fieldErrors.username} />
            <Field label="E-mail" type="email" value={form.email} onChange={set("email")} autoComplete="email" required error={fieldErrors.email} />
            <Field label="Celular (opcional)" type="tel" value={form.phone} onChange={set("phone")} autoComplete="tel" error={fieldErrors.phone} />
            <Field label="Senha (8 a 72 caracteres)" type="password" value={form.password} onChange={set("password")}
              autoComplete="new-password" minLength={8} maxLength={72} required error={fieldErrors.password} />
            <Field label="Confirmar senha" type="password" value={form.confirm} onChange={set("confirm")}
              autoComplete="new-password" required error={fieldErrors.confirm} />
            {error && <p className="error" role="alert">{error}</p>}
            <button className="btn" disabled={busy}>{busy ? "Criando…" : "Criar conta"}</button>
          </form>
          )}
          <p className="muted small center">
            Já tem conta? <Link to="/login" className="link-btn link-btn--inline"><strong>Entrar</strong></Link>
          </p>
        </main>
      </div>
    </div>
  );
}
