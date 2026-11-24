import { Navigate, Outlet, useLocation } from "react-router-dom";
import Loader from "../components/Loader.jsx";
import { useAuth } from "./AuthContext.jsx";

export function RequireAuth() {
  const { status } = useAuth();
  const location = useLocation();
  if (status === "loading") return <Loader />;
  if (status === "anonymous") return <Navigate to="/login" replace state={{ from: location }} />;
  return <Outlet />;
}

export function PublicOnly() {
  const { status } = useAuth();
  const location = useLocation();
  if (status === "loading") return <Loader />;
  if (status === "authenticated") return <Navigate to={location.state?.from?.pathname ?? "/"} replace />;
  return <Outlet />;
}
