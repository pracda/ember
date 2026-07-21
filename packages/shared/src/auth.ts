/**
 * Staff auth session state. The JWT is stored in localStorage so a login survives
 * a reload (this is auth state, not order state — golden rule #2 is about orders).
 * A tiny pub/sub lets `useAuth` re-render on login/logout.
 */
import type { AuthSession } from './types';

const STORAGE_KEY = 'ember.auth';

function load(): AuthSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as AuthSession) : null;
  } catch {
    return null;
  }
}

let current: AuthSession | null = load();
const listeners = new Set<(session: AuthSession | null) => void>();

export function getSession(): AuthSession | null {
  return current;
}

export function getToken(): string | null {
  return current?.token ?? null;
}

export function setSession(session: AuthSession | null): void {
  current = session;
  try {
    if (session) localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    else localStorage.removeItem(STORAGE_KEY);
  } catch {
    /* storage unavailable — keep the in-memory session */
  }
  listeners.forEach((l) => l(current));
}

export function logout(): void {
  setSession(null);
}

export function subscribeAuth(listener: (session: AuthSession | null) => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}
