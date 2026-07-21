import { describe, expect, it } from 'vitest';
import { ageState, clock, elapsed, money } from './format';
import { colors } from './tokens';

describe('money', () => {
  it('formats to two decimals with a $ sign', () => {
    expect(money(6.5)).toBe('$6.50');
    expect(money(0)).toBe('$0.00');
    expect(money(16.22)).toBe('$16.22');
  });
});

describe('elapsed', () => {
  it('returns whole seconds since the instant', () => {
    const created = '2026-07-20T19:00:00.000Z';
    const now = Date.parse('2026-07-20T19:01:05.000Z');
    expect(elapsed(created, now)).toBe(65);
  });

  it('never goes negative for a future timestamp', () => {
    const created = '2026-07-20T19:00:00.000Z';
    const now = Date.parse('2026-07-20T18:59:00.000Z');
    expect(elapsed(created, now)).toBe(0);
  });
});

describe('clock', () => {
  it('renders m:ss', () => {
    expect(clock(0)).toBe('0:00');
    expect(clock(65)).toBe('1:05');
    expect(clock(600)).toBe('10:00');
  });
});

describe('ageState', () => {
  it('is fresh below 5 minutes', () => {
    expect(ageState(0).key).toBe('fresh');
    expect(ageState(299).key).toBe('fresh');
    expect(ageState(0).color).toBe(colors.fresh);
  });

  it('is working from 5 to 10 minutes', () => {
    expect(ageState(300).key).toBe('working');
    expect(ageState(599).key).toBe('working');
    expect(ageState(300).color).toBe(colors.working);
  });

  it('is late at 10 minutes and beyond', () => {
    expect(ageState(600).key).toBe('late');
    expect(ageState(3600).key).toBe('late');
    expect(ageState(600).color).toBe(colors.late);
  });
});
