import PasswordGenerator from "../components/PasswordGenerator.jsx";
import Screen from "../components/Screen.jsx";

export default function AccountSecurity() {
  return (
    <Screen sub="Seu perfil" title="Segurança da conta" backTo="/perfil">
      <section className="card stack">
        <h2>Gerar senha forte</h2>
        <p className="muted">Crie uma senha aleatória para usar onde preferir. Ela não será salva automaticamente.</p>
        <PasswordGenerator helpText="Esta ferramenta apenas gera e mostra a senha; ela não altera a senha da sua conta." />
      </section>
    </Screen>
  );
}
