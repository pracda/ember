/**
 * Station sign-in: pick your name from the roster, then punch your PIN. Used by
 * the POS (light) and Kitchen (dark) — pass `dark` to theme it. The roster is
 * filtered to the roles that station accepts.
 */
import { useEffect, useState } from 'react';
import { ApiError, api } from '../api';
import type { RosterEntry, Role } from '../types';

interface Props {
  title: string;
  roles: Role[];
  dark?: boolean;
  onSubmit: (staffId: number, pin: string) => Promise<unknown>;
}

const MAX_PIN = 6;

export function PinLogin({ title, roles, dark, onSubmit }: Props) {
  const [roster, setRoster] = useState<RosterEntry[] | null>(null);
  const [selected, setSelected] = useState<RosterEntry | null>(null);
  const [pin, setPin] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api
      .getRoster()
      .then((all) => setRoster(all.filter((r) => roles.includes(r.role))))
      .catch(() => setError('Could not load staff.'));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const submit = async (value: string) => {
    if (!selected || value.length < 4) return;
    setBusy(true);
    setError(null);
    try {
      await onSubmit(selected.id, value);
    } catch (e) {
      setError(e instanceof ApiError && e.status === 401 ? 'Wrong PIN.' : 'Could not sign in.');
      setPin('');
    } finally {
      setBusy(false);
    }
  };

  const press = (digit: string) => {
    setError(null);
    setPin((prev) => (prev.length >= MAX_PIN ? prev : prev + digit));
  };

  const bg = dark ? 'bg-char text-bone' : 'bg-bone text-ink';
  const surface = dark ? 'bg-graphite' : 'bg-white shadow-lg';
  const staffCard = dark
    ? 'bg-graphite hover:bg-graphite2 border border-steel'
    : 'bg-white border border-bone2 hover:border-ember shadow-sm';
  const keyCls = dark ? 'bg-steel2 text-bone hover:bg-steel' : 'bg-bone2 text-ink hover:bg-ink/10';

  return (
    <main className={`grid min-h-screen place-items-center font-body p-6 ${bg}`}>
      <div className={`w-full max-w-md rounded-3xl p-6 ${surface}`}>
        <h1 className="font-display text-4xl bg-ember-gradient bg-clip-text text-transparent">{title}</h1>

        {roster === null ? (
          <p className="mt-6 text-muted">Loading staff…</p>
        ) : selected === null ? (
          <>
            <p className="mt-1 text-muted">Tap your name to sign in</p>
            <div className="mt-4 grid grid-cols-2 gap-3">
              {roster.map((r) => (
                <button
                  key={r.id}
                  onClick={() => {
                    setSelected(r);
                    setPin('');
                    setError(null);
                  }}
                  className={`min-h-16 rounded-2xl px-4 text-left transition ${staffCard} focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember`}
                >
                  <span className="block font-semibold">{r.displayName}</span>
                  <span className="text-xs uppercase tracking-wider text-muted">{r.role.toLowerCase()}</span>
                </button>
              ))}
              {roster.length === 0 && <p className="col-span-2 text-muted">No staff for this station.</p>}
            </div>
            {error && <p role="alert" className="mt-3 text-sm text-late">{error}</p>}
          </>
        ) : (
          <>
            <div className="mt-2 flex items-center justify-between">
              <span className="font-semibold">{selected.displayName}</span>
              <button onClick={() => setSelected(null)} className="text-sm text-muted hover:underline">
                ← Not you?
              </button>
            </div>

            <div className="my-5 flex justify-center gap-3" aria-label="PIN entry">
              {Array.from({ length: MAX_PIN }).map((_, i) => (
                <span
                  key={i}
                  className={`h-3 w-3 rounded-full ${i < pin.length ? 'bg-ember' : dark ? 'bg-steel' : 'bg-bone2'}`}
                />
              ))}
            </div>

            <div className="grid grid-cols-3 gap-3">
              {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((d) => (
                <PinKey key={d} className={keyCls} onClick={() => press(d)}>
                  {d}
                </PinKey>
              ))}
              <PinKey className={keyCls} onClick={() => setPin('')}>
                ✕
              </PinKey>
              <PinKey className={keyCls} onClick={() => press('0')}>
                0
              </PinKey>
              <PinKey className={keyCls} onClick={() => setPin((p) => p.slice(0, -1))}>
                ⌫
              </PinKey>
            </div>

            <button
              onClick={() => submit(pin)}
              disabled={busy || pin.length < 4}
              className="mt-4 min-h-14 w-full rounded-2xl bg-ember-gradient font-display text-xl text-[#1a0f08] shadow-md disabled:opacity-40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ink"
            >
              {busy ? 'Signing in…' : 'Sign in'}
            </button>

            {error && <p role="alert" className="mt-3 text-center text-sm text-late">{error}</p>}
          </>
        )}
      </div>
    </main>
  );
}

function PinKey({
  onClick,
  className,
  children,
}: {
  onClick: () => void;
  className: string;
  children: string;
}) {
  return (
    <button
      onClick={onClick}
      className={`min-h-16 rounded-2xl font-display text-2xl transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember ${className}`}
    >
      {children}
    </button>
  );
}
