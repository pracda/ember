import { describe, expect, it } from 'vitest';
import type { MenuItem } from '@ember/shared';
import {
  addLine,
  cartTotals,
  lineKey,
  previewUnitPrice,
  setQuantity,
  toCreateRequest,
  type CartLine,
  type LineChoices,
} from './cart';

const burger: MenuItem = {
  id: 'b1',
  name: 'Ember Smash',
  category: 'Burgers',
  basePrice: 6.5,
  mealAvailable: true,
  sizes: [],
  addons: [
    { label: 'Extra cheese', priceDelta: 0.9 },
    { label: 'Bacon', priceDelta: 1.5 },
    { label: 'No onion', priceDelta: 0 },
  ],
};

const soda: MenuItem = {
  id: 'd1',
  name: 'Fountain Soda',
  category: 'Drinks',
  basePrice: 2.25,
  mealAvailable: false,
  sizes: [
    { label: 'Small', priceDelta: 0 },
    { label: 'Large', priceDelta: 1.8 },
  ],
  addons: [],
};

const choices = (over: Partial<LineChoices> = {}): LineChoices => ({
  meal: false,
  addons: [],
  ...over,
});

describe('previewUnitPrice', () => {
  it('sums base + size + add-ons + meal', () => {
    expect(previewUnitPrice(burger, choices({ meal: true, addons: ['No onion', 'Extra cheese'] }))).toBe(10.9);
    expect(previewUnitPrice(soda, choices({ size: 'Large' }))).toBe(4.05);
  });

  it('ignores meal on a non-meal item', () => {
    expect(previewUnitPrice(soda, choices({ meal: true }))).toBe(2.25);
  });
});

describe('lineKey / merge', () => {
  it('treats add-on order as identical', () => {
    expect(lineKey('b1', choices({ addons: ['a', 'b'] }))).toBe(lineKey('b1', choices({ addons: ['b', 'a'] })));
  });

  it('merges an identical configuration by incrementing quantity', () => {
    let lines: CartLine[] = [];
    lines = addLine(lines, burger, choices({ meal: true, addons: ['Extra cheese'] }));
    lines = addLine(lines, burger, choices({ meal: true, addons: ['Extra cheese'] }));
    expect(lines).toHaveLength(1);
    expect(lines[0].quantity).toBe(2);
  });

  it('keeps different configurations as separate lines', () => {
    let lines: CartLine[] = [];
    lines = addLine(lines, burger, choices({ addons: ['Bacon'] }));
    lines = addLine(lines, burger, choices({ addons: ['Extra cheese'] }));
    expect(lines).toHaveLength(2);
  });
});

describe('setQuantity', () => {
  it('removes the line at zero', () => {
    let lines = addLine([], burger, choices());
    lines = setQuantity(lines, lines[0].key, 0);
    expect(lines).toHaveLength(0);
  });
});

describe('cartTotals', () => {
  it('computes subtotal, 8.5% tax and total', () => {
    let lines = addLine([], burger, choices({ meal: true, addons: ['No onion', 'Extra cheese'] })); // 10.90
    lines = addLine(lines, soda, choices({ size: 'Large' })); // 4.05
    expect(cartTotals(lines)).toEqual({ subtotal: 14.95, tax: 1.27, total: 16.22 });
  });

  it('is zero for an empty cart', () => {
    expect(cartTotals([])).toEqual({ subtotal: 0, tax: 0, total: 0 });
  });
});

describe('toCreateRequest', () => {
  it('sends item ids + choices, never prices', () => {
    const lines = addLine([], burger, choices({ meal: true, addons: ['No onion'], notes: 'well done' }));
    const req = toCreateRequest(lines, 'DINE_IN');
    expect(req).toEqual({
      type: 'DINE_IN',
      lines: [{ itemId: 'b1', quantity: 1, size: undefined, meal: true, addons: ['No onion'], notes: 'well done' }],
    });
    expect(JSON.stringify(req)).not.toContain('unitPrice');
  });
});
