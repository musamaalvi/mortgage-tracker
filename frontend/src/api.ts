import type { Entry, EntryType, ImportResult, Mortgage, Rate, RepaymentType, Summary } from './types';

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

let onUnauthorized: () => void = () => {};

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler;
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T | null> {
  const headers: Record<string, string> = {};
  if (init.body && !(init.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }
  const res = await fetch(`/api${path}`, { credentials: 'same-origin', ...init, headers });

  if (res.status === 401 && path !== '/auth/login') {
    onUnauthorized();
  }
  if (!res.ok) {
    let message = res.status === 401 ? 'Invalid username or password' : `Request failed (${res.status})`;
    try {
      const body = await res.json();
      message = body.detail ?? body.message ?? message;
    } catch {
      // no JSON body
    }
    throw new ApiError(res.status, message);
  }
  if (res.status === 204) {
    return null;
  }
  return res.json() as Promise<T>;
}

const json = (method: string, body: unknown): RequestInit => ({ method, body: JSON.stringify(body) });

export interface MortgageInput {
  propertyPrice: number;
  deposit: number;
  mortgageAmount: number;
  termYears: number;
  startDate: string;
  paymentDay: number;
  repaymentType: RepaymentType;
  initialRate?: number;
}

export interface EntryInput {
  date: string;
  totalPayment: number;
  rent: number;
  equity: number;
  type: EntryType;
}

export const api = {
  me: () => request<{ username: string }>('/auth/me'),
  login: (username: string, password: string) =>
    request<{ username: string }>('/auth/login', json('POST', { username, password })),
  logout: () => request<void>('/auth/logout', { method: 'POST' }),

  mortgage: () => request<Mortgage>('/mortgage'),
  saveMortgage: (m: MortgageInput) => request<Mortgage>('/mortgage', json('PUT', m)),

  rates: async () => (await request<Rate[]>('/rates')) ?? [],
  addRate: (rate: number, effectiveFrom: string) => request<Rate>('/rates', json('POST', { rate, effectiveFrom })),
  deleteRate: (id: number) => request<void>(`/rates/${id}`, { method: 'DELETE' }),

  entries: async () => (await request<Entry[]>('/entries')) ?? [],
  createEntry: (e: EntryInput) => request<Entry>('/entries', json('POST', e)),
  updateEntry: (id: number, e: EntryInput) => request<Entry>(`/entries/${id}`, json('PUT', e)),
  deleteEntry: (id: number) => request<void>(`/entries/${id}`, { method: 'DELETE' }),
  importEntries: (file: File) => {
    const body = new FormData();
    body.append('file', file);
    return request<ImportResult>('/entries/import', { method: 'POST', body });
  },

  summary: () => request<Summary>('/summary'),
};
