import { useContext } from "react";

import AuthContext from "../context/AuthContext";

function useAuth() {
  const authContext = useContext(AuthContext);

  if (authContext === undefined) {
    throw new Error(
      "useAuth phải được sử dụng bên trong AuthProvider.",
    );
  }

  return authContext;
}

export default useAuth;