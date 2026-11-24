import { useId } from "react";

export default function Field({ label, error, ...input }) {
  const id = useId();
  const errorId = `${id}-erro`;
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} aria-invalid={error ? "true" : undefined} aria-describedby={error ? errorId : undefined} {...input} />
      {error && <p id={errorId} className="field__error">{error}</p>}
    </div>
  );
}
