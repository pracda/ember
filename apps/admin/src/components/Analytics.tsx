// Manager analytics dashboard: KPIs, sales over time, top items, category &
// order-type split, peak hours, and staff sales — over a chosen date range.
import { useEffect, useState, type ReactNode } from 'react';
import { api, money, type Analytics as AnalyticsData } from '@ember/shared';
import { BarRows, HourBars, LineChart } from './charts';

const isoToday = () => new Date().toISOString().slice(0, 10);
const isoDaysAgo = (n: number) => {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toISOString().slice(0, 10);
};

export function Analytics() {
  const [from, setFrom] = useState(isoToday());
  const [to, setTo] = useState(isoToday());
  const [data, setData] = useState<AnalyticsData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    api
      .getAnalytics(from, to)
      .then(setData)
      .catch(() => setError('Could not load analytics.'))
      .finally(() => setLoading(false));
  }, [from, to]);

  const applyPreset = (days: number) => {
    setFrom(days === 0 ? isoToday() : isoDaysAgo(days));
    setTo(isoToday());
  };

  return (
    <div className="p-6">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 className="font-display text-2xl">Analytics</h2>
        <div className="flex flex-wrap items-center gap-2">
          <button onClick={() => applyPreset(0)} className="preset">Today</button>
          <button onClick={() => applyPreset(6)} className="preset">7 days</button>
          <button onClick={() => applyPreset(29)} className="preset">30 days</button>
          <input type="date" value={from} max={to} onChange={(e) => setFrom(e.target.value)} className="input w-auto" />
          <span className="text-muted">→</span>
          <input type="date" value={to} min={from} max={isoToday()} onChange={(e) => setTo(e.target.value)} className="input w-auto" />
        </div>
      </div>

      {error && <p className="text-late">{error}</p>}
      {loading || !data ? (
        <p className="text-muted">Loading…</p>
      ) : (
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
            <Kpi label="Orders" value={String(data.orderCount)} />
            <Kpi label="Revenue" value={money(data.revenue)} />
            <Kpi label="Avg order value" value={money(data.avgOrderValue)} />
          </div>
          {(data.voidedCount > 0 || data.refundedCount > 0) && (
            <p className="text-sm text-muted">
              Excluded from net sales:{' '}
              <span className="text-bone">{data.voidedCount}</span> voided ·{' '}
              <span className="text-bone">{data.refundedCount}</span> refunded (
              {money(data.refundedAmount)})
            </p>
          )}

          <Card title="Revenue over time">
            <LineChart points={data.salesByDay.map((d) => ({ label: d.date.slice(5), value: d.revenue }))} />
          </Card>

          <div className="grid gap-4 lg:grid-cols-2">
            <Card title="Top items">
              <BarRows
                items={data.topItems.map((i) => ({ label: i.itemName, hint: `${i.quantity} sold`, value: i.revenue }))}
                format={money}
              />
            </Card>
            <Card title="By category">
              <BarRows
                items={data.byCategory.map((c) => ({ label: c.category, hint: `${c.quantity} sold`, value: c.revenue }))}
                format={money}
              />
            </Card>
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <Card title="Order type">
              <BarRows
                items={data.byOrderType.map((t) => ({
                  label: t.type === 'TO_GO' ? 'To go' : 'Dine in',
                  hint: `${t.orderCount} order${t.orderCount === 1 ? '' : 's'}`,
                  value: t.revenue,
                }))}
                format={money}
              />
            </Card>
            <Card title="Staff sales">
              <BarRows
                items={data.byStaff.map((s) => ({ label: s.displayName, hint: `${s.orderCount} order${s.orderCount === 1 ? '' : 's'}`, value: s.revenue }))}
                format={money}
              />
            </Card>
          </div>

          <Card title="Peak hours (orders by hour)">
            <HourBars hours={data.byHour} />
          </Card>
        </div>
      )}
    </div>
  );
}

function Kpi({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-steel bg-graphite p-5">
      <p className="text-xs uppercase tracking-wider text-muted">{label}</p>
      <p className="mt-1 font-display text-4xl text-bone">{value}</p>
    </div>
  );
}

function Card({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-2xl border border-steel bg-graphite/40 p-5">
      <h3 className="mb-3 text-xs font-semibold uppercase tracking-wider text-muted">{title}</h3>
      {children}
    </section>
  );
}
