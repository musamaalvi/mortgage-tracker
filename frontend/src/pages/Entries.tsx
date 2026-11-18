import { ChangeEvent, FormEvent, useEffect, useRef, useState } from 'react';
import { api } from '../api';
import { money, pennies, todayIso, ukDate } from '../format';
import type { Entry, EntryType, ImportResult } from '../types';

export default function Entries() {
  const [entries, setEntries] = useState<Entry[]>([]);
  const [editing, setEditing] = useState<Entry | null>(null);
  const [error, setError] = useState('');

  const load = () => api.entries().then(setEntries).catch((e) => setError(e.message));
  useEffect(() => {
    load();
  }, []);

  const remove = async (e: Entry) => {
    if (!confirm(`Delete the ${money(e.totalPayment)} payment on ${ukDate(e.date)}?`)) return;
    try {
      await api.deleteEntry(e.id);
      if (editing?.id === e.id) setEditing(null);
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const newestFirst = [...entries].reverse();

  return (
    <div className="stack">
      <div className="two-col">
        <EntryForm
          key={editing?.id ?? 'new'}
          editing={editing}
          onDone={async () => {
            setEditing(null);
            await load();
          }}
        />
        <ImportCard onImported={load} />
      </div>

      <section className="card">
        <h2>Payments ({entries.length})</h2>
        {error && <p className="error">{error}</p>}
        {entries.length === 0 ? (
          <p className="muted">No payments yet. Add one above or import a spreadsheet.</p>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Type</th>
                  <th className="num">Total</th>
                  <th className="num">Rent (interest)</th>
                  <th className="num">Equity</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {newestFirst.map((e) => (
                  <tr key={e.id} className={editing?.id === e.id ? 'selected' : undefined}>
                    <td>{ukDate(e.date)}</td>
                    <td>
                      <span className={`pill ${e.type === 'OVERPAYMENT' ? 'pill-over' : ''}`}>
                        {e.type === 'OVERPAYMENT' ? 'Overpayment' : 'Regular'}
                      </span>
                    </td>
                    <td className="num">
                      {e.mismatch && (
                        <span className="warn-icon" title="Total does not equal rent + equity" aria-label="Mismatch">
                          ⚠
                        </span>
                      )}
                      {money(e.totalPayment)}
                    </td>
                    <td className="num">{money(e.rent)}</td>
                    <td className="num">{money(e.equity)}</td>
                    <td className="actions">
                      <button className="btn ghost small" onClick={() => setEditing(e)}>
                        Edit
                      </button>
                      <button className="btn ghost small danger" onClick={() => remove(e)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function EntryForm({ editing, onDone }: { editing: Entry | null; onDone: () => Promise<void> }) {
  const [date, setDate] = useState(editing?.date ?? todayIso());
  const [total, setTotal] = useState(editing ? String(editing.totalPayment) : '');
  const [rent, setRent] = useState(editing ? String(editing.rent) : '');
  const [equity, setEquity] = useState(editing ? String(editing.equity) : '');
  // Equity follows total − rent until the user types their own value.
  const [equityEdited, setEquityEdited] = useState(!!editing);
  const [type, setType] = useState<EntryType>(editing?.type ?? 'REGULAR');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!equityEdited && total !== '') {
      setEquity(String(pennies(Number(total) - Number(rent || 0))));
    }
  }, [total, rent, equityEdited]);

  const mismatch =
    total !== '' && equity !== '' && Math.abs(Number(total) - Number(rent || 0) - Number(equity)) > 0.01;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    const body = { date, totalPayment: Number(total), rent: Number(rent || 0), equity: Number(equity), type };
    try {
      if (editing) await api.updateEntry(editing.id, body);
      else await api.createEntry(body);
      if (!editing) {
        setTotal('');
        setRent('');
        setEquity('');
        setEquityEdited(false);
      }
      await onDone();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="card" onSubmit={submit}>
      <h2>{editing ? `Edit payment on ${ukDate(editing.date)}` : 'Add a payment'}</h2>
      <div className="grid-form">
        <label>
          Date
          <input type="date" value={date} onChange={(e) => setDate(e.target.value)} required />
        </label>
        <label>
          Type
          <select value={type} onChange={(e) => setType(e.target.value as EntryType)}>
            <option value="REGULAR">Regular</option>
            <option value="OVERPAYMENT">Overpayment</option>
          </select>
        </label>
        <label>
          Total payment (£)
          <input type="number" min="0" step="0.01" value={total} onChange={(e) => setTotal(e.target.value)} required />
        </label>
        <label>
          Rent / interest (£)
          <input type="number" min="0" step="0.01" value={rent} onChange={(e) => setRent(e.target.value)} />
        </label>
        <label>
          Equity (£)
          <input
            type="number"
            min="0"
            step="0.01"
            value={equity}
            onChange={(e) => {
              setEquity(e.target.value);
              setEquityEdited(true);
            }}
            required
          />
          {!equityEdited && <span className="hint">Total − rent</span>}
        </label>
      </div>
      {mismatch && <p className="warning">⚠ Total doesn't equal rent + equity. You can still save it.</p>}
      {error && <p className="error">{error}</p>}
      <div className="row">
        <button className="btn primary" disabled={busy}>
          {editing ? 'Save changes' : 'Add payment'}
        </button>
        {editing && (
          <button type="button" className="btn ghost" onClick={() => onDone()}>
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}

function ImportCard({ onImported }: { onImported: () => Promise<void> }) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const upload = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setBusy(true);
    setError('');
    setResult(null);
    try {
      setResult(await api.importEntries(file));
      await onImported();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
      if (inputRef.current) inputRef.current.value = '';
    }
  };

  return (
    <section className="card">
      <h2>Import from Excel</h2>
      <p className="muted">
        Reads the first sheet from row 2: <b>A</b> date (dd/mm/yyyy), <b>B</b> total payment, <b>C</b> rent,{' '}
        <b>D</b> equity. Every row is added, and rows with no rent are marked as overpayments.
      </p>
      <label className="btn primary file-btn">
        {busy ? 'Importing…' : 'Choose .xlsx file'}
        <input ref={inputRef} type="file" accept=".xlsx,.xls" onChange={upload} disabled={busy} hidden />
      </label>
      {error && <p className="error">{error}</p>}
      {result && (
        <div className="import-result">
          <p className="ok">Imported {result.imported} payment{result.imported === 1 ? '' : 's'}.</p>
          {result.mismatchedRows.length > 0 && (
            <p className="warning">
              ⚠ Total ≠ rent + equity on row{result.mismatchedRows.length === 1 ? '' : 's'}{' '}
              {result.mismatchedRows.join(', ')} (imported as-is).
            </p>
          )}
          {result.errors.length > 0 && (
            <>
              <p className="error">Skipped {result.errors.length} row(s):</p>
              <ul className="error-list">
                {result.errors.map((er) => (
                  <li key={er.row}>
                    Row {er.row}: {er.message}
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}
    </section>
  );
}
