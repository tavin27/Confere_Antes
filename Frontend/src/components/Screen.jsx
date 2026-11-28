import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";
import BottomNav from "./BottomNav.jsx";
import Icon from "./Icon.jsx";

/** Estrutura das telas internas: cabeçalho vermelho, conteúdo sobreposto e menu inferior. */
export default function Screen({ sub, title, backTo, headerExtra, heroClassName = "", children }) {
  const { isDemo } = useAuth();

  return (
    <div className="app">
      <a className="skip-link" href="#main-content">Pular para o conteúdo</a>
      <div className="scroll">
        <header className={`hero ${heroClassName}`}>
          <div className="hero__row">
            {backTo && <Link to={backTo} className="back" aria-label="Voltar"><Icon name="back" /></Link>}
            <div>
              <p className="hero__sub">{sub}</p>
              <h1 className="hero__title">{title}</h1>
            </div>
          </div>
          {headerExtra}
        </header>
        <main className="content" id="main-content" tabIndex="-1">
          {isDemo && <p className="notice small" role="status">Demonstração: dados fictícios e análises simuladas; nada é enviado ao backend.</p>}
          {children}
        </main>
      </div>
      <BottomNav />
    </div>
  );
}
