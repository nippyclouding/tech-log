import React, { createContext, useContext, useEffect, useState } from 'react';
import { CurrentUser, fetchCurrentUser } from '../lib/api';
import { BACKEND_ORIGIN } from '../lib/urls';

interface AuthContextType {
  user: CurrentUser | null;
  loading: boolean;
  isAdmin: boolean;
  login: () => void;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchCurrentUser()
      .then(currentUser => setUser(currentUser.authenticated ? currentUser : null))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  const login = () => {
    window.location.href = `${BACKEND_ORIGIN}/oauth2/authorization/github`;
  };

  const logout = async () => {
    await fetch(`${BACKEND_ORIGIN}/admin-console/logout`, {
      method: 'POST',
      credentials: 'include',
    });
    setUser(null);
  };

  const isAdmin = Boolean(user?.admin);

  return (
    <AuthContext.Provider value={{ user, loading, isAdmin, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
