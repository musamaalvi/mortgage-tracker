export type RepaymentType = 'REPAYMENT' | 'INTEREST_ONLY';
export type EntryType = 'REGULAR' | 'OVERPAYMENT';

export interface Mortgage {
  id: number;
  propertyPrice: number;
  deposit: number;
  mortgageAmount: number;
  termYears: number;
  startDate: string;
  paymentDay: number;
  repaymentType: RepaymentType;
}

export interface Rate {
  id: number;
  rate: number;
  effectiveFrom: string;
}

export interface Entry {
  id: number;
  date: string;
  totalPayment: number;
  rent: number;
  equity: number;
  type: EntryType;
  mismatch: boolean;
}

export interface ImportResult {
  imported: number;
  errors: { row: number; message: string }[];
  mismatchedRows: number[];
}

export interface NextPayment {
  date: string;
  amount: number;
  rent: number;
  equity: number;
  remainingPayments: number;
}

export interface Payoff {
  currentPayment: number;
  payoffDate: string | null;
  paymentsLeft: number | null;
  remainingRent: number | null;
}

export interface MonthlyBreakdown {
  month: string;
  rent: number;
  equity: number;
  overpayment: number;
}

export interface TimelinePoint {
  month: string;
  balance: number | null;
  cumulativePaid: number | null;
  cumulativeRent: number | null;
  cumulativeEquity: number | null;
  projectedBalance: number | null;
}

export interface Summary {
  propertyPrice: number;
  deposit: number;
  mortgageAmount: number;
  totalPaid: number;
  totalRent: number;
  totalEquity: number;
  totalOverpayments: number;
  remainingBalance: number;
  equityOwned: number;
  equityOwnedPercent: number;
  currentRate: number;
  termEndDate: string;
  nextPayment: NextPayment | null;
  payoff: Payoff | null;
  monthly: MonthlyBreakdown[];
  timeline: TimelinePoint[];
}
