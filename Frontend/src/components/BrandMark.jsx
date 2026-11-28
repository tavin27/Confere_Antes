import Icon from "./Icon.jsx";

export default function BrandMark() {
  return (
    <div className="brand-mark" aria-hidden="true">
      <span className="brand-mark__core"><Icon name="shield" size={38} /></span>
      <span className="brand-mark__spark" />
    </div>
  );
}
