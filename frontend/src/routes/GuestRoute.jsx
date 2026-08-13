import {Navigate, Outlet, useLocation,} from "react-router-dom";

import useAuth from "../features/auth/hooks/useAuth";

function GuestRoute() {
  const {
    isAuthenticated,
    isInitializing,
  } = useAuth();

  const location = useLocation();

  if (isInitializing) {
    return <p>Đang kiểm tra phiên đăng nhập...</p>;
  }

  if (isAuthenticated) {
    const redirectPath = location.state?.redirectPath || "/";

    return (
      <Navigate
        to={redirectPath}
        replace
      />
    );
  }

  return <Outlet />;
}

export default GuestRoute;