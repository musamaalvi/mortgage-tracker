import { FormEvent, useEffect, useState } from 'react';
import { api } from '../api';
import { pennies, todayIso, ukDate } from '../format';
import type { Mortgage, Rate, RepaymentType } from '../types';

interface Props {
  mortgage: Mortgage | null;
  onSaved: () => Promise<void>;
}

export default function Settings({ mortgage, onSaved }: Props) {
  return (
    <div className="stack">
      {!mortgage && (
        <div className="banner">Welcome! Enter your mortgage details to get started.</div>
      )}
      <MortgageForm mortgage={mortgage} onSaved={onSaved} />
      {mortgage && <Rates />}
    </div>
  );
}

function MortgageForm({ mortgage, onSaved }: Props) {
  const [price, setPrice] = useState(mortgage ? String(mortgage.propertyPrice) : '');
  const [deposit, setDeposit] = useState(mortgage ? String(mortgage.deposit) : '');
  const [amount, setAmount] = useState(mortgage ? String(mortgage.mortgageAmount) : '');
  // The mortgage amount follows price − deposit until the user types their own value.
  const [amountEdited, setAmountEdited] = useState(!!mortgage);
  const [term, setTerm] = useState(mortgage ? String(mortgage.termYears) : '25');
  const [startDate, setStartDate] = useState(mortgage?.startDate ?? todayIso());
  const [paymentDay, setPaymentDay] = useState(mortgage ? String(mortgage.paymentDay) : '1');
  const [repaymentType, setRepaymentType] = useState<RepaymentType>(mortgage?.repaymentType ?? 'REPAYMENT');
  const [initialRate, setInitialRate] = useState('');
  const [status, setStatus] = useState<{ ok: boolean; text: string } | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!amountEdited && price !== '' && deposit !== '') {
      setAmount(String(pennies(Number(price) - Number(deposit))));
    }
  }, [price, deposit, amountEdited]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setStatus(null);
    try {
      await api.saveMortgage({
        propertyPrice: Number(price),
        deposit: Number(deposit),
        mortgageAmount: Number(amount),
        termYears: Number(term),
        startDate,
        paymentDay: Number(paymentDay),
        repaymentType,
        initialRate: mortgage ? undefined : Number(initialRate),
      });
      setStatus({ ok: true, text: 'Saved' });
      await onSaved();
    } catch (err) {
      setStatus({ ok: false, text: (err as Error).message });
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="card" onSubmit={submit}>
      <h2>Mortgage details</h2>
      <div className="grid-form">
        <label>
          Property price (£)
          <input type="number" min="0" step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} required />
        </label>
        <label>
          Deposit (£)
          <input type="number" min="0" step="0.01" value={deposit} onChange={(e) => setDeposit(e.target.value)} required />
        </label>
        <label>
          Mortgage amount (£)
          <input
            type="number"
            min="0"
            step="0.01"
            value={amount}
            onChange={(e) => {
              setAmount(e.target.value);
              setAmountEdited(true);
            }}
            required
          />
          {!amountEdited && <span className="hint">Price − deposit</span>}
        </label>
        <label>
          Term (years)
          <input type="number" min="1" max="50" value={term} onChange={(e) => setTerm(e.target.value)} required />
        </label>
        <label>
          Start date
          <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} required />
        </label>
        <label>
          Payment day of month
          <input type="number" min="1" max="31" value={paymentDay} onChange={(e) => setPaymentDay(e.target.value)} required />
        </label>
        <label>
          Repayment type
          <select value={repaymentType} onChange={(e) => setRepaymentType(e.target.value as RepaymentType)}>
            <option value="REPAYMENT">Repayment</option>
            <option value="INTEREST_ONLY">Interest only</option>
          </select>
        </label>
        {!mortgage && (
          <label>
            Interest rate (% per year)
            <input
              type="number"
              min="0"
              max="100"
              step="0.001"
              value={initialRate}
              onChange={(e) => setInitialRate(e.target.value)}
              required
            />
          </label>
        )}
      </div>
      <div className="row">
        <button className="btn primary" disabled={busy}>
          {mortgage ? 'Save changes' : 'Create mortgage'}
        </button>
        {status && <span className={status.ok ? 'ok' : 'error'}>{status.text}</span>}
      </div>
    </form>
  );
}

function Rates() {
  const [rates, setRates] = useState<Rate[]>([]);
  const [rate, setRate] = useState('');
  const [effectiveFrom, setEffectiveFrom] = useState(todayIso());
  const [error, setError] = useState('');

  const load = () => api.rates().then(setRates).catch((e) => setError(e.message));
  useEffect(() => {
    load();
  }, []);

  const today = todayIso();
  const currentId = [...rates].reverse().find((r) => r.effectiveFrom <= today)?.id ?? rates[0]?.id;

  const add = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await api.addRate(Number(rate), effectiveFrom);
      setRate('');
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const remove = async (id: number) => {
    if (!confirm('Delete this rate?')) return;
    setError('');
    try {
      await api.deleteRate(id);
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <section className="card">
      <h2>Interest rates</h2>
      <p className="muted">
        Add a new rate whenever it changes (e.g. when a fixed deal ends). Forecasts use the rate in effect on each date.
      </p>
      <table className="table">
        <thead>
          <tr>
            <th>Effective from</th>
            <th className="num">Rate</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {[...rates].reverse().map((r) => (
            <tr key={r.id}>
              <td>
                {ukDate(r.effectiveFrom)}
                {r.id === currentId && <span className="pill">Current</span>}
                {r.effectiveFrom > today && <span className="pill muted-pill">Upcoming</span>}
              </td>
              <td className="num">{r.rate}%</td>
              <td className="actions">
                <button className="btn ghost small" onClick={() => remove(r.id)} disabled={rates.length <= 1}>
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <form className="row wrap" onSubmit={add}>
        <label>
          New rate (%)
          <input type="number" min="0" max="100" step="0.001" value={rate} onChange={(e) => setRate(e.target.value)} required />
        </label>
        <label>
          Effective from
          <input type="date" value={effectiveFrom} onChange={(e) => setEffectiveFrom(e.target.value)} required />
        </label>
        <button className="btn primary">Add rate</button>
      </form>
      {error && <p className="error">{error}</p>}
    </section>
  );
}
