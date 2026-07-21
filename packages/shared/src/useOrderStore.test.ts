import { beforeEach, describe, expect, it } from 'vitest';
import {
  selectActive,
  selectPreparing,
  selectReady,
  useOrderStore,
} from './useOrderStore';
import type { Order, OrderStatus } from './types';

function order(id: number, status: OrderStatus, createdAt: string, readyAt: string | null = null): Order {
  return {
    id,
    ticketNumber: id,
    type: 'DINE_IN',
    status,
    lines: [],
    subtotal: 0,
    tax: 0,
    total: 0,
    createdAt,
    startedAt: null,
    readyAt,
    collectedAt: null,
    reason: null,
  };
}

const state = () => useOrderStore.getState();

beforeEach(() => {
  state().reset();
});

describe('order store', () => {
  it('seeds a snapshot keyed by id', () => {
    state().seed([order(1, 'NEW', '2026-07-20T19:00:00Z'), order(2, 'PREP', '2026-07-20T19:01:00Z')]);
    expect(Object.keys(state().orders)).toEqual(['1', '2']);
  });

  it('upserts by id (replace, not duplicate)', () => {
    state().seed([order(1, 'NEW', '2026-07-20T19:00:00Z')]);
    state().upsert(order(1, 'PREP', '2026-07-20T19:00:00Z'));
    expect(Object.keys(state().orders)).toHaveLength(1);
    expect(state().orders[1].status).toBe('PREP');
  });
});

describe('selectors', () => {
  beforeEach(() => {
    state().seed([
      order(3, 'PREP', '2026-07-20T19:02:00Z'),
      order(1, 'NEW', '2026-07-20T19:00:00Z'),
      order(2, 'PREP', '2026-07-20T19:01:00Z'),
      order(4, 'READY', '2026-07-20T18:55:00Z', '2026-07-20T19:05:00Z'),
      order(5, 'READY', '2026-07-20T18:50:00Z', '2026-07-20T19:06:00Z'),
      order(6, 'DONE', '2026-07-20T18:40:00Z'),
    ]);
  });

  it('active = NEW + PREP, oldest first', () => {
    expect(selectActive(state()).map((o) => o.id)).toEqual([1, 2, 3]);
  });

  it('preparing = PREP only, oldest first', () => {
    expect(selectPreparing(state()).map((o) => o.id)).toEqual([2, 3]);
  });

  it('ready = READY only, newest readyAt first', () => {
    expect(selectReady(state()).map((o) => o.id)).toEqual([5, 4]);
  });

  it('excludes DONE from every active view', () => {
    const activeIds = selectActive(state()).map((o) => o.id);
    expect(activeIds).not.toContain(6);
  });
});
