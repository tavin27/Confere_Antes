import { useEffect, useRef, useState } from "react";
import Field from "../components/Field.jsx";
import Screen from "../components/Screen.jsx";
import { useAuth } from "../auth/AuthContext.jsx";
import { trustedContactsStore } from "../lib/storage.js";

const EMPTY_CONTACT = { name: "", phone: "", relationship: "" };

function createContactId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

export default function TrustedContacts() {
  const { user } = useAuth();
  const [contacts, setContacts] = useState([]);
  const [form, setForm] = useState(EMPTY_CONTACT);
  const [editingId, setEditingId] = useState(null);
  const [loadError, setLoadError] = useState("");
  const [saveError, setSaveError] = useState("");
  const editorRef = useRef(null);

  useEffect(() => {
    setContacts([]);
    setLoadError("");
    try {
      setContacts(trustedContactsStore.list(user.email));
    } catch {
      setLoadError("Não foi possível carregar os contatos salvos neste aparelho.");
    }
  }, [user.email]);

  function updateField(key) {
    return (event) => setForm((current) => ({ ...current, [key]: event.target.value }));
  }

  function saveContacts(nextContacts) {
    try {
      trustedContactsStore.save(user.email, nextContacts);
      setContacts(nextContacts);
      setSaveError("");
      return true;
    } catch {
      setSaveError("Não foi possível salvar. Confira o espaço disponível no aparelho e tente novamente.");
      return false;
    }
  }

  function submit(event) {
    event.preventDefault();
    const name = form.name.trim();
    const phone = form.phone.trim();
    if (!name || !phone) {
      setSaveError("Informe o nome e o telefone do contato.");
      return;
    }

    const contact = {
      id: editingId || createContactId(),
      name,
      phone,
      relationship: form.relationship.trim(),
    };
    const nextContacts = editingId
      ? contacts.map((current) => current.id === editingId ? contact : current)
      : [...contacts, contact];

    if (saveContacts(nextContacts)) {
      setForm(EMPTY_CONTACT);
      setEditingId(null);
    }
  }

  function editContact(contact) {
    setForm({ name: contact.name, phone: contact.phone, relationship: contact.relationship || "" });
    setEditingId(contact.id);
    setSaveError("");
    editorRef.current?.scrollIntoView({ block: "center" });
  }

  function removeContact(id) {
    if (!saveContacts(contacts.filter((contact) => contact.id !== id))) return;
    if (editingId === id) {
      setForm(EMPTY_CONTACT);
      setEditingId(null);
    }
  }

  return (
    <Screen sub="Seu perfil" title="Contatos de confiança" backTo="/perfil">
      {loadError && <p className="error" role="alert">{loadError}</p>}
      <section className="card stack" ref={editorRef}>
        <h2>{editingId ? "Editar contato" : "Adicionar contato"}</h2>
        <form className="stack" onSubmit={submit}>
          <Field label="Nome" type="text" value={form.name} onChange={updateField("name")}
            autoComplete="name" maxLength={80} required disabled={Boolean(loadError)} />
          <Field label="Telefone" type="tel" inputMode="tel" value={form.phone} onChange={updateField("phone")}
            autoComplete="tel" placeholder="(11) 99999-9999" maxLength={30} required disabled={Boolean(loadError)} />
          <Field label="Relação com você (opcional)" type="text" value={form.relationship}
            onChange={updateField("relationship")} maxLength={60} disabled={Boolean(loadError)} />
          {saveError && <p className="error" role="alert">{saveError}</p>}
          <button className="btn" type="submit" disabled={Boolean(loadError)}>
            {editingId ? "Salvar alterações" : "Adicionar contato"}
          </button>
          {editingId && (
            <button className="btn btn--outline" type="button" onClick={() => {
              setForm(EMPTY_CONTACT);
              setEditingId(null);
              setSaveError("");
            }}>
              Cancelar edição
            </button>
          )}
        </form>
        <p className="muted small">Os dados ficam salvos neste aparelho e não enviam alertas automaticamente.</p>
      </section>

      <section className="card">
        <h2>Contatos cadastrados</h2>
        {contacts.length === 0 ? (
          <p className="muted" style={{ marginTop: 10 }}>Você ainda não adicionou contatos de confiança.</p>
        ) : (
          <ul className="list" aria-label="Contatos de confiança">
            {contacts.map((contact) => (
              <li className="trusted-contact" key={contact.id}>
                <div className="trusted-contact__details">
                  <strong>{contact.name}</strong>
                  {contact.relationship && <span className="trusted-contact__relationship">{contact.relationship}</span>}
                  <span className="trusted-contact__phone">{contact.phone}</span>
                </div>
                <div className="trusted-contact__actions">
                  <button className="link-btn link-btn--inline" type="button"
                    onClick={() => editContact(contact)} disabled={Boolean(loadError)}
                    aria-label={`Editar ${contact.name}`}>
                    Editar
                  </button>
                  <button className="link-btn link-btn--inline" type="button"
                    onClick={() => removeContact(contact.id)} disabled={Boolean(loadError)}
                    aria-label={`Remover ${contact.name}`}>
                    Remover
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </Screen>
  );
}
