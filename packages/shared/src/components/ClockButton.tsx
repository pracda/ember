/**
 * Time-clock control for the station header. Reflects the signed-in staff
 * member's current punch and toggles clock in / out. Themeable via `dark`.
 */
import { useEffect, useState } from 'react';
import { api } from '../api';
import type { TimeEntry } from '../types';

export function ClockButton({ dark }: { dark?: boolean }) {
  const [entry, setEntry] = useState<TimeEntry | null | undefined>(undefined);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.getMyTimeclock().then(setEntry).catch(() => setEntry(null));
  }, []);

  const toggle = async () => {
    setBusy(true);
    try {
      if (entry) {
        await api.clockOut();
        setEntry(null);
      } else {
        setEntry(await api.clockIn());
      }
    } catch {
      /* leave state as-is */
    } finally {
      setBusy(false);
    }
  };

  if (entry === undefined) return null;
  const clockedIn = entry !== null;

  const base =
    'min-h-9 rounded-full px-3 text-sm font-semibold focus-visible:outline-none focus-visible:ring-2 disabled:opacity-50';
  const theme = clockedIn
    ? dark
      ? 'bg-fresh/15 text-fresh ring-fresh/40 focus-visible:ring-fresh'
      : 'bg-fresh/15 text-fresh focus-visible:ring-fresh'
    : dark
      ? 'border border-steel2 text-bone/80 hover:bg-steel2 focus-visible:ring-ember'
      : 'border border-bone2 text-ink/70 hover:bg-ink/10 focus-visible:ring-ember';

  return (
    <button onClick={toggle} disabled={busy} className={`${base} ${theme}`}>
      {clockedIn ? '● Clock out' : 'Clock in'}
    </button>
  );
}
