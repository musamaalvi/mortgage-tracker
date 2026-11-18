package com.mortgagetracker.summary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class SummaryDtos {

    private SummaryDtos() {}

    public record Summary(
            BigDecimal propertyPrice,
            BigDecimal deposit,
            BigDecimal mortgageAmount,
            BigDecimal totalPaid,
            BigDecimal totalRent,
            BigDecimal totalEquity,
            BigDecimal totalOverpayments,
            BigDecimal remainingBalance,
            BigDecimal equityOwned,
            BigDecimal equityOwnedPercent,
            BigDecimal currentRate,
            LocalDate termEndDate,
            NextPayment nextPayment,
            Payoff payoff,
            List<MonthlyBreakdown> monthly,
            List<TimelinePoint> timeline) {}

    /** Recalculated payment: remaining balance over the remaining term at the current rate. Null once paid off. */
    public record NextPayment(LocalDate date, BigDecimal amount, BigDecimal rent, BigDecimal equity, int remainingPayments) {}

    /**
     * What happens if you keep paying {@code currentPayment} each month.
     * {@code payoffDate} is null when the payment doesn't cover the interest (or the mortgage is interest-only).
     */
    public record Payoff(BigDecimal currentPayment, LocalDate payoffDate, Integer paymentsLeft, BigDecimal remainingRent) {}

    /** {@code month} is yyyy-MM. Equity excludes overpayments, which are reported separately. */
    public record MonthlyBreakdown(String month, BigDecimal rent, BigDecimal equity, BigDecimal overpayment) {}

    /** Actual values are null for future months; {@code projectedBalance} is null for past months except the last. */
    public record TimelinePoint(String month, BigDecimal balance, BigDecimal cumulativePaid, BigDecimal cumulativeRent,
                                BigDecimal cumulativeEquity, BigDecimal projectedBalance) {}
}
