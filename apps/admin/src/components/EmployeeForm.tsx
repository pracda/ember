// Create a new employee, or edit an existing one (profile + reset PIN/password + delete).
// A manager can't demote, deactivate or delete themselves — those controls are disabled.
import { useState, type ReactNode } from 'react';
import type { Role, Staff, StaffInput, StaffUpdate } from '@ember/shared';

const ROLES: Role[] = ['CASHIER', 'COOK', 'MANAGER'];

interface Props {
  staff: Staff | null; // null → creating a new employee
  isSelf: boolean;
  onCreate: (input: StaffInput) => Promise<void>;
  onUpdate: (update: StaffUpdate) => Promise<void>;
  onResetPin: (pin: string) => Promise<void>;
  onResetPassword: (password: string) => Promise<void>;
  onDelete: () => Promise<void>;
  onClose: () => void;
}

export function EmployeeForm(props: Props) {
  const { staff, isSelf, onClose } = props;
  const isNew = staff === null;

  const [username, setUsername] = useState(staff?.username ?? '');
  const [displayName, setDisplayName] = useState(staff?.displayName ?? '');
  const [role, setRole] = useState<Role>(staff?.role ?? 'CASHIER');
  const [active, setActive] = useState(staff?.active ?? true);
  const [pin, setPin] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const run = async (fn: () => Promise<void>) => {
    setBusy(true);
    setError(null);
    try {
      await fn();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Something went wrong.');
      setBusy(false);
    }
  };

  const validPin = (p: string) => /^\d{4,6}$/.test(p);

  const saveNew = () =>
    run(async () => {
      if (!username.trim() || !displayName.trim()) throw new Error('Username and name are required.');
      if (!pin && !password) throw new Error('Set a PIN, a password, or both.');
      if (pin && !validPin(pin)) throw new Error('PIN must be 4–6 digits.');
      if (password && password.length < 6) throw new Error('Password must be at least 6 characters.');
      await props.onCreate({
        username: username.trim(),
        displayName: displayName.trim(),
        role,
        pin: pin || undefined,
        password: password || undefined,
      });
    });

  const saveProfile = () =>
    run(() => props.onUpdate({ displayName: displayName.trim(), role, active }));

  const resetPin = () =>
    run(async () => {
      if (!validPin(pin)) throw new Error('PIN must be 4–6 digits.');
      await props.onResetPin(pin);
      setPin('');
    });

  const resetPassword = () =>
    run(async () => {
      if (password.length < 6) throw new Error('Password must be at least 6 characters.');
      await props.onResetPassword(password);
      setPassword('');
    });

  return (
    <div className="fixed inset-0 z-40 grid place-items-center bg-black/60 p-4" onClick={onClose} role="presentation">
      <div
        role="dialog"
        aria-modal="true"
        aria-label={isNew ? 'New employee' : `Edit ${staff.displayName}`}
        onClick={(e) => e.stopPropagation()}
        className="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-3xl bg-graphite p-6 text-bone shadow-2xl"
      >
        <h2 className="font-display text-3xl">{isNew ? 'New employee' : staff.displayName}</h2>

        <label className="mt-4 block text-sm">
          <span className="text-muted">Name</span>
          <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} className="input mt-1" />
        </label>

        {isNew && (
          <label className="mt-3 block text-sm">
            <span className="text-muted">Username (for admin login)</span>
            <input value={username} onChange={(e) => setUsername(e.target.value)} className="input mt-1" />
          </label>
        )}

        <label className="mt-3 block text-sm">
          <span className="text-muted">Role</span>
          <select
            value={role}
            disabled={isSelf}
            onChange={(e) => setRole(e.target.value as Role)}
            className="input mt-1 disabled:opacity-50"
          >
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {r.charAt(0) + r.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
          {isSelf && <span className="text-xs text-muted">You can't change your own role.</span>}
        </label>

        {!isNew && (
          <label className="mt-3 flex items-center gap-2">
            <input
              type="checkbox"
              checked={active}
              disabled={isSelf}
              onChange={(e) => setActive(e.target.checked)}
              className="h-4 w-4 accent-ember disabled:opacity-50"
            />
            <span>Active</span>
          </label>
        )}

        {isNew ? (
          <>
            <div className="mt-4 grid grid-cols-2 gap-3">
              <label className="block text-sm">
                <span className="text-muted">PIN (stations)</span>
                <input value={pin} inputMode="numeric" onChange={(e) => setPin(e.target.value)} className="input mt-1" placeholder="4–6 digits" />
              </label>
              <label className="block text-sm">
                <span className="text-muted">Password (admin)</span>
                <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} className="input mt-1" placeholder="optional" />
              </label>
            </div>
            {error && <p role="alert" className="mt-3 text-late">{error}</p>}
            <div className="mt-6 flex justify-end gap-2">
              <button onClick={onClose} disabled={busy} className="min-h-11 rounded-xl border border-steel2 px-4 text-bone/80">Cancel</button>
              <button onClick={saveNew} disabled={busy} className="min-h-11 rounded-xl bg-ember-gradient px-5 font-display text-lg text-[#1a0f08] disabled:opacity-40">
                {busy ? 'Saving…' : 'Create'}
              </button>
            </div>
          </>
        ) : (
          <>
            <Section title="Reset station PIN">
              <div className="flex gap-2">
                <input value={pin} inputMode="numeric" onChange={(e) => setPin(e.target.value)} placeholder="4–6 digits" className="input" />
                <button onClick={resetPin} disabled={busy || !pin} className="min-h-11 shrink-0 rounded-xl bg-steel2 px-4 disabled:opacity-40">Set PIN</button>
              </div>
            </Section>
            <Section title="Reset admin password">
              <div className="flex gap-2">
                <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="min 6 chars" className="input" />
                <button onClick={resetPassword} disabled={busy || !password} className="min-h-11 shrink-0 rounded-xl bg-steel2 px-4 disabled:opacity-40">Set password</button>
              </div>
            </Section>

            {error && <p role="alert" className="mt-3 text-late">{error}</p>}

            <div className="mt-6 flex items-center gap-2">
              <button
                onClick={() => run(props.onDelete)}
                disabled={busy || isSelf}
                title={isSelf ? "You can't delete yourself" : undefined}
                className="min-h-11 rounded-xl border border-late/50 px-4 text-late hover:bg-late/10 disabled:opacity-40"
              >
                Delete
              </button>
              <div className="ml-auto flex gap-2">
                <button onClick={onClose} disabled={busy} className="min-h-11 rounded-xl border border-steel2 px-4 text-bone/80">Close</button>
                <button onClick={saveProfile} disabled={busy} className="min-h-11 rounded-xl bg-ember-gradient px-5 font-display text-lg text-[#1a0f08] disabled:opacity-40">
                  {busy ? 'Saving…' : 'Save'}
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="mt-4">
      <h3 className="mb-1 text-xs font-semibold uppercase tracking-wider text-muted">{title}</h3>
      {children}
    </section>
  );
}
