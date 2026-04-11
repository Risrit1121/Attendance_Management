/**
 * AuthContext — session-scoped auth.
 *
 * We store the token in sessionStorage (not localStorage) so that closing
 * the browser tab / window clears the session and the user must log in again.
 * The API base-URL preference (non-sensitive) stays in localStorage.
 */
import { createContext, useContext, useState, useEffect } from "react";
import { login as apiLogin } from "../api/client";

const AuthContext = createContext(null);

const USER_KEY  = "diams_user";
const TOKEN_KEY = "diams_token";

export function AuthProvider({ children }) {
  const [user,    setUser]    = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Restore session only from sessionStorage so closing the browser tab
    // forces a fresh login — localStorage persists across browser restarts.
    const stored = sessionStorage.getItem(USER_KEY);
    const token  = sessionStorage.getItem(TOKEN_KEY);
    if (stored && token) {
      try { setUser(JSON.parse(stored)); } catch {}
    }
    setLoading(false);
  }, []);

  const login = async (email, password) => {
    const res = await apiLogin({ email, password });
    const u   = res.data;
    sessionStorage.setItem(USER_KEY,  JSON.stringify(u));
    if (u.token) sessionStorage.setItem(TOKEN_KEY, u.token);
    setUser(u);
    return u;
  };

  const logout = () => {
    sessionStorage.removeItem(USER_KEY);
    sessionStorage.removeItem(TOKEN_KEY);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, loading }}>
      {!loading && children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);