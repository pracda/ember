// Manager Schedule tab: the roster (create/edit/delete shifts) plus a
// shift-performance summary (hours worked + sales per staff) for the range.
import { useEffect, useState, type ReactNode } from 'react';
import { api, money, type LaborRow, type Shift, type ShiftInput, type Staff } from '@ember/shared';

const iso = (d: Date) => d.toISOString().slice(0, 10);
const today = () => iso(new Date());
const plusDays = (n: number) => {
  const d = new Date();
  d.setDate(d.getDate() + n);
  return iso(d);
};
const hhmm = (t: string) => t.slice(0, 5);

type Editing = { shift: Shift | null } | null;

export function Schedule() {
  const [from, setFrom] = useState(today());
  const [to, setTo] = useState(plusDays(6));
  const [shifts, setShifts] = useState<Shift[] | null>(null);
  const [labor, setLabor] = useState<LaborRow[]>([]);
  const [staff, setStaff] = useState<Staff[]>([]);
  const [editing, setEditing] = useState<Editing>(null);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setError(null);
    Promise.all([api.getShifts(from, to), api.getLabor(from, to), api.getStaff()])
      .then(([s, l, st]) => {
        setShifts(s);
        setLabor(l);
        setStaff(st.filter((x) => x.active));
      })
      .catch(() => setError('Could not load the schedule.'));
  };
  useEffect(load, [from, to]);

  const save = async (input: ShiftInput) => {
    if (editing?.shift) await api.updateShift(editing.shift.id, input);
    else await api.createShift(input);
    setEditing(null);
    load();
  };
  const remove = async (id: number) => {
    await api.deleteShift(id);
    setEditing(null);
    load();
  };

  return (
    <div className="p-6">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 className="font-display text-2xl">Schedule</h2>
        <div className="flex flex-wrap items-center gap-2">
          <input type="date" value={from} max={to} onChange={(e) => setFrom(e.target.value)} className="input w-auto" />
          <span className="text-muted">→</span>
          <input type="date" value={to} min={from} onChange={(e) => setTo(e.target.value)} className="input w-auto" />
          <button
            onClick={() => setEditing({ shift: null })}
            className="min-h-9 rounded-xl bg-ember-gradient px-4 font-display text-[#1a0f08]"
          >
            + Shift
          </button>
        </div>
      </div>

      {error && <p className="text-late">{error}</p>}

      <div className="grid gap-4 lg:grid-cols-2">
        <Card title="Roster">
          {!shifts ? (
            <p className="text-muted">Loading…</p>
          ) : shifts.length === 0 ? (
            <p className="text-muted">No shifts scheduled for this range.</p>
          ) : (
            <ul className="divide-y divide-steel/60">
              {shifts.map((s) => (
                <li key={s.id} className="flex items-center justify-between gap-2 py-2">
                  <div>
                    <span className="font-semibold">{s.staffName}</span>
                    <span className="ml-2 text-sm text-muted">
                      {s.workDate} · {hhmm(s.startTime)}–{hhmm(s.endTime)}
                    </span>
                    {s.note && <span className="ml-2 text-sm italic text-ember">{s.note}</span>}
                  </div>
                  <button
                    onClick={() => setEditing({ shift: s })}
                    className="rounded-lg px-3 py-1 text-flame hover:bg-steel2"
                  >
                    Edit
                  </button>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card title="Hours & sales (shift performance)">
          {labor.length === 0 ? (
            <p className="text-muted">No hours or sales in this range.</p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="text-xs uppercase tracking-wider text-muted">
                <tr>
                  <th className="py-1">Staff</th>
                  <th className="py-1 text-right">Hours</th>
                  <th className="py-1 text-right">Orders</th>
                  <th className="py-1 text-right">Sales</th>
                  <th className="py-1 text-right">$/hr</th>
                </tr>
              </thead>
              <tbody>
                {labor.map((r) => (
                  <tr key={r.staffId} className="border-t border-steel/40">
                    <td className="py-1.5 font-semibold">{r.displayName}</td>
                    <td className="py-1.5 text-right font-mono">{r.hoursWorked.toFixed(2)}</td>
                    <td className="py-1.5 text-right font-mono">{r.ordersServed}</td>
                    <td className="py-1.5 text-right font-mono">{money(r.sales)}</td>
                    <td className="py-1.5 text-right font-mono">{r.hoursWorked > 0 ? money(r.salesPerHour) : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      </div>

      {editing && (
        <ShiftForm
          shift={editing.shift}
          staff={staff}
          defaultDate={from}
          onSave={save}
          onDelete={editing.shift ? () => remove(editing.shift!.id) : undefined}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  );
}

function ShiftForm({
  shift,
  staff,
  defaultDate,
  onSave,
  onDelete,
  onClose,
}: {
  shift: Shift | null;
  staff: Staff[];
  defaultDate: string;
  onSave: (input: ShiftInput) => Promise<void>;
  onDelete?: () => Promise<void>;
  onClose: () => void;
}) {
  const [staffId, setStaffId] = useState<number>(shift?.staffId ?? staff[0]?.id ?? 0);
  const [workDate, setWorkDate] = useState(shift?.workDate ?? defaultDate);
  const [start, setStart] = useState(shift ? hhmm(shift.startTime) : '09:00');
  const [end, setEnd] = useState(shift ? hhmm(shift.endTime) : '17:00');
  const [note, setNote] = useState(shift?.note ?? '');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    setBusy(true);
    setError(null);
    try {
      if (end <= start) throw new Error('End time must be after start time.');
      await onSave({ staffId, workDate, startTime: start, endTime: end, note: note.trim() || undefined });
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed.');
      setBusy(false);
    }
  };

  return (
    <div className="fixed inset-0 z-40 grid place-items-center bg-black/60 p-4" onClick={onClose} role="presentation">
      <div
        role="dialog"
        aria-modal="true"
        aria-label={shift ? 'Edit shift' : 'New shift'}
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-sm rounded-3xl bg-graphite p-6 text-bone shadow-2xl"
      >
        <h2 className="font-display text-2xl">{shift ? 'Edit shift' : 'New shift'}</h2>

        <label className="mt-4 block text-sm">
          <span className="text-muted">Staff</span>
          <select value={staffId} onChange={(e) => setStaffId(Number(e.target.value))} className="input mt-1">
            {staff.map((s) => (
              <option key={s.id} value={s.id}>{s.displayName}</option>
            ))}
          </select>
        </label>
        <label className="mt-3 block text-sm">
          <span className="text-muted">Date</span>
          <input type="date" value={workDate} onChange={(e) => setWorkDate(e.target.value)} className="input mt-1" />
        </label>
        <div className="mt-3 grid grid-cols-2 gap-3">
          <label className="block text-sm">
            <span className="text-muted">Start</span>
            <input type="time" value={start} onChange={(e) => setStart(e.target.value)} className="input mt-1" />
          </label>
          <label className="block text-sm">
            <span className="text-muted">End</span>
            <input type="time" value={end} onChange={(e) => setEnd(e.target.value)} className="input mt-1" />
          </label>
        </div>
        <label className="mt-3 block text-sm">
          <span className="text-muted">Note</span>
          <input value={note} onChange={(e) => setNote(e.target.value)} placeholder="optional" className="input mt-1" />
        </label>

        {error && <p role="alert" className="mt-3 text-late">{error}</p>}

        <div className="mt-6 flex items-center gap-2">
          {onDelete && (
            <button onClick={() => { setBusy(true); onDelete().catch(() => setBusy(false)); }} disabled={busy}
              className="min-h-11 rounded-xl border border-late/50 px-4 text-late hover:bg-late/10 disabled:opacity-40">
              Delete
            </button>
          )}
          <div className="ml-auto flex gap-2">
            <button onClick={onClose} disabled={busy} className="min-h-11 rounded-xl border border-steel2 px-4 text-bone/80">Cancel</button>
            <button onClick={submit} disabled={busy || !staffId} className="min-h-11 rounded-xl bg-ember-gradient px-5 font-display text-lg text-[#1a0f08] disabled:opacity-40">
              {busy ? 'Saving…' : 'Save'}
            </button>
          </div>
        </div>
      </div>
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
