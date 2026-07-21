/**
 * Typed REST client. The server owns pricing/state; this client only sends intent
 * (item ids + choices, "advance", "collect") and reads back the server's truth.
 * Base URL comes from VITE_API_BASE (defaults to the dev backend on :8080).
 *
 * Requests attach the staff JWT (when signed in) as a Bearer token; a 401 on a
 * protected call clears the stored session so the app falls back to the login screen.
 */
import { getToken, setSession } from './auth';
import type {
  AuthSession,
  CreateOrderRequest,
  DaySummary,
  MenuItem,
  MenuItemInput,
  Order,
} from './types';

export const apiBase: string = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';

/** RFC 7807 problem body the backend returns on errors. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
}

/** Thrown for any non-2xx response, carrying the parsed ProblemDetail. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly problem: ProblemDetail,
  ) {
    super(problem.detail ?? problem.title ?? `HTTP ${status}`);
    this.name = 'ApiError';
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(init?.headers as Record<string, string> | undefined),
  };
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${apiBase}${path}`, { ...init, headers });

  if (!res.ok) {
    // An expired/invalid token on a protected call — drop the session.
    if (res.status === 401 && token) setSession(null);
    let problem: ProblemDetail = {};
    try {
      problem = (await res.json()) as ProblemDetail;
    } catch {
      /* non-JSON error body */
    }
    throw new ApiError(res.status, problem);
  }

  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}

export const api = {
  // auth
  login: async (username: string, password: string): Promise<AuthSession> => {
    const session = await request<AuthSession>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    setSession(session);
    return session;
  },

  // menu + orders
  getMenu: () => request<MenuItem[]>('/api/menu'),
  getActiveOrders: () => request<Order[]>('/api/orders?status=active'),
  getReadyOrders: () => request<Order[]>('/api/orders?status=ready'),
  createOrder: (req: CreateOrderRequest) =>
    request<Order>('/api/orders', { method: 'POST', body: JSON.stringify(req) }),
  advance: (id: number) => request<Order>(`/api/orders/${id}/advance`, { method: 'POST' }),
  recall: (id: number) => request<Order>(`/api/orders/${id}/recall`, { method: 'POST' }),
  collect: (id: number) => request<Order>(`/api/orders/${id}/collect`, { method: 'POST' }),

  // menu admin (MANAGER)
  createMenuItem: (item: MenuItemInput) =>
    request<MenuItem>('/api/menu', { method: 'POST', body: JSON.stringify(item) }),
  updateMenuItem: (id: string, item: MenuItemInput) =>
    request<MenuItem>(`/api/menu/${id}`, { method: 'PUT', body: JSON.stringify(item) }),
  deleteMenuItem: (id: string) =>
    request<void>(`/api/menu/${id}`, { method: 'DELETE' }),

  // reporting (MANAGER)
  getDaySummary: (date?: string) =>
    request<DaySummary>(`/api/reports/day-summary${date ? `?date=${date}` : ''}`),
};

export type Api = typeof api;
