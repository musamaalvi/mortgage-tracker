import type { CSSProperties } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { money, moneyCompact, monthLabel, monthShort } from '../format';
import type { MonthlyBreakdown, TimelinePoint } from '../types';

interface Series {
  key: string;
  label: string;
  color: string;
  dashed?: boolean;
}

// Colours are CSS variables (see styles.css) so light/dark mode swap in one place.
// Identity is shared across charts: equity = blue, rent = orange.
const MONTHLY_SERIES: Series[] = [
  { key: 'equity', label: 'Equity', color: 'var(--c-equity)' },
  { key: 'overpayment', label: 'Overpayment', color: 'var(--c-overpayment)' },
  { key: 'rent', label: 'Rent (interest)', color: 'var(--c-rent)' },
];

const PROGRESS_SERIES: Series[] = [
  { key: 'balance', label: 'Remaining balance', color: 'var(--c-balance)' },
  { key: 'projectedBalance', label: 'Projected balance', color: 'var(--c-balance)', dashed: true },
  { key: 'cumulativePaid', label: 'Total paid', color: 'var(--c-paid)' },
  { key: 'cumulativeEquity', label: 'Equity paid', color: 'var(--c-equity)' },
  { key: 'cumulativeRent', label: 'Rent paid', color: 'var(--c-rent)' },
];

const axisTick = { fill: 'var(--text-muted)', fontSize: 12 };

function Legend({ series }: { series: Series[] }) {
  return (
    <ul className="legend">
      {series.map((s) => (
        <li key={s.key}>
          <span className={`swatch ${s.dashed ? 'dashed' : ''}`} style={{ '--swatch': s.color } as CSSProperties} />
          {s.label}
        </li>
      ))}
    </ul>
  );
}

interface TooltipProps {
  active?: boolean;
  label?: string;
  payload?: { dataKey?: string | number; value?: number | null }[];
  series: Series[];
  total?: boolean;
}

function ChartTooltip({ active, label, payload, series, total }: TooltipProps) {
  if (!active || !payload?.length || !label) return null;
  const rows = series
    .map((s) => ({ s, value: payload.find((p) => p.dataKey === s.key)?.value }))
    .filter((r) => r.value != null);
  if (!rows.length) return null;
  const sum = rows.reduce((acc, r) => acc + (r.value ?? 0), 0);
  return (
    <div className="chart-tooltip">
      <div className="tt-title">{monthLabel(label)}</div>
      {rows.map(({ s, value }) => (
        <div className="tt-row" key={s.key}>
          <span className={`swatch ${s.dashed ? 'dashed' : ''}`} style={{ '--swatch': s.color } as CSSProperties} />
          <span className="tt-label">{s.label}</span>
          <span className="tt-value">{money(value)}</span>
        </div>
      ))}
      {total && rows.length > 1 && (
        <div className="tt-row tt-total">
          <span />
          <span className="tt-label">Total</span>
          <span className="tt-value">{money(sum)}</span>
        </div>
      )}
    </div>
  );
}

export function MonthlyChart({ data }: { data: MonthlyBreakdown[] }) {
  const hasOverpayments = data.some((d) => d.overpayment > 0);
  const series = hasOverpayments ? MONTHLY_SERIES : MONTHLY_SERIES.filter((s) => s.key !== 'overpayment');
  const top = series[series.length - 1].key;

  return (
    <figure className="chart">
      <Legend series={series} />
      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }} barCategoryGap="20%">
          <CartesianGrid vertical={false} stroke="var(--grid)" />
          <XAxis dataKey="month" tickFormatter={monthShort} tick={axisTick} tickLine={false}
                 axisLine={{ stroke: 'var(--axis)' }} minTickGap={12} />
          <YAxis tickFormatter={moneyCompact} tick={axisTick} tickLine={false} axisLine={false} width={64} />
          <Tooltip cursor={{ fill: 'var(--hover)' }}
                   content={(p) => <ChartTooltip {...(p as TooltipProps)} series={[...series].reverse()} total />} />
          {series.map((s) => (
            <Bar key={s.key} dataKey={s.key} stackId="month" fill={s.color} stroke="var(--surface)" strokeWidth={1}
                 radius={s.key === top ? [4, 4, 0, 0] : 0} maxBarSize={40} />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </figure>
  );
}

export function ProgressChart({ data, fullTerm }: { data: TimelinePoint[]; fullTerm: boolean }) {
  const points = fullTerm ? data : data.filter((p) => p.balance != null);
  const series = fullTerm ? PROGRESS_SERIES : PROGRESS_SERIES.filter((s) => s.key !== 'projectedBalance');
  return (
    <figure className="chart">
      <Legend series={series} />
      <ResponsiveContainer width="100%" height={320}>
        <LineChart data={points} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
          <CartesianGrid vertical={false} stroke="var(--grid)" />
          <XAxis dataKey="month" tickFormatter={monthShort} tick={axisTick} tickLine={false}
                 axisLine={{ stroke: 'var(--axis)' }} minTickGap={24} />
          <YAxis tickFormatter={moneyCompact} tick={axisTick} tickLine={false} axisLine={false} width={64} />
          <Tooltip cursor={{ stroke: 'var(--axis)', strokeWidth: 1 }}
                   content={(p) => <ChartTooltip {...(p as TooltipProps)} series={series} />} />
          {series.map((s) => (
            <Line key={s.key} type="monotone" dataKey={s.key} stroke={s.color} strokeWidth={2}
                  strokeDasharray={s.dashed ? '6 4' : undefined} dot={false}
                  activeDot={{ r: 4, stroke: 'var(--surface)', strokeWidth: 2 }} isAnimationActive={false} />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </figure>
  );
}
