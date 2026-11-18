const gbp = new Intl.NumberFormat('en-GB', { style: 'currency', currency: 'GBP' });
const gbpCompact = new Intl.NumberFormat('en-GB', {
  style: 'currency',
  currency: 'GBP',
  notation: 'compact',
  maximumFractionDigits: 1,
});
const monthFmt = new Intl.DateTimeFormat('en-GB', { month: 'short', year: 'numeric', timeZone: 'UTC' });
const monthShortFmt = new Intl.DateTimeFormat('en-GB', { month: 'short', year: '2-digit', timeZone: 'UTC' });

export const money = (v: number | null | undefined) => (v == null ? '—' : gbp.format(v));
export const moneyCompact = (v: number) => gbpCompact.format(v);

/** ISO yyyy-mm-dd -> dd/mm/yyyy */
export function ukDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
}

/** yyyy-MM -> "Mar 2025" */
export const monthLabel = (ym: string) => monthFmt.format(new Date(`${ym}-01T00:00:00Z`));
/** yyyy-MM -> "Mar 25" */
export const monthShort = (ym: string) => monthShortFmt.format(new Date(`${ym}-01T00:00:00Z`));

export function todayIso(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** Rounds to pennies to avoid floating-point noise in derived amounts. */
export const pennies = (v: number) => Math.round(v * 100) / 100;
