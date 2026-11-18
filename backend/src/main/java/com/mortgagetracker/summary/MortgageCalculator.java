package com.mortgagetracker.summary;

import com.mortgagetracker.entry.Entry;
import com.mortgagetracker.entry.EntryType;
import com.mortgagetracker.mortgage.InterestRate;
import com.mortgagetracker.mortgage.Mortgage;
import com.mortgagetracker.mortgage.RepaymentType;
import com.mortgagetracker.summary.SummaryDtos.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Pure calculations over a mortgage, its rate history and its entries. Interest is monthly (annual rate / 12). */
public class MortgageCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final int MAX_SIMULATED_MONTHS = 1200;

    private final Mortgage mortgage;
    private final List<InterestRate> rates; // sorted by effectiveFrom
    private final List<Entry> entries;      // sorted by date
    private final LocalDate today;

    public MortgageCalculator(Mortgage mortgage, List<InterestRate> rates, List<Entry> entries, LocalDate today) {
        this.mortgage = mortgage;
        this.rates = rates.stream().sorted(Comparator.comparing(InterestRate::getEffectiveFrom)).toList();
        this.entries = entries.stream().sorted(Comparator.comparing(Entry::getDate)).toList();
        this.today = today;
    }

    public Summary summarise() {
        BigDecimal totalPaid = sum(entries, Entry::getTotalPayment);
        BigDecimal totalRent = sum(entries, Entry::getRent);
        BigDecimal totalEquity = sum(entries, Entry::getEquity);
        BigDecimal totalOverpayments = sum(entries.stream().filter(e -> e.getType() == EntryType.OVERPAYMENT).toList(),
                Entry::getEquity);

        BigDecimal remaining = mortgage.getMortgageAmount().subtract(totalEquity).max(BigDecimal.ZERO);
        BigDecimal equityOwned = mortgage.getDeposit().add(totalEquity).min(mortgage.getPropertyPrice());
        BigDecimal equityPercent = equityOwned.multiply(HUNDRED)
                .divide(mortgage.getPropertyPrice(), 2, RoundingMode.HALF_UP);

        NextPayment next = remaining.signum() > 0 ? nextPayment(remaining) : null;
        Payoff payoff = next != null ? payoff(remaining, next) : null;

        return new Summary(
                mortgage.getPropertyPrice(),
                mortgage.getDeposit(),
                mortgage.getMortgageAmount(),
                money(totalPaid),
                money(totalRent),
                money(totalEquity),
                money(totalOverpayments),
                money(remaining),
                money(equityOwned),
                equityPercent,
                rateAt(today),
                mortgage.getStartDate().plusYears(mortgage.getTermYears()),
                next,
                payoff,
                monthly(),
                timeline(remaining, next));
    }

    // --- next payment -------------------------------------------------------------------------

    NextPayment nextPayment(BigDecimal balance) {
        LocalDate date = nextPaymentDate();
        int n = remainingPayments(date);
        BigDecimal r = monthlyRate(date);
        BigDecimal interest = balance.multiply(r, MC);
        BigDecimal amount = mortgage.getRepaymentType() == RepaymentType.INTEREST_ONLY
                ? interest
                : amortisedPayment(balance, r, n);
        return new NextPayment(date, money(amount), money(interest), money(amount).subtract(money(interest)), n);
    }

    /** The next payment day that is today or later, after the start date and after the last regular payment. */
    LocalDate nextPaymentDate() {
        LocalDate lastRegular = entries.stream()
                .filter(e -> e.getType() == EntryType.REGULAR)
                .map(Entry::getDate)
                .max(Comparator.naturalOrder())
                .orElse(null);
        YearMonth month = YearMonth.from(today);
        LocalDate candidate = onPaymentDay(month);
        while (candidate.isBefore(today)
                || !candidate.isAfter(mortgage.getStartDate())
                || (lastRegular != null && !candidate.isAfter(lastRegular))) {
            month = month.plusMonths(1);
            candidate = onPaymentDay(month);
        }
        return candidate;
    }

    /** Payments left including the one on {@code date}; the last falls in the month the term ends. */
    int remainingPayments(LocalDate date) {
        YearMonth last = YearMonth.from(mortgage.getStartDate()).plusMonths(mortgage.getTermYears() * 12L);
        long n = ChronoUnit.MONTHS.between(YearMonth.from(date), last) + 1;
        return (int) Math.max(1, n);
    }

    public static BigDecimal amortisedPayment(BigDecimal balance, BigDecimal monthlyRate, int payments) {
        if (monthlyRate.signum() == 0) {
            return balance.divide(BigDecimal.valueOf(payments), MC);
        }
        // P * r * (1+r)^n / ((1+r)^n - 1)
        BigDecimal growth = BigDecimal.ONE.add(monthlyRate).pow(payments, MC);
        return balance.multiply(monthlyRate, MC).multiply(growth, MC).divide(growth.subtract(BigDecimal.ONE), MC);
    }

    // --- payoff at current payment -------------------------------------------------------------

    Payoff payoff(BigDecimal balance, NextPayment next) {
        BigDecimal payment = entries.stream()
                .filter(e -> e.getType() == EntryType.REGULAR)
                .reduce((a, b) -> b)
                .map(Entry::getTotalPayment)
                .orElse(next.amount());

        // Payments are rounded to pennies, which can leave a few pounds after the "last" payment.
        // Like lenders do, fold a leftover under 1% of a payment into the final payment.
        BigDecimal residual = payment.movePointLeft(2);
        BigDecimal remainingRent = BigDecimal.ZERO;
        LocalDate date = next.date();
        for (int i = 1; i <= MAX_SIMULATED_MONTHS; i++) {
            BigDecimal interest = balance.multiply(monthlyRate(date), MC);
            if (payment.compareTo(interest) <= 0) {
                return new Payoff(money(payment), null, null, null);
            }
            remainingRent = remainingRent.add(interest);
            balance = balance.add(interest).subtract(payment);
            if (balance.compareTo(residual) <= 0) {
                return new Payoff(money(payment), date, i, money(remainingRent));
            }
            date = onPaymentDay(YearMonth.from(date).plusMonths(1));
        }
        return new Payoff(money(payment), null, null, null);
    }

    // --- charts --------------------------------------------------------------------------------

    List<MonthlyBreakdown> monthly() {
        Map<YearMonth, BigDecimal[]> byMonth = new TreeMap<>();
        for (Entry e : entries) {
            BigDecimal[] m = byMonth.computeIfAbsent(YearMonth.from(e.getDate()),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            m[0] = m[0].add(e.getRent());
            int slot = e.getType() == EntryType.OVERPAYMENT ? 2 : 1;
            m[slot] = m[slot].add(e.getEquity());
        }
        return byMonth.entrySet().stream()
                .map(en -> new MonthlyBreakdown(en.getKey().toString(),
                        money(en.getValue()[0]), money(en.getValue()[1]), money(en.getValue()[2])))
                .toList();
    }

    List<TimelinePoint> timeline(BigDecimal remaining, NextPayment next) {
        TreeMap<YearMonth, BigDecimal[]> actual = new TreeMap<>(); // paid, rent, equity (cumulative)
        YearMonth startMonth = YearMonth.from(mortgage.getStartDate());
        YearMonth firstMonth = entries.isEmpty() ? startMonth
                : min(startMonth, YearMonth.from(entries.get(0).getDate()));
        YearMonth lastMonth = entries.isEmpty() ? firstMonth
                : YearMonth.from(entries.get(entries.size() - 1).getDate());

        BigDecimal paid = BigDecimal.ZERO, rent = BigDecimal.ZERO, equity = BigDecimal.ZERO;
        int idx = 0;
        for (YearMonth m = firstMonth; !m.isAfter(lastMonth); m = m.plusMonths(1)) {
            while (idx < entries.size() && !YearMonth.from(entries.get(idx).getDate()).isAfter(m)) {
                Entry e = entries.get(idx++);
                paid = paid.add(e.getTotalPayment());
                rent = rent.add(e.getRent());
                equity = equity.add(e.getEquity());
            }
            actual.put(m, new BigDecimal[]{paid, rent, equity});
        }

        TreeMap<YearMonth, BigDecimal> projected = new TreeMap<>();
        projected.put(lastMonth, remaining);
        if (next != null) {
            BigDecimal balance = remaining;
            LocalDate date = next.date();
            for (int left = next.remainingPayments(); left > 0 && balance.signum() > 0; left--) {
                BigDecimal r = monthlyRate(date);
                BigDecimal interest = balance.multiply(r, MC);
                BigDecimal principal = mortgage.getRepaymentType() == RepaymentType.INTEREST_ONLY
                        ? BigDecimal.ZERO
                        : amortisedPayment(balance, r, left).subtract(interest);
                balance = balance.subtract(principal).max(BigDecimal.ZERO);
                projected.put(YearMonth.from(date), balance);
                date = onPaymentDay(YearMonth.from(date).plusMonths(1));
            }
        }

        SortedSet<YearMonth> months = new TreeSet<>(actual.keySet());
        months.addAll(projected.keySet());
        List<TimelinePoint> points = new ArrayList<>();
        for (YearMonth m : months) {
            BigDecimal[] a = actual.get(m);
            BigDecimal p = projected.get(m);
            points.add(new TimelinePoint(m.toString(),
                    a == null ? null : money(mortgage.getMortgageAmount().subtract(a[2]).max(BigDecimal.ZERO)),
                    a == null ? null : money(a[0]),
                    a == null ? null : money(a[1]),
                    a == null ? null : money(a[2]),
                    p == null ? null : money(p)));
        }
        return points;
    }

    // --- helpers -------------------------------------------------------------------------------

    /** Annual rate in percent effective on {@code date}; the earliest rate if none had started yet. */
    BigDecimal rateAt(LocalDate date) {
        BigDecimal rate = rates.isEmpty() ? BigDecimal.ZERO : rates.get(0).getRate();
        for (InterestRate r : rates) {
            if (!r.getEffectiveFrom().isAfter(date)) {
                rate = r.getRate();
            }
        }
        return rate;
    }

    private BigDecimal monthlyRate(LocalDate date) {
        return rateAt(date).divide(HUNDRED, MC).divide(TWELVE, MC);
    }

    private LocalDate onPaymentDay(YearMonth month) {
        return month.atDay(Math.min(mortgage.getPaymentDay(), month.lengthOfMonth()));
    }

    private static YearMonth min(YearMonth a, YearMonth b) {
        return a.isBefore(b) ? a : b;
    }

    private static BigDecimal sum(List<Entry> list, java.util.function.Function<Entry, BigDecimal> f) {
        return list.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
