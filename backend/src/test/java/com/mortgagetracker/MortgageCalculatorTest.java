package com.mortgagetracker;

import com.mortgagetracker.entry.Entry;
import com.mortgagetracker.entry.EntryType;
import com.mortgagetracker.mortgage.InterestRate;
import com.mortgagetracker.mortgage.Mortgage;
import com.mortgagetracker.mortgage.RepaymentType;
import com.mortgagetracker.summary.MortgageCalculator;
import com.mortgagetracker.summary.SummaryDtos.Summary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MortgageCalculatorTest {

    private static Mortgage mortgage() {
        Mortgage m = new Mortgage();
        m.setPropertyPrice(new BigDecimal("250000"));
        m.setDeposit(new BigDecimal("50000"));
        m.setMortgageAmount(new BigDecimal("200000"));
        m.setTermYears(25);
        m.setStartDate(LocalDate.of(2025, 1, 15));
        m.setPaymentDay(1);
        m.setRepaymentType(RepaymentType.REPAYMENT);
        return m;
    }

    private static Entry entry(Mortgage m, LocalDate date, String total, String rent, String equity, EntryType type) {
        Entry e = new Entry(m);
        e.setDate(date);
        e.setTotalPayment(new BigDecimal(total));
        e.setRent(new BigDecimal(rent));
        e.setEquity(new BigDecimal(equity));
        e.setType(type);
        return e;
    }

    @Test
    void firstPaymentMatchesStandardAmortisation() {
        Mortgage m = mortgage();
        List<InterestRate> rates = List.of(new InterestRate(m, new BigDecimal("5"), m.getStartDate()));

        Summary s = new MortgageCalculator(m, rates, List.of(), LocalDate.of(2025, 1, 20)).summarise();

        assertThat(s.nextPayment().date()).isEqualTo(LocalDate.of(2025, 2, 1));
        assertThat(s.nextPayment().remainingPayments()).isEqualTo(300);
        assertThat(s.nextPayment().amount()).isEqualByComparingTo("1169.18");
        assertThat(s.nextPayment().rent()).isEqualByComparingTo("833.33");
        assertThat(s.nextPayment().equity()).isEqualByComparingTo("335.85");
        assertThat(s.payoff().paymentsLeft()).isEqualTo(300);
    }

    @Test
    void totalsAndBalanceComeFromEntries() {
        Mortgage m = mortgage();
        List<InterestRate> rates = List.of(new InterestRate(m, new BigDecimal("5"), m.getStartDate()));
        List<Entry> entries = List.of(
                entry(m, LocalDate.of(2025, 2, 1), "1169.18", "833.33", "335.85", EntryType.REGULAR),
                entry(m, LocalDate.of(2025, 3, 1), "1169.18", "831.93", "337.25", EntryType.REGULAR),
                entry(m, LocalDate.of(2025, 3, 1), "5000", "0", "5000", EntryType.OVERPAYMENT));

        Summary s = new MortgageCalculator(m, rates, entries, LocalDate.of(2025, 3, 10)).summarise();

        assertThat(s.totalPaid()).isEqualByComparingTo("7338.36");
        assertThat(s.totalRent()).isEqualByComparingTo("1665.26");
        assertThat(s.totalEquity()).isEqualByComparingTo("5673.10");
        assertThat(s.totalOverpayments()).isEqualByComparingTo("5000");
        assertThat(s.remainingBalance()).isEqualByComparingTo("194326.90");
        assertThat(s.equityOwned()).isEqualByComparingTo("55673.10");
        assertThat(s.nextPayment().date()).isEqualTo(LocalDate.of(2025, 4, 1));
        assertThat(s.nextPayment().remainingPayments()).isEqualTo(298);
        // Keeping the old payment after a £5k overpayment finishes early.
        assertThat(s.payoff().paymentsLeft()).isLessThan(298);

        assertThat(s.monthly()).hasSize(2);
        assertThat(s.monthly().get(1).overpayment()).isEqualByComparingTo("5000");
        assertThat(s.monthly().get(1).equity()).isEqualByComparingTo("337.25");
        assertThat(s.timeline().get(s.timeline().size() - 1).projectedBalance()).isEqualByComparingTo("0");
    }

    @Test
    void usesLatestRateInEffect() {
        Mortgage m = mortgage();
        List<InterestRate> rates = List.of(
                new InterestRate(m, new BigDecimal("5"), m.getStartDate()),
                new InterestRate(m, new BigDecimal("3.5"), LocalDate.of(2026, 1, 1)),
                new InterestRate(m, new BigDecimal("6"), LocalDate.of(2030, 1, 1)));

        Summary s = new MortgageCalculator(m, rates, List.of(), LocalDate.of(2026, 6, 1)).summarise();

        assertThat(s.currentRate()).isEqualByComparingTo("3.5");
    }

    @Test
    void zeroRateSplitsEvenly() {
        assertThat(MortgageCalculator.amortisedPayment(new BigDecimal("1200"), BigDecimal.ZERO, 12))
                .isEqualByComparingTo("100");
    }
}
