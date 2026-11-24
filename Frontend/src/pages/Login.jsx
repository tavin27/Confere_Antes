import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";
import Field from "../components/Field.jsx";
import Icon from "../components/Icon.jsx";
import { DEMO_MODE } from "../config.js";

export default function Login() {
  const { login, loginDemo } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [showReset, setShowReset] = useState(false);

  async function onSubmit(event) {
    event.preventDefault();
    if (busy) return;
    setError("");
    setBusy(true);
    try {
      await login(email.trim(), password); // ao autenticar, PublicOnly redireciona
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  }

  return (
    <div className="app">
      <a className="skip-link" href="#main-content">Pular para o conteúdo</a>
      <div className="scroll">
        <header className="hero hero--login">
          <div className="logo"><Icon name="shield" size={34} /></div>
          <h1 className="hero__title">Confere Antes</h1>
          <p>Confirme antes de agir. Proteja-se do golpe da falsa central.</p>
        </header>
        <main className="content content--login" id="main-content" tabIndex="-1">
          {DEMO_MODE && <p className="notice" role="status">Demonstração local ativa: os dados exibidos são fictícios.</p>}
          <form className="card stack" onSubmit={onSubmit} noValidate={false}>
            <h2>Acesse sua conta</h2>
            <Field label="E-mail" type="email" value={email} onChange={(e) => setEmail(e.target.value)}
              placeholder="nome@exemplo.com" autoComplete="username" required />
            <Field label="Senha" type="password" value={password} onChange={(e) => setPassword(e.target.value)}
              placeholder="Sua senha" autoComplete="current-password" required />
            {error && <p className="error" role="alert">{error}</p>}
            <button className="btn" disabled={busy}>{busy ? "Entrando…" : "Entrar"}</button>
            {DEMO_MODE && <button type="button" className="btn btn--outline" onClick={loginDemo}>Entrar em modo demonstração</button>}
            <button type="button" className="link-btn" onClick={() => setShowReset((v) => !v)} aria-expanded={showReset}>
              Esqueci minha senha
            </button>
            {showReset && (
              <p className="notice" role="status">
                A recuperação de senha ainda não está disponível. Fale com o suporte do Confere Antes.
              </p>
            )}
          </form>
          <p className="muted small center">
            Novo por aqui? <Link to="/cadastro" className="link-btn link-btn--inline"><strong>Criar conta</strong></Link>
          </p>
          <p className="muted small center">O banco nunca pede senha ou token por telefone.</p>
        </main>
      </div>
    </div>
  );
}
