export default function Loader({ label = "Carregando…" }) {
  return <div className="loader" role="status">{label}</div>;
}
