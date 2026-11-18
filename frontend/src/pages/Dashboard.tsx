import { useEffect, useState } from 'react';
import { api } from '../api';
import { MonthlyChart, ProgressChart } from '../components/Charts';
import { money, monthLabel, ukDate } from '../format';
import type { Summary } from '../types';

function Stat({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <div className="stat">
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      {sub && <div className="stat-sub">{sub}</div>}
    </div>
  );
}

export default function Dashboard() {
  const [s, setS] = useState<Summary | null>(null);
  const [error, setError] = useState('');
  const [fullTerm, setFullTerm] = useState(false);

  useEffect(() => {
    api.summary().then(setS).catch((e) => setError(e.message));
  }, []);

  if (error) return <p className="error">{error}</p>;
  if (!s) return <p className="muted">Loading…</p>;

  const next = s.nextPayment;
  const payoff = s.payoff;
  const paidOffPct = s.mortgageAmount > 0 ? Math.min(100, (s.totalEquity / s.mortgageAmount) * 100) : 0;

  return (
    <div className="stack">
      <div className="hero-row">
        <section className="card hero">
          <div className="stat-label">Remaining balance</div>
          <div className="hero-value">{money(s.remainingBalance)}</div>
          <div
            className="meter"
            role="meter"
            aria-valuenow={Math.round(paidOffPct)}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-label="Share of mortgage paid off"
          >
            <div className="meter-fill" style={{ width: `${paidOffPct}%` }} />
          </div>
          <div className="stat-sub">
            {paidOffPct.toFixed(1)}% of {money(s.mortgageAmount)} paid off · term ends {ukDate(s.termEndDate)}
          </div>
        </section>

        <section className="card next">
          <div className="stat-label">Expected next payment</div>
          {next ? (
            <>
              <div className="hero-value">{money(next.amount)}</div>
              <div className="stat-sub">
                due {ukDate(next.date)} at {s.currentRate}% · {next.remainingPayments} payments left in term
              </div>
              <div className="split">
                <span>
                  <i className="dot" style={{ background: 'var(--c-rent)' }} /> Rent {money(next.rent)}
                </span>
                <span>
                  <i className="dot" style={{ background: 'var(--c-equity)' }} /> Equity {money(next.equity)}
                </span>
              </div>
              {payoff && (
                <p className="payoff">
                  {payoff.payoffDate ? (
                    <>
                      If you keep paying <b>{money(payoff.currentPayment)}</b>/month, you'll be mortgage-free by{' '}
                      <b>{monthLabel(payoff.payoffDate.slice(0, 7))}</b> ({payoff.paymentsLeft} payments,{' '}
                      {money(payoff.remainingRent)} more rent).
                    </>
                  ) : (
                    <>
                      A payment of <b>{money(payoff.currentPayment)}</b>/month doesn't reduce the balance, so the loan
                      won't be paid off at that rate.
                    </>
                  )}
                </p>
              )}
            </>
          ) : (
            <div className="hero-value">Paid off 🎉</div>
          )}
        </section>
      </div>

      <div className="stats">
        <Stat label="Total paid" value={money(s.totalPaid)} />
        <Stat label="Rent (interest) paid" value={money(s.totalRent)} />
        <Stat
          label="Equity paid"
          value={money(s.totalEquity)}
          sub={s.totalOverpayments > 0 ? `incl. ${money(s.totalOverpayments)} overpayments` : undefined}
        />
        <Stat
          label="Equity owned"
          value={money(s.equityOwned)}
          sub={`${s.equityOwnedPercent}% of ${money(s.propertyPrice)} (deposit + equity)`}
        />
        <Stat label="Current rate" value={`${s.currentRate}%`} />
      </div>

      <section className="card">
        <h2>Where each month's payment goes</h2>
        {s.monthly.length ? (
          <>
            <MonthlyChart data={s.monthly} />
            <details className="table-toggle">
              <summary>Show as table</summary>
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Month</th>
                      <th className="num">Rent</th>
                      <th className="num">Equity</th>
                      <th className="num">Overpayment</th>
                      <th className="num">Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {[...s.monthly].reverse().map((m) => (
                      <tr key={m.month}>
                        <td>{monthLabel(m.month)}</td>
                        <td className="num">{money(m.rent)}</td>
                        <td className="num">{money(m.equity)}</td>
                        <td className="num">{money(m.overpayment)}</td>
                        <td className="num">{money(m.rent + m.equity + m.overpayment)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </details>
          </>
        ) : (
          <p className="muted">Add payments to see the monthly breakdown.</p>
        )}
      </section>

      <section className="card">
        <div className="card-head">
          <h2>Progress over time</h2>
          <div className="segmented" role="group" aria-label="Time range">
            <button className={!fullTerm ? 'active' : ''} aria-pressed={!fullTerm} onClick={() => setFullTerm(false)}>
              Paid so far
            </button>
            <button className={fullTerm ? 'active' : ''} aria-pressed={fullTerm} onClick={() => setFullTerm(true)}>
              Full term
            </button>
          </div>
        </div>
        {fullTerm && (
          <p className="muted">The dashed line projects the balance to the end of the term at your current rate.</p>
        )}
        <ProgressChart data={s.timeline} fullTerm={fullTerm} />
      </section>
    </div>
  );
}
