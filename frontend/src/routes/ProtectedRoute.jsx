import {Navigate,Outlet,useLocation,} from "react-router-dom";

import useAuth from "../features/auth/hooks/useAuth";

function ProtectedRoute() {
  const {
    isAuthenticated,
    isInitializing,
  } = useAuth();

  const location = useLocation();

  if (isInitializing) {
    return <p>Đang kiểm tra phiên đăng nhập...</p>;
  }

  if (!isAuthenticated) {
    const redirectPath = `${location.pathname}${location.search}`;

    return (
      <Navigate
        to="/login"
        replace
        state={{ redirectPath }}
      />
    );
  }

  return <Outlet />;
}

export default ProtectedRoute;