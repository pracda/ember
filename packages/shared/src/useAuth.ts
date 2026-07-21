/** React binding over the auth session: re-renders on login/logout. */
import { useSyncExternalStore } from 'react';
import { api } from './api';
import { getSession, logout, subscribeAuth } from './auth';
import type { AuthSession } from './types';

export interface UseAuth {
  session: AuthSession | null;
  login: (username: string, password: string) => Promise<AuthSession>;
  logout: () => void;
}

export function useAuth(): UseAuth {
  const session = useSyncExternalStore(subscribeAuth, getSession, getSession);
  return {
    session,
    login: (username, password) => api.login(username, password),
    logout,
  };
}
