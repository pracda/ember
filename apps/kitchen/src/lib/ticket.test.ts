import { describe, expect, it } from 'vitest';
import type { OrderLine } from '@ember/shared';
import { lineModifiers, nextAction } from './ticket';

describe('nextAction', () => {
  it('starts cooking a new ticket', () => {
    expect(nextAction('NEW')).toEqual({ label: 'Start cooking', kind: 'advance' });
  });

  it('marks a preparing ticket ready', () => {
    expect(nextAction('PREP')).toEqual({ label: 'Mark ready', kind: 'advance' });
  });

  it('has no action once ready or done', () => {
    expect(nextAction('READY')).toBeNull();
    expect(nextAction('DONE')).toBeNull();
  });
});

describe('lineModifiers', () => {
  const base: OrderLine = {
    id: 1,
    menuItemId: 'b1',
    itemName: 'Ember Smash',
    quantity: 1,
    size: null,
    meal: false,
    addons: [],
    notes: null,
    unitPrice: 6.5,
    lineTotal: 6.5,
  };

  it('lists size, meal and add-ons in order', () => {
    expect(lineModifiers({ ...base, size: 'Large', meal: true, addons: ['No onion'] })).toEqual([
      'Large',
      'Meal',
      'No onion',
    ]);
  });

  it('is empty for a plain line', () => {
    expect(lineModifiers(base)).toEqual([]);
  });
});
