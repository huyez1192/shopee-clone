import { useCallback, useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { getCurrentUserRequest, loginRequest, registerRequest } from "../api/authApi";
import { AUTH_UNAUTHORIZED_EVENT } from "../constants/authEvents";
import { clearAuthStorage, getAccessToken, getStoredUser, saveAuthStorage } from "../utils/authStorage";
import { queryKeys } from "../../../query/queryKeys";

import AuthContext from "./AuthContext";

function createUserFromAuthResponse(authResponse) {
  return {
    email: authResponse.email,
    role: authResponse.role,
  };
}

function AuthProvider({ children }) {
  const queryClient = useQueryClient();

  const [token, setToken] = useState(getAccessToken);
  const [sessionUser, setSessionUser] = useState(getStoredUser);

  const {
    data: currentUser,
    isPending: isCurrentUserPending,
    isError: isCurrentUserError,
  } = useQuery({
    queryKey: queryKeys.auth.currentUser,
    queryFn: async () => {
      try {
        const authenticatedUser = await getCurrentUserRequest();

        saveAuthStorage({ token, user: authenticatedUser });
        return authenticatedUser;
      } catch (error) {
        clearAuthStorage();
        setToken(null);
        setSessionUser(null);
        throw error;
      }
    },
    enabled: Boolean(token),
    retry: false,
  });

  const loginMutation = useMutation({
    mutationFn: loginRequest,
  });

  const registerMutation = useMutation({
    mutationFn: registerRequest,
  });

  const user = currentUser ?? sessionUser;
  const isInitializing = Boolean(token) && isCurrentUserPending;
  const isAuthenticated = Boolean(token && user && !isCurrentUserError);

  const clearPrivateQueryCache = useCallback(() => {
    queryClient.removeQueries({ queryKey: queryKeys.auth.currentUser });
    queryClient.removeQueries({ queryKey: queryKeys.cart.all });
    queryClient.removeQueries({ queryKey: queryKeys.addresses.all });
    queryClient.removeQueries({ queryKey: ["checkout"] });
    queryClient.removeQueries({ queryKey: queryKeys.orders.all });
    queryClient.removeQueries({ queryKey: queryKeys.notifications.all });
    queryClient.removeQueries({ queryKey: ["payment"] });
    queryClient.removeQueries({ queryKey: ["seller"] });
  }, [queryClient]);

  const clearAuthState = useCallback(() => {
    clearAuthStorage();
    setToken(null);
    setSessionUser(null);
    clearPrivateQueryCache();
  }, [clearPrivateQueryCache]);

  useEffect(() => {
    function handleUnauthorized() {
      clearAuthStorage();
      setToken(null);
      setSessionUser(null);
      clearPrivateQueryCache();
    }

    window.addEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized);

    return () => {
      window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized);
    };
  }, [clearPrivateQueryCache]);

  async function establishSession(authResponse) {
    if (!authResponse?.token) {
      throw new Error("Backend không trả về JWT.");
    }

    const temporaryUser = createUserFromAuthResponse(authResponse);

    saveAuthStorage({
      token: authResponse.token,
      user: temporaryUser,
    });

    try {
      queryClient.removeQueries({ queryKey: queryKeys.auth.currentUser });

      const authenticatedUser = await queryClient.fetchQuery({
        queryKey: queryKeys.auth.currentUser,
        queryFn: getCurrentUserRequest,
        staleTime: 0,
      });

      saveAuthStorage({
        token: authResponse.token,
        user: authenticatedUser,
      });

      setToken(authResponse.token);
      setSessionUser(authenticatedUser);

      return authenticatedUser;
    } catch (error) {
      clearAuthState();
      throw error;
    }
  }

  async function login(credentials) {
    const authResponse = await loginMutation.mutateAsync(credentials);
    await establishSession(authResponse);
    return authResponse;
  }

  async function register(registerData) {
    const authResponse = await registerMutation.mutateAsync(registerData);
    await establishSession(authResponse);
    return authResponse;
  }

  function logout() {
    clearAuthState();
  }

  function hasRole(...acceptedRoles) {
    if (!user?.role) {
      return false;
    }

    return acceptedRoles.includes(user.role);
  }

  const contextValue = {
    token,
    user,
    isAuthenticated,
    isInitializing,
    login,
    register,
    logout,
    hasRole,
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
}

export default AuthProvider;
