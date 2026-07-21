/** Money and time formatting, plus the order-age color logic (EMBER-SPEC.md §7.1, §8). */
import { business, colors } from './tokens';

/** Format a money amount (JSON number, scale 2) for display. */
export function money(value: number): string {
  return `$${value.toFixed(2)}`;
}

/** Whole seconds elapsed since an ISO-8601 instant, clamped at 0. */
export function elapsed(createdAt: string, now: number = Date.now()): number {
  const start = new Date(createdAt).getTime();
  return Math.max(0, Math.floor((now - start) / 1000));
}

/** Render a duration in seconds as a `m:ss` kitchen clock. */
export function clock(totalSeconds: number): string {
  const mins = Math.floor(totalSeconds / 60);
  const secs = totalSeconds % 60;
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}

export type AgeKey = 'fresh' | 'working' | 'late';

export interface AgeState {
  key: AgeKey;
  color: string;
  label: string;
}

/**
 * Map an order's age (seconds) to its rail color: fresh <5min (green),
 * working 5–10min (amber), late ≥10min (red). Thresholds live in tokens.
 */
export function ageState(seconds: number): AgeState {
  if (seconds < business.age.freshUnder) {
    return { key: 'fresh', color: colors.fresh, label: 'Fresh' };
  }
  if (seconds < business.age.workingUnder) {
    return { key: 'working', color: colors.working, label: 'Working' };
  }
  return { key: 'late', color: colors.late, label: 'Late' };
}
