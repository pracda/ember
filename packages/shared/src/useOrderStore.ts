/**
 * The single source of client-side order state — one store keyed by order id.
 * Every screen upserts events into this map and derives its view by filtering on
 * status (EMBER-SPEC.md golden rule #2: no parallel per-screen lists).
 */
import { create } from 'zustand';
import { useShallow } from 'zustand/react/shallow';
import type { Order } from './types';

interface OrderStoreState {
  orders: Record<number, Order>;
  /** Replace the whole map from a REST snapshot (mount / reconnect heal). */
  seed: (orders: Order[]) => void;
  /** Insert or replace one order by id (from a stream event). */
  upsert: (order: Order) => void;
  reset: () => void;
}

export const useOrderStore = create<OrderStoreState>((set) => ({
  orders: {},
  seed: (orders) =>
    set(() => ({ orders: Object.fromEntries(orders.map((o) => [o.id, o])) })),
  upsert: (order) => set((state) => ({ orders: { ...state.orders, [order.id]: order } })),
  reset: () => set({ orders: {} }),
}));

/* ----- selectors (pure; also usable in tests) ----- */

const byCreatedAsc = (a: Order, b: Order) => a.createdAt.localeCompare(b.createdAt);
const byReadyDesc = (a: Order, b: Order) => (b.readyAt ?? '').localeCompare(a.readyAt ?? '');

/** Active rail: NEW + PREP, oldest first. */
export const selectActive = (state: OrderStoreState): Order[] =>
  Object.values(state.orders)
    .filter((o) => o.status === 'NEW' || o.status === 'PREP')
    .sort(byCreatedAsc);

/** Just the PREP orders (board "Preparing" zone), oldest first. */
export const selectPreparing = (state: OrderStoreState): Order[] =>
  Object.values(state.orders)
    .filter((o) => o.status === 'PREP')
    .sort(byCreatedAsc);

/** READY orders (board "Ready" zone), newest call first. */
export const selectReady = (state: OrderStoreState): Order[] =>
  Object.values(state.orders)
    .filter((o) => o.status === 'READY')
    .sort(byReadyDesc);

/* ----- hook wrappers (useShallow keeps array identity stable across renders) ----- */

export const useActiveOrders = (): Order[] => useOrderStore(useShallow(selectActive));
export const usePreparing = (): Order[] => useOrderStore(useShallow(selectPreparing));
export const useReady = (): Order[] => useOrderStore(useShallow(selectReady));
