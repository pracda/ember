// Lightweight, dependency-free SVG/CSS charts for the analytics dashboard.
import { money } from '@ember/shared';

export interface Bar {
  label: string;
  value: number;
  hint?: string;
}

/** Horizontal bars (top items, categories, staff, order types). */
export function BarRows({ items, format = String }: { items: Bar[]; format?: (v: number) => string }) {
  if (items.length === 0) return <p className="text-sm text-muted">No data for this range.</p>;
  const max = Math.max(1, ...items.map((i) => i.value));
  return (
    <div className="space-y-2.5">
      {items.map((it, i) => (
        <div key={i} className="text-sm">
          <div className="flex justify-between gap-2">
            <span className="truncate">
              {it.label}
              {it.hint && <span className="text-muted"> · {it.hint}</span>}
            </span>
            <span className="font-mono shrink-0">{format(it.value)}</span>
          </div>
          <div className="mt-1 h-2 rounded bg-steel">
            <div className="h-2 rounded bg-ember-gradient" style={{ width: `${(it.value / max) * 100}%` }} />
          </div>
        </div>
      ))}
    </div>
  );
}

/** Revenue-over-time line chart. */
export function LineChart({ points }: { points: { label: string; value: number }[] }) {
  const w = 640;
  const h = 180;
  const pad = 28;
  const n = points.length;
  const max = Math.max(1, ...points.map((p) => p.value));
  const x = (i: number) => (n <= 1 ? w / 2 : pad + (i * (w - 2 * pad)) / (n - 1));
  const y = (v: number) => h - pad - (v / max) * (h - 2 * pad);
  const line = points.map((p, i) => `${x(i)},${y(p.value)}`).join(' ');
  const area = n > 0 ? `${x(0)},${h - pad} ${line} ${x(n - 1)},${h - pad}` : '';

  return (
    <svg viewBox={`0 0 ${w} ${h}`} className="w-full" role="img" aria-label="Revenue over time">
      <line x1={pad} y1={h - pad} x2={w - pad} y2={h - pad} stroke="#3A322D" />
      {n > 0 && <polygon points={area} fill="rgba(255,87,34,0.15)" />}
      {n > 1 && <polyline points={line} fill="none" stroke="#FF5722" strokeWidth="2" />}
      {points.map((p, i) => (
        <circle key={i} cx={x(i)} cy={y(p.value)} r="2.5" fill="#FF8A3D" />
      ))}
      <text x={pad} y={16} fill="#A99A8C" fontSize="11" fontFamily="monospace">{money(max)}</text>
      <text x={pad} y={h - 8} fill="#A99A8C" fontSize="10">{points[0]?.label ?? ''}</text>
      {n > 1 && (
        <text x={w - pad} y={h - 8} fill="#A99A8C" fontSize="10" textAnchor="end">
          {points[n - 1]?.label}
        </text>
      )}
    </svg>
  );
}

/** 24-hour order-volume bars (peak hours). */
export function HourBars({ hours }: { hours: { hour: number; orderCount: number }[] }) {
  const max = Math.max(1, ...hours.map((h) => h.orderCount));
  return (
    <div>
      <div className="flex h-32 items-end gap-[3px]">
        {hours.map((h) => (
          <div
            key={h.hour}
            className="flex-1 rounded-t bg-ember-gradient"
            style={{ height: `${Math.max(2, (h.orderCount / max) * 100)}%`, opacity: h.orderCount ? 1 : 0.25 }}
            title={`${String(h.hour).padStart(2, '0')}:00 — ${h.orderCount} order${h.orderCount === 1 ? '' : 's'}`}
          />
        ))}
      </div>
      <div className="mt-1 flex justify-between font-mono text-[10px] text-muted">
        <span>00:00</span>
        <span>06:00</span>
        <span>12:00</span>
        <span>18:00</span>
        <span>23:00</span>
      </div>
    </div>
  );
}
