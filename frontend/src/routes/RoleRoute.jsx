import {Navigate, Outlet, useLocation,} from "react-router-dom";

import useAuth from "../features/auth/hooks/useAuth";

function RoleRoute({ allowedRoles }) {
  const {
    user,
    isAuthenticated,
    isInitializing,
  } = useAuth();

  const location = useLocation();

  if (isInitializing) {
    return <p>Đang kiểm tra quyền truy cập...</p>;
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

  if (!allowedRoles.includes(user?.role)) {
    return <Navigate to="/forbidden" replace />;
  }

  return <Outlet />;
}

export default RoleRoute;