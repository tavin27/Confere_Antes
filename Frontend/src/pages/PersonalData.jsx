import { useAuth } from "../auth/AuthContext.jsx";
import Screen from "../components/Screen.jsx";
import { formatDateTime } from "../lib/format.js";

export default function PersonalData() {
  const { user } = useAuth();
  return (
    <Screen sub="Seu perfil" title="Dados pessoais" backTo="/perfil">
      <section className="card">
        <dl className="data" style={{ margin: 0 }}>
          <dt>Nome de usuário</dt><dd>{user.username}</dd>
          <dt>E-mail</dt><dd>{user.email}</dd>
          <dt>Celular</dt><dd>{user.phone || "Não informado"}</dd>
          <dt>Conta criada em</dt><dd>{formatDateTime(user.createdAt) || "—"}</dd>
        </dl>
        <p className="muted small" style={{ marginTop: 16 }}>A edição dos dados ainda não está disponível.</p>
      </section>
    </Screen>
  );
}
