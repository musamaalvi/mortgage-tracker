import { useCallback, useEffect, useState } from 'react';
import { api, setUnauthorizedHandler } from './api';
import type { Mortgage } from './types';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Entries from './pages/Entries';
import Settings from './pages/Settings';

type Page = 'dashboard' | 'entries' | 'settings';
type AuthState = 'checking' | 'out' | 'in';

const PAGES: { id: Page; label: string }[] = [
  { id: 'dashboard', label: 'Dashboard' },
  { id: 'entries', label: 'Payments' },
  { id: 'settings', label: 'Mortgage & rates' },
];

export default function App() {
  const [auth, setAuth] = useState<AuthState>('checking');
  const [user, setUser] = useState('');
  const [page, setPage] = useState<Page>('dashboard');
  const [mortgage, setMortgage] = useState<Mortgage | null | undefined>(undefined);

  useEffect(() => {
    setUnauthorizedHandler(() => setAuth('out'));
    api.me()
      .then((u) => {
        setUser(u!.username);
        setAuth('in');
      })
      .catch(() => setAuth('out'));
  }, []);

  const loadMortgage = useCallback(async () => {
    const m = await api.mortgage();
    setMortgage(m);
    if (!m) setPage('settings');
  }, []);

  useEffect(() => {
    if (auth === 'in') loadMortgage().catch(() => setMortgage(null));
  }, [auth, loadMortgage]);

  if (auth === 'checking') return <div className="center muted">Loading…</div>;
  if (auth === 'out') {
    return (
      <Login
        onLogin={(name) => {
          setUser(name);
          setAuth('in');
        }}
      />
    );
  }

  const logout = async () => {
    await api.logout().catch(() => {});
    setMortgage(undefined);
    setAuth('out');
  };

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">Mortgage Tracker</div>
        <nav className="tabs" aria-label="Sections">
          {PAGES.map((p) => (
            <button
              key={p.id}
              className={`tab ${page === p.id ? 'active' : ''}`}
              onClick={() => setPage(p.id)}
              disabled={!mortgage && p.id !== 'settings'}
              aria-current={page === p.id ? 'page' : undefined}
            >
              {p.label}
            </button>
          ))}
        </nav>
        <div className="user">
          <span className="muted">{user}</span>
          <button className="btn ghost small" onClick={logout}>
            Log out
          </button>
        </div>
      </header>

      <main className="content">
        {mortgage === undefined ? (
          <div className="muted">Loading…</div>
        ) : page === 'dashboard' && mortgage ? (
          <Dashboard />
        ) : page === 'entries' && mortgage ? (
          <Entries />
        ) : (
          <Settings mortgage={mortgage} onSaved={loadMortgage} />
        )}
      </main>
    </div>
  );
}
