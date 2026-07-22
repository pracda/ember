import { describe, expect, it } from 'vitest';
import type { MenuItem } from '@ember/shared';
import { blankItem, toInput, validate } from './menuForm';

describe('menuForm', () => {
  it('blankItem is empty and invalid', () => {
    const b = blankItem();
    expect(b.sizes).toEqual([]);
    expect(validate(b)).toMatch(/ID is required/);
  });

  it('validate flags missing name, bad price and blank option labels', () => {
    expect(validate({ ...blankItem(), id: 'x1' })).toMatch(/Name/);
    expect(validate({ ...blankItem(), id: 'x1', name: 'X', category: 'Sides', basePrice: -1 }))
      .toMatch(/Base price/);
    expect(validate({ ...blankItem(), id: 'x1', name: 'X', category: 'Sides', basePrice: 1, sizes: [{ label: '', priceDelta: 0 }] }))
      .toMatch(/label/);
  });

  it('accepts a complete item', () => {
    expect(validate({ ...blankItem(), id: 'x1', name: 'X', category: 'Sides', basePrice: 3 })).toBeNull();
  });

  it('toInput deep-copies modifier lists', () => {
    const item: MenuItem = {
      id: 'b1', name: 'Burger', category: 'Burgers', basePrice: 6.5, mealAvailable: true,
      sizes: [], addons: [{ label: 'Bacon', priceDelta: 1.5 }],
      available: true, tracksStock: false, stock: 0, lowStockThreshold: 0, soldOut: false, lowStock: false,
    };
    const input = toInput(item);
    input.addons[0].label = 'Changed';
    expect(item.addons[0].label).toBe('Bacon');
  });
});
