'use client';

import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { api, AuthResponse, User } from './api';

type AuthContextValue = {
  user: User | null;
  token: string | null;
  loading: boolean;
  login: (emailOrUsername: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  refreshMe: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);
const TOKEN_KEY = 'threadly_token';

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = typeof window !== 'undefined' ? localStorage.getItem(TOKEN_KEY) : null;
    if (!stored) {
      setLoading(false);
      return;
    }
    setToken(stored);
    api<User>('/api/v1/auth/session', {}, stored)
      .then(setUser)
      .catch(() => {
        localStorage.removeItem(TOKEN_KEY);
        setToken(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const applyAuth = (res: AuthResponse) => {
    localStorage.setItem(TOKEN_KEY, res.accessToken);
    setToken(res.accessToken);
    setUser(res.user);
  };

  const value = useMemo<AuthContextValue>(() => ({
    user,
    token,
    loading,
    async login(emailOrUsername, password) {
      const res = await api<AuthResponse>('/api/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify({ emailOrUsername, password }),
      });
      applyAuth(res);
    },
    async register(username, email, password) {
      const res = await api<AuthResponse>('/api/v1/auth/signup', {
        method: 'POST',
        body: JSON.stringify({ username, email, password, displayName: username }),
      });
      applyAuth(res);
    },
    logout() {
      localStorage.removeItem(TOKEN_KEY);
      setToken(null);
      setUser(null);
    },
    async refreshMe() {
      if (!token) return;
      const me = await api<User>('/api/v1/auth/session', {}, token);
      setUser(me);
    },
  }), [user, token, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
