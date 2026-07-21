// Manager day-summary: counts, revenue and average prep time for a chosen date.
import { useEffect, useState } from 'react';
import { api, clock, money, type DaySummary as Summary } from '@ember/shared';

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

export function DaySummary() {
  const [date, setDate] = useState(todayIso());
  const [summary, setSummary] = useState<Summary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    setError(null);
    api
      .getDaySummary(date)
      .then(setSummary)
      .catch(() => setError('Could not load the summary.'))
      .finally(() => setLoading(false));
  }, [date]);

  return (
    <div className="p-6">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="font-display text-2xl">Day summary</h2>
        <input
          type="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          className="input w-auto"
        />
      </div>

      {error && <p className="text-late">{error}</p>}
      {loading || !summary ? (
        <p className="text-muted">Loading…</p>
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <Stat label="Orders" value={String(summary.orderCount)} />
          <Stat label="Collected" value={String(summary.collectedCount)} />
          <Stat label="Revenue" value={money(summary.revenue)} />
          <Stat
            label="Avg prep"
            value={summary.avgPrepSeconds != null ? clock(summary.avgPrepSeconds) : '—'}
          />
        </div>
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-steel bg-graphite p-5">
      <p className="text-xs uppercase tracking-wider text-muted">{label}</p>
      <p className="mt-1 font-display text-4xl text-bone">{value}</p>
    </div>
  );
}
