import { useEffect, useState } from "react";

import {getCurrentUserRequest, loginRequest, registerRequest,} from "../api/authApi";

import { AUTH_UNAUTHORIZED_EVENT } from "../constants/authEvents";

import { clearAuthStorage, getAccessToken, getStoredUser, saveAuthStorage,} from "../utils/authStorage";

import AuthContext from "./AuthContext";

function createUserFromAuthResponse(authResponse) {
  return {
    email: authResponse.email,
    role: authResponse.role,
  };
}

function AuthProvider({ children }) {
  const [token, setToken] = useState(getAccessToken);
  const [user, setUser] = useState(getStoredUser);
  const [isInitializing, setIsInitializing] = useState(true);

  const isAuthenticated = Boolean(token && user);

  function clearAuthState() {
    clearAuthStorage();
    setToken(null);
    setUser(null);
  }

  useEffect(() => {
    let isCancelled = false;

    function handleUnauthorized() {
      if (isCancelled) {
        return;
      }

      setToken(null);
      setUser(null);
      setIsInitializing(false);
    }

    async function initializeAuth() {
      const storedToken = getAccessToken();

      if (!storedToken) {
        setToken(null);
        setUser(null);
        setIsInitializing(false);
        return;
      }

      try {
        const currentUser = await getCurrentUserRequest();

        if (isCancelled) {
          return;
        }

        saveAuthStorage({
          token: storedToken,
          user: currentUser,
        });

        setToken(storedToken);
        setUser(currentUser);
      } catch (error) {
        if (!isCancelled) {
          console.error(
            "Không thể khôi phục phiên đăng nhập.",
            error,
          );

          clearAuthState();
        }
      } finally {
        if (!isCancelled) {
          setIsInitializing(false);
        }
      }
    }

    window.addEventListener(
      AUTH_UNAUTHORIZED_EVENT,
      handleUnauthorized,
    );

    initializeAuth();

    return () => {
      isCancelled = true;

      window.removeEventListener(
        AUTH_UNAUTHORIZED_EVENT,
        handleUnauthorized,
      );
    };
  }, []);

  async function establishSession(authResponse) {
    if (!authResponse?.token) {
      throw new Error("Backend không trả về JWT.");
    }

    const temporaryUser =
      createUserFromAuthResponse(authResponse);

    // Phải lưu token trước để request /users/me
    // có thể gắn Authorization header.
    saveAuthStorage({
      token: authResponse.token,
      user: temporaryUser,
    });

    try {
      const currentUser = await getCurrentUserRequest();

      saveAuthStorage({
        token: authResponse.token,
        user: currentUser,
      });

      setToken(authResponse.token);
      setUser(currentUser);

      return currentUser;
    } catch (error) {
      clearAuthState();
      throw error;
    }
  }

  async function login(credentials) {
    const authResponse = await loginRequest(credentials);

    await establishSession(authResponse);

    return authResponse;
  }

  async function register(registerData) {
    const authResponse = await registerRequest(registerData);

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