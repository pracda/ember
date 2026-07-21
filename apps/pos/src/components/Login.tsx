// Staff sign-in for the POS (light theme). The backend still enforces the role;
// this screen just collects credentials and surfaces errors.
import { useState, type FormEvent } from 'react';
import { ApiError } from '@ember/shared';

interface Props {
  onSubmit: (username: string, password: string) => Promise<unknown>;
  hint?: string;
}

export function Login({ onSubmit, hint }: Props) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await onSubmit(username.trim(), password);
    } catch (ex) {
      setError(ex instanceof ApiError && ex.status === 401 ? 'Wrong username or password.' : 'Could not sign in.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="grid min-h-screen place-items-center bg-bone text-ink font-body p-6">
      <form onSubmit={submit} className="w-full max-w-sm rounded-3xl bg-white p-8 shadow-lg">
        <h1 className="font-display text-4xl bg-ember-gradient bg-clip-text text-transparent">Ember POS</h1>
        <p className="mt-1 text-muted">Cashier sign-in</p>

        <label className="mt-6 block text-sm font-semibold">
          Username
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoFocus
            autoComplete="username"
            className="mt-1 w-full rounded-lg border border-bone2 px-3 py-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember"
          />
        </label>
        <label className="mt-4 block text-sm font-semibold">
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            className="mt-1 w-full rounded-lg border border-bone2 px-3 py-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember"
          />
        </label>

        {error && <p role="alert" className="mt-3 text-sm text-late">{error}</p>}

        <button
          type="submit"
          disabled={busy || !username || !password}
          className="mt-6 min-h-12 w-full rounded-2xl bg-ember-gradient font-display text-xl text-[#1a0f08] shadow-md disabled:opacity-40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ink"
        >
          {busy ? 'Signing in…' : 'Sign in'}
        </button>

        {hint && <p className="mt-4 text-center text-xs text-muted">{hint}</p>}
      </form>
    </main>
  );
}
