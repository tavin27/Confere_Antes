import { Link } from "react-router-dom";
import { formatDay } from "../lib/format.js";
import { channelInfo } from "../lib/risk.js";
import Icon from "./Icon.jsx";
import { RiskChip } from "./Risk.jsx";

export default function SessionRow({ session }) {
  const channel = channelInfo(session.channel);
  return (
    <li>
      <Link to={`/analisar/${session.id}`} className="item">
        <span className="avatar avatar--soft"><Icon name={channel.icon} size={20} /></span>
        <span className="grow">
          <strong className="ellipsis">{channel.label} · {session.preview}</strong>
          <small>{formatDay(session.createdAt)}</small>
        </span>
        <RiskChip score={session.score} />
      </Link>
    </li>
  );
}
